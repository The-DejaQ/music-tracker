package org.dejaq.plugins.musictracker.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Text;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerConfig;
import org.dejaq.plugins.musictracker.MusicTrackManager;

@Singleton
public class UnlockDetectionService
{
	public interface UnlockListener
	{
		void onTrackUnlocked(MusicTrack unlockedMusicTrack);
	}

	private static final Pattern UNLOCK_CHAT_MESSAGE_PATTERN =
		Pattern.compile("unlocked (?:a new )?music track[:\\s]+(?:<col=[^>]+>)?([^<]+)");

	// Green text - set by the game's music tab script on unlocked track rows
	private static final int UNLOCKED_TRACK_WIDGET_TEXT_COLOR = 0x0DC10D;
	// Cyan text - set by the game's music tab script on the currently playing track row
	private static final int NOW_PLAYING_WIDGET_TEXT_COLOR = 0x3CE6E6;

	private static final long SYNC_BATCH_TIME_BUDGET_NANOS = 1_000_000L;

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private MusicTrackManager musicTrackManager;
	@Inject
	private TrackingStateService trackingStateService;
	@Inject
	private MusicTrackerConfig musicTrackerConfig;

	private String lastKnownNowPlayingTrackTitle;

	@Getter
	private boolean musicTabSyncInProgress;
	private List<Widget> pendingSyncWidgets;
	private int pendingSyncWidgetIndex;
	private Map<String, MusicTrack> pendingSyncTrackTitleIndex;
	private int pendingSyncNewlyUnlockedCount;
	private UnlockListener pendingSyncListener;
	private IntConsumer pendingSyncCompletionCallback;

	public void handleChatMessage(ChatMessage chatMessageEvent, UnlockListener unlockListener)
	{
		if (chatMessageEvent.getType() != ChatMessageType.GAMEMESSAGE && chatMessageEvent.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		Matcher unlockMessageMatcher = UNLOCK_CHAT_MESSAGE_PATTERN.matcher(chatMessageEvent.getMessage());
		if (unlockMessageMatcher.find())
		{
			recordUnlockedTrackByTitle(unlockMessageMatcher.group(1).trim(), unlockListener);
		}
	}

	public boolean isMusicTabOpen()
	{
		Widget trackListWidget = client.getWidget(InterfaceID.Music.SCROLLABLE);
		if (trackListWidget != null && !trackListWidget.isHidden()
			&& trackListWidget.getChildren() != null && trackListWidget.getChildren().length > 0)
		{
			return true;
		}

		Widget trackListBackgroundWidget = client.getWidget(InterfaceID.Music.BACKGROUND);
		if (trackListBackgroundWidget != null && !trackListBackgroundWidget.isHidden())
		{
			return true;
		}

		Widget musicTabContentsWidget = client.getWidget(InterfaceID.Music.CONTENTS);
		return musicTabContentsWidget != null && !musicTabContentsWidget.isHidden();
	}

	public void pollNowPlayingTrack(UnlockListener unlockListener)
	{
		Widget nowPlayingWidget = client.getWidget(InterfaceID.Music.NOW_PLAYING_TEXT);
		if (nowPlayingWidget == null || nowPlayingWidget.getText() == null)
		{
			return;
		}

		String nowPlayingTrackTitle = Text.removeTags(nowPlayingWidget.getText()).trim();
		if (nowPlayingTrackTitle.isBlank() || nowPlayingTrackTitle.equalsIgnoreCase(lastKnownNowPlayingTrackTitle))
		{
			return;
		}

		lastKnownNowPlayingTrackTitle = nowPlayingTrackTitle;

		if (!trackingStateService.isTrackUnlocked(nowPlayingTrackTitle))
		{
			recordUnlockedTrackByTitle(nowPlayingTrackTitle, unlockListener);
		}
	}

	public void beginMusicTabSync(UnlockListener unlockListener, IntConsumer onSyncComplete)
	{
		if (musicTabSyncInProgress || musicTrackerConfig.lockedTracks())
		{
			return;
		}

		pendingSyncWidgets = flattenMusicTabTrackWidgets();
		pendingSyncWidgetIndex = 0;
		pendingSyncTrackTitleIndex = musicTrackManager.buildTrackTitleIndex();
		pendingSyncNewlyUnlockedCount = 0;
		pendingSyncListener = unlockListener;
		pendingSyncCompletionCallback = onSyncComplete;
		musicTabSyncInProgress = true;

		processSyncBatch();
	}

	private List<Widget> flattenMusicTabTrackWidgets()
	{
		List<Widget> flattenedWidgets = new ArrayList<>();

		Widget trackListRoot = client.getWidget(InterfaceID.Music.JUKEBOX);
		if (trackListRoot == null)
		{
			return flattenedWidgets;
		}

		for (Widget categoryContainer : getWidgetNull(trackListRoot.getChildren()))
		{
			flattenedWidgets.add(categoryContainer);
			addAllNonNull(flattenedWidgets, categoryContainer.getChildren());
			addAllNonNull(flattenedWidgets, categoryContainer.getDynamicChildren());
		}

		return flattenedWidgets;
	}

	private static Widget[] getWidgetNull(Widget[] widgets)
	{
		return widgets != null ? widgets : new Widget[0];
	}

	private static void addAllNonNull(List<Widget> destination, Widget[] widgets)
	{
		if (widgets == null)
		{
			return;
		}
		for (Widget widget : widgets)
		{
			if (widget != null)
			{
				destination.add(widget);
			}
		}
	}

	private void processSyncBatch()
	{
		long batchStartNanos = System.nanoTime();

		while (pendingSyncWidgetIndex < pendingSyncWidgets.size())
		{
			Widget widget = pendingSyncWidgets.get(pendingSyncWidgetIndex);
			pendingSyncWidgetIndex++;

			pendingSyncNewlyUnlockedCount += synchronizeUnlockedTrackWidget(widget, pendingSyncTrackTitleIndex, pendingSyncListener);

			if (System.nanoTime() - batchStartNanos >= SYNC_BATCH_TIME_BUDGET_NANOS)
			{
				break;
			}
		}

		if (pendingSyncWidgetIndex < pendingSyncWidgets.size())
		{
			clientThread.invokeLater(this::processSyncBatch);
			return;
		}

		int totalNewlyUnlockedCount = pendingSyncNewlyUnlockedCount;
		IntConsumer completionCallback = pendingSyncCompletionCallback;

		musicTabSyncInProgress = false;
		pendingSyncWidgets = null;
		pendingSyncTrackTitleIndex = null;
		pendingSyncListener = null;
		pendingSyncCompletionCallback = null;

		completionCallback.accept(totalNewlyUnlockedCount);
	}

	private int synchronizeUnlockedTrackWidget(Widget trackEntryWidget, Map<String, MusicTrack> trackTitleIndex, UnlockListener unlockListener)
	{
		if (trackEntryWidget.getTextColor() != UNLOCKED_TRACK_WIDGET_TEXT_COLOR
			&& trackEntryWidget.getTextColor() != NOW_PLAYING_WIDGET_TEXT_COLOR)
		{
			return 0;
		}

		String widgetText = trackEntryWidget.getText();
		if (widgetText == null || widgetText.isBlank())
		{
			return 0;
		}

		String trackTitle = Text.removeTags(widgetText).trim();
		MusicTrack matchingTrack = trackTitleIndex.get(MusicTrackManager.normalizeTrackTitleKey(trackTitle));

		if (matchingTrack == null || !trackingStateService.recordTrackUnlocked(matchingTrack.getTitle()))
		{
			return 0;
		}

		unlockListener.onTrackUnlocked(matchingTrack);
		return 1;
	}

	private void recordUnlockedTrackByTitle(String rawTrackTitle, UnlockListener unlockListener)
	{
		if (rawTrackTitle == null || rawTrackTitle.isBlank())
		{
			return;
		}

		musicTrackManager.findTrackByTitle(rawTrackTitle)
			.filter(matchingTrack -> trackingStateService.recordTrackUnlocked(matchingTrack.getTitle()))
			.ifPresent(unlockListener::onTrackUnlocked);
	}
}