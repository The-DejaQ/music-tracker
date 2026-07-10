package org.dejaq.plugins.musictracker;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import org.dejaq.plugins.musictracker.builder.RouteBuilderSession;
import org.dejaq.plugins.musictracker.json.CustomTrackStore;
import org.dejaq.plugins.musictracker.navigation.AutoProgressionService;
import org.dejaq.plugins.musictracker.navigation.NavigationCoordinator;
import org.dejaq.plugins.musictracker.navigation.TrackNavigator;
import org.dejaq.plugins.musictracker.overlay.EntityHighlightOverlay;
import org.dejaq.plugins.musictracker.overlay.MusicTrackOverlay;
import org.dejaq.plugins.musictracker.overlay.MusicTrackMinimapOverlay;
import org.dejaq.plugins.musictracker.overlay.MusicTrackWorldMapOverlay;
import org.dejaq.plugins.musictracker.requirement.ItemRequirement;
import org.dejaq.plugins.musictracker.state.PlayerState;
import org.dejaq.plugins.musictracker.state.TrackingStateService;
import org.dejaq.plugins.musictracker.state.UnlockDetectionService;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.InteractionType;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.SpecialTrackRegistry;
import org.dejaq.plugins.musictracker.track.UnlockType;
import org.dejaq.plugins.musictracker.ui.MusicTrackPanel;

@PluginDescriptor(name = "Music Tracker", description = "Advanced music unlock tracker with multi-route navigation, requirements, overlays and pathing to help you earn your Music Cape.", tags = {"music", "tracker", "navigation", "overlay", "requirements", "routes", "music-cape"})
public class MusicTrackerPlugin extends Plugin
{
	private static final String CONFIG_GROUP_NAME = "music-tracker";
	private static final String PLUGIN_MESSAGE_NAMESPACE = "music-tracker";
	private static final int MUSIC_TAB_INTERFACE_GROUP_ID = InterfaceID.MUSIC;

	@Inject
	@Getter
	private Client client;
	@Inject
	@Getter
	private ClientThread clientThread;
	@Inject
	@Getter
	private MusicTrackerConfig musicTrackerConfig;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	@Getter
	private ItemManager itemManager;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	@Getter
	private PlayerState playerState;
	@Inject
	@Getter
	private TrackNavigator trackNavigator;
	@Inject
	private EntityHighlightOverlay entityHighlightOverlay;
	@Inject
	private MusicTrackWorldMapOverlay musicTrackWorldMapOverlay;
	@Inject
	private MusicTrackMinimapOverlay musicTrackMinimapOverlay;
	@Inject
	private MusicTrackManager musicTrackManager;
	@Inject
	private TrackingStateService trackingStateService;
	@Inject
	private UnlockDetectionService unlockDetectionService;
	@Inject
	private AutoProgressionService autoProgressionService;
	@Inject
	private NavigationCoordinator navigationCoordinator;

	@Inject
	@Getter
	private CustomTrackStore customTrackStore;

	@Inject
	@Getter
	private RouteBuilderSession routeBuilderSession;

	@Getter
	private MusicTrackPanel musicTrackPanel;
	private NavigationButton navigationButton;
	private MusicTrackOverlay musicTrackOverlay;

	private boolean musicTabWasOpenLastTick = false;

	@Override
	protected void startUp()
	{
		trackingStateService.loadSkippedTracksFromConfig();

		musicTrackPanel = new MusicTrackPanel(this, client, clientThread, musicTrackerConfig, musicTrackManager);

		BufferedImage navigationButtonIcon = loadNavigationButtonIcon();
		navigationButton = NavigationButton.builder()
			.tooltip("Music Tracker")
			.icon(navigationButtonIcon)
			.priority(5)
			.panel(musicTrackPanel)
			.build();
		clientToolbar.addNavigation(navigationButton);

		musicTrackOverlay = new MusicTrackOverlay(client, this, musicTrackerConfig);
		overlayManager.add(musicTrackOverlay);
		overlayManager.add(entityHighlightOverlay);
		overlayManager.add(musicTrackMinimapOverlay);
		musicTrackMinimapOverlay.startUp();
		musicTrackWorldMapOverlay.startUp(this);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navigationButton);
		overlayManager.remove(musicTrackOverlay);
		overlayManager.remove(entityHighlightOverlay);
		overlayManager.remove(musicTrackMinimapOverlay);
		musicTrackMinimapOverlay.shutDown();
		musicTrackWorldMapOverlay.shutDown();
		navigationCoordinator.clearWorldMapPoints();
		trackNavigator.clear();
		trackingStateService.setTrackingActive(false);
		customTrackStore.shutdown();
		musicTrackManager.shutdown();
	}

	private BufferedImage loadNavigationButtonIcon()
	{
		BufferedImage navigationButtonIcon = ImageUtil.loadImageResource(getClass(), "icon.png");
		if (navigationButtonIcon == null)
		{
			navigationButtonIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			Graphics2D iconGraphics = navigationButtonIcon.createGraphics();
			iconGraphics.setColor(new Color(100, 149, 237));
			iconGraphics.fillRect(0, 0, 16, 16);
			iconGraphics.dispose();
		}
		return navigationButtonIcon;
	}

	public void reloadRegions()
	{
		musicTrackManager.reloadRegionsFromJson(() -> {
			musicTrackWorldMapOverlay.rebuildWorldMapPoints();
			if (musicTrackPanel != null)
			{
				musicTrackPanel.getTrackerContentPanel().buildTracksList();
			}
			sendDebugMessage("Reloaded " + musicTrackManager.getAllTracks().size() + " Music Tracks in " + musicTrackManager.getRegionNames().size() + " Regions.");
		});
	}

	public void toggleTracking()
	{
		boolean newTrackingActiveState = !trackingStateService.isTrackingActive();
		trackingStateService.setTrackingActive(newTrackingActiveState);
		updateTrackingButtonState(newTrackingActiveState);

		if (newTrackingActiveState)
		{
			clientThread.invokeLater(() -> {
				String detectedRegionName = determineCurrentRegionFromPlayerLocation();
				if (detectedRegionName != null)
				{
					trackingStateService.setCurrentRegionName(detectedRegionName);
					SwingUtilities.invokeLater(() -> musicTrackPanel.getTrackerContentPanel().expandRegion(detectedRegionName));
				}

				if (trackNavigator.getCurrentTrack() == null)
				{
					progressToNextTrack(true);
				}
				else
				{
					trackNavigator.checkProgress();
				}

				sendDebugMessage("Music Tracker started.");
			});
		}
		else
		{
			trackNavigator.clear();
			sendDebugMessage("Music Tracker stopped.");
		}

		refreshTrackList();
	}

	public void selectTrack(MusicTrack musicTrack)
	{
		if (musicTrack == null)
		{
			return;
		}

		String unlockRestrictionMessage = musicTrack.getUnlockRestrictionMessage(trackNavigator.getCurrentRoute());
		if (unlockRestrictionMessage != null)
		{
			sendGameMessage(unlockRestrictionMessage);
			if (musicTrack.getUnlockType() == UnlockType.AUTOMATIC)
			{
				return;
			}
		}

		clientThread.invokeLater(() -> {
			trackNavigator.clearEntityHighlights();
			trackNavigator.setCurrentTrack(musicTrack);
			trackNavigator.checkProgress();
		});

		SwingUtilities.invokeLater(() -> {
			if (musicTrack.getRegion() != null)
			{
				musicTrackPanel.getTrackerContentPanel().expandRegion(musicTrack.getRegion());
			}
			musicTrackPanel.getTrackerContentPanel().refreshVisibleTracks();
		});

		if (!trackingStateService.isTrackingActive())
		{
			trackingStateService.setTrackingActive(true);
			updateTrackingButtonState(true);
			sendDebugMessage("Music Tracker started.");
		}

		trackNavigator.checkProgress();
		sendDebugMessage("Selected track: " + musicTrack.getTitle() + ".");
	}

	public void progressToNextTrack()
	{
		progressToNextTrack(false);
	}

	public void progressToNextTrack(boolean forceProgress)
	{
		if (!trackingStateService.isTrackingActive())
		{
			refreshTrackList();
			return;
		}

		if (!forceProgress && !musicTrackerConfig.autoProgress())
		{
			trackNavigator.clear();
			refreshTrackList();
			return;
		}

		Optional<MusicTrack> nextMusicTrackCandidate = findNextAutoProgressTrack();

		if (nextMusicTrackCandidate.isEmpty())
		{
			sendGameMessage(autoProgressionService.buildCompletionMessage());
			trackingStateService.setTrackingActive(false);
			trackNavigator.clear();
			updateTrackingButtonState(false);
			navigationCoordinator.clearWorldMapPoints();
			refreshTrackList();
			return;
		}

		MusicTrack nextMusicTrack = nextMusicTrackCandidate.get();
		trackNavigator.setCurrentTrack(nextMusicTrack);
		trackingStateService.setCurrentRegionName(nextMusicTrack.getRegion());
		trackNavigator.checkProgress();
		refreshTrackList();

		sendDebugMessage("Next track: " + nextMusicTrack.getTitle() + " in " + trackingStateService.getCurrentRegionName());
	}

	private Optional<MusicTrack> findNextAutoProgressTrack()
	{
		MusicTrack currentTrack = trackNavigator.getCurrentTrack();

		if (currentTrack != null && !musicTrackerConfig.stayInRegion())
		{
			Optional<MusicTrack> closestEligibleTrackAnyRegion = autoProgressionService.findNextClosestTrackToCurrent(currentTrack);
			if (closestEligibleTrackAnyRegion.isPresent())
			{
				return closestEligibleTrackAnyRegion;
			}
		}

		Optional<MusicTrack> nextMusicTrackCandidate =
			autoProgressionService.findNextAutoProgressableTrackInRegion(trackingStateService.getCurrentRegionName());

		if (nextMusicTrackCandidate.isEmpty())
		{
			Optional<String> nextPendingRegionName =
				autoProgressionService.findNextPendingRegion(trackingStateService.getCurrentRegionName());

			if (nextPendingRegionName.isPresent())
			{
				trackingStateService.setCurrentRegionName(nextPendingRegionName.get());
				nextMusicTrackCandidate = autoProgressionService.findNextAutoProgressableTrackInRegion(trackingStateService.getCurrentRegionName());
			}
		}

		return nextMusicTrackCandidate;
	}

	public boolean isTrackingActive()
	{
		return trackingStateService.isTrackingActive();
	}

	private void updateTrackingButtonState(boolean trackingActive)
	{
		SwingUtilities.invokeLater(() -> musicTrackPanel.getTrackerContentPanel().updateTrackingButton(trackingActive));
	}

	private String determineCurrentRegionFromPlayerLocation()
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return null;
		}
		WorldPoint localPlayerLocation = localPlayer.getWorldLocation();
		return musicTrackManager.getAllTracks().stream()
			.filter(candidateTrack -> !trackingStateService.isTrackSkipped(candidateTrack.getTitle()))
			.min(Comparator.comparingDouble(candidateTrack -> musicTrackManager.distanceTo(localPlayerLocation, candidateTrack.getUnlockPoint())))
			.map(MusicTrack::getRegion)
			.orElse(null);
	}

	public void setSkipped(String trackTitle, boolean skipped)
	{
		trackingStateService.setTrackSkipped(trackTitle, skipped);

		if (skipped && trackingStateService.isTrackingActive()
			&& trackNavigator.getCurrentTrack() != null
			&& trackNavigator.getCurrentTrack().getTitle().equals(trackTitle))
		{
			clientThread.invokeLater(() -> progressToNextTrack(true));
		}

		refreshTrackList();
	}

	public boolean isSkipped(String trackTitle)
	{
		return trackingStateService.isTrackSkipped(trackTitle);
	}

	public void clearSkippedTracks()
	{
		trackingStateService.clearSkippedTracks();
		refreshTrackList();
		sendDebugMessage("Cleared all skipped tracks.");
	}

	public boolean isTrackUnlocked(String trackTitle)
	{
		return trackingStateService.isTrackUnlocked(trackTitle);
	}

	public Set<String> getCurrentUnlockedTracks()
	{
		return trackingStateService.getCurrentUnlockedTrackTitles();
	}

	public void clearUnlockedTracks()
	{
		trackingStateService.clearUnlockedTracks();
	}

	public MusicTrack getNextSuggestedTrack()
	{
		if (!musicTrackerConfig.autoProgress() || trackNavigator.getCurrentTrack() == null)
		{
			return null;
		}
		return autoProgressionService.findNextClosestTrackToCurrent(trackNavigator.getCurrentTrack()).orElse(null);
	}

	public void refreshAll()
	{
		refreshTrackList();

		clientThread.invokeLater(() -> {
			syncUnlockedTracksFromMusicTabAndNotify();
			if (trackingStateService.isTrackingActive() && trackNavigator.getCurrentTrack() != null)
			{
				trackNavigator.checkProgress();
			}
		});
	}

	private void refreshTrackList()
	{
		if (musicTrackPanel != null)
		{
			SwingUtilities.invokeLater(() -> musicTrackPanel.getTrackerContentPanel().refreshVisibleTracks());
		}
		musicTrackWorldMapOverlay.rebuildWorldMapPoints();
	}

	private void syncUnlockedTracksFromMusicTabAndNotify()
	{
		unlockDetectionService.beginMusicTabSync(this::onTrackUnlockedDuringBulkSync, newlyUnlockedTrackCount -> {
			if (newlyUnlockedTrackCount > 0)
			{
				refreshTrackList();
				sendDebugMessage("Synced " + newlyUnlockedTrackCount + " unlocked tracks from Music tab.");
			}
		});
	}

	private void onTrackUnlockedDuringBulkSync(MusicTrack unlockedMusicTrack)
	{
		if (trackingStateService.isTrackingActive()
			&& trackNavigator.getCurrentTrack() != null
			&& trackNavigator.getCurrentTrack().getTitle().equalsIgnoreCase(unlockedMusicTrack.getTitle()))
		{
			clientThread.invokeLater(() -> progressToNextTrack(false));
		}
	}

	private void onTrackUnlocked(MusicTrack unlockedMusicTrack)
	{
		refreshTrackList();

		if (musicTrackerConfig.lockedTracks())
		{
			sendGameMessage("You have unlocked a new music track: <col=e00a19>" + unlockedMusicTrack.getTitle(), false);
		}

		if (trackingStateService.isTrackingActive()
			&& trackNavigator.getCurrentTrack() != null
			&& trackNavigator.getCurrentTrack().getTitle().equalsIgnoreCase(unlockedMusicTrack.getTitle()))
		{
			clientThread.invokeLater(() -> progressToNextTrack(false));
		}
	}

	public boolean playerHasItem(ItemRequirement itemRequirement)
	{
		if (itemRequirement.isItemCollection())
		{
			return playerState.hasAnyItemFromGroup(itemRequirement.getGroupItemIds(), itemRequirement.getQuantity());
		}
		if (itemRequirement.isNameBased())
		{
			return playerState.hasItemByPartialName(itemRequirement.getItem(), itemRequirement.getQuantity(), itemManager);
		}
		return playerState.hasItemQuantity(itemRequirement.getItemId(), itemRequirement.getQuantity());
	}

	public void sendGameMessage(String message)
	{
		sendGameMessage(message, true);
	}

	public void sendGameMessage(String message, boolean includeSenderName)
	{
		clientThread.invokeLater(() -> {
			Color nameColor = musicTrackerConfig.messageNameColor();
			Color textColor = musicTrackerConfig.messageTextColor();

			String prefix = includeSenderName
				? "<col=" + String.format("%02x%02x%02x", nameColor.getRed(), nameColor.getGreen(), nameColor.getBlue()) + ">[MusicTracker]:</col> "
				: "";

			String coloredMessage = "<col=" + String.format("%02x%02x%02x", textColor.getRed(), textColor.getGreen(), textColor.getBlue()) + ">" + message + "</col>";

			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", prefix + coloredMessage, null);
		});
	}

	private void sendDebugMessage(String message)
	{
		if (musicTrackerConfig.debugData())
		{
			sendGameMessage("[Debug] " + message);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged varbitChangedEvent)
	{
		int varbitId = varbitChangedEvent.getVarbitId();
		if (varbitId != VarbitID.MUSIC_UNLOCK_TEXT_TOGGLE && varbitId != VarbitID.MUSIC_AREA_MODE)
		{
			return;
		}

		boolean newVarbitValue = varbitChangedEvent.getValue() == 0;
		Boolean previousVarbitValue = playerState.getVarbitData().put(varbitId, newVarbitValue);
		boolean varbitValueChanged = (previousVarbitValue == null) || (previousVarbitValue != newVarbitValue);

		if (varbitValueChanged && musicTrackPanel != null)
		{
			musicTrackPanel.refreshState();
			musicTrackPanel.getSettingsRequiredPanel().update(this);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChangedEvent)
	{
		GameState newGameState = gameStateChangedEvent.getGameState();
		boolean gameStateChanged = (playerState.getGameState() != newGameState);
		playerState.setGameState(newGameState);

		if (newGameState == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(() -> {
				playerState.refreshTrackingVarbits();
				playerState.refreshItemQuantityCaches();

				if (gameStateChanged)
				{
					SwingUtilities.invokeLater(() -> {
						musicTrackPanel.refreshState();
						musicTrackPanel.getTrackerContentPanel().refreshVisibleTracks();
					});
				}
			});

			trackingStateService.loadSkippedTracksFromConfig();
			syncUnlockedTracksFromMusicTabAndNotify();
		}
		else if (gameStateChanged)
		{
			playerState.reset();

			if (newGameState == GameState.LOGIN_SCREEN)
			{
				trackNavigator.clear();
				trackingStateService.resetAccountScopedState();
			}

			SwingUtilities.invokeLater(() -> {
				musicTrackPanel.refreshState();
				musicTrackPanel.getTrackerContentPanel().refreshVisibleTracks();
			});
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged itemContainerChangedEvent)
	{
		if (itemContainerChangedEvent.getContainerId() == InventoryID.INV
			|| itemContainerChangedEvent.getContainerId() == InventoryID.WORN)
		{
			playerState.refreshItemQuantityCaches();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged statChangedEvent)
	{
		playerState.updateRealSkillLevel(statChangedEvent.getSkill(), statChangedEvent.getLevel());
	}

	@Subscribe
	public void onGameTick(GameTick gameTickEvent)
	{
		boolean isMusicTabCurrentlyOpen = unlockDetectionService.isMusicTabOpen();
		boolean musicTabJustOpened = isMusicTabCurrentlyOpen && !musicTabWasOpenLastTick;
		musicTabWasOpenLastTick = isMusicTabCurrentlyOpen;

		if (musicTabJustOpened)
		{
			clientThread.invokeLater(this::syncUnlockedTracksFromMusicTabAndNotify);
		}

		if (trackingStateService.isTrackingActive())
		{
			trackNavigator.checkProgress();
		}
		unlockDetectionService.pollNowPlayingTrack(this::onTrackUnlocked);
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessageEvent)
	{
		unlockDetectionService.handleChatMessage(chatMessageEvent, this::onTrackUnlocked);
		notifySpecialTrackHandlerOfChatMessage(chatMessageEvent);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded menuEntryAddedEvent)
	{
		handleMusicTabTrackShortcut(menuEntryAddedEvent);
		handleRouteBuilderMenuEntries(menuEntryAddedEvent);
	}

	private void notifySpecialTrackHandlerOfChatMessage(ChatMessage chatMessageEvent)
	{
		if (chatMessageEvent.getType() != ChatMessageType.GAMEMESSAGE && chatMessageEvent.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		MusicTrack currentlyTrackedMusicTrack = trackNavigator.getCurrentTrack();
		if (currentlyTrackedMusicTrack == null)
		{
			return;
		}

		Route currentRoute = trackNavigator.getCurrentRoute();
		SpecialTrackRegistry.getHandler(currentlyTrackedMusicTrack, currentRoute)
			.onChatMessage(currentlyTrackedMusicTrack, currentRoute, chatMessageEvent, this);
	}

	private void handleMusicTabTrackShortcut(MenuEntryAdded menuEntryAddedEvent)
	{
		if (WidgetUtil.componentToInterface(menuEntryAddedEvent.getActionParam1()) != MUSIC_TAB_INTERFACE_GROUP_ID)
		{
			return;
		}
		if (!menuEntryAddedEvent.getOption().equals("Play"))
		{
			return;
		}

		client.getMenu().createMenuEntry(-1)
			.setParam0(menuEntryAddedEvent.getActionParam0())
			.setParam1(menuEntryAddedEvent.getActionParam1())
			.setTarget(menuEntryAddedEvent.getTarget())
			.setOption("Track")
			.setType(MenuAction.RUNELITE)
			.setIdentifier(menuEntryAddedEvent.getIdentifier())
			.onClick(clickedMenuEntry -> {
				String trackTitle = Text.removeTags(menuEntryAddedEvent.getTarget()).trim();
				if (musicTrackPanel != null && musicTrackPanel.getTrackerContentPanel() != null)
				{
					musicTrackPanel.getTrackerContentPanel().clearSearch();
				}
				musicTrackManager.findTrackByTitle(trackTitle).ifPresent(this::selectTrack);
			});
	}

	private void handleRouteBuilderMenuEntries(MenuEntryAdded menuEntryAddedEvent)
	{
		if (!musicTrackerConfig.enableTrackBuilder() || !routeBuilderSession.isActive())
		{
			return;
		}

		MenuAction menuAction = MenuAction.of(menuEntryAddedEvent.getType());

		if (menuAction == MenuAction.WALK || menuAction == MenuAction.SET_HEADING)
		{
			addRouteBuilderTileMenu(menuEntryAddedEvent);
		}
		else if (isGameObjectMenuAction(menuAction))
		{
			addRouteBuilderObjectMenu(menuEntryAddedEvent);
		}
		else if (isNpcMenuAction(menuAction))
		{
			addRouteBuilderNpcMenu(menuEntryAddedEvent);
		}
	}

	private boolean isGameObjectMenuAction(MenuAction menuAction)
	{
		return menuAction == MenuAction.GAME_OBJECT_FIRST_OPTION
			|| menuAction == MenuAction.GAME_OBJECT_SECOND_OPTION
			|| menuAction == MenuAction.GAME_OBJECT_THIRD_OPTION
			|| menuAction == MenuAction.GAME_OBJECT_FOURTH_OPTION
			|| menuAction == MenuAction.GAME_OBJECT_FIFTH_OPTION
			|| menuAction == MenuAction.EXAMINE_OBJECT;
	}

	private boolean isNpcMenuAction(MenuAction menuAction)
	{
		return menuAction == MenuAction.NPC_FIRST_OPTION
			|| menuAction == MenuAction.NPC_SECOND_OPTION
			|| menuAction == MenuAction.NPC_THIRD_OPTION
			|| menuAction == MenuAction.NPC_FOURTH_OPTION
			|| menuAction == MenuAction.NPC_FIFTH_OPTION;
	}

	private void addRouteBuilderTileMenu(MenuEntryAdded menuEntryAddedEvent)
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return;
		}

		// TODO Modern
		Tile selectedSceneTile = client.getSelectedSceneTile();
		WorldPoint tileWorldPoint = selectedSceneTile != null
			? selectedSceneTile.getWorldLocation()
			: WorldPoint.fromScene(client, menuEntryAddedEvent.getActionParam0(), menuEntryAddedEvent.getActionParam1(), localPlayer.getWorldView().getPlane());

		if (tileWorldPoint == null)
		{
			return;
		}

		MenuEntry routeBuilderParentEntry = client.getMenu().createMenuEntry(-1)
			.setOption("Route Builder")
			.setTarget("")
			.setType(MenuAction.RUNELITE);

		Menu routeBuilderSubMenu = routeBuilderParentEntry.createSubMenu();

		routeBuilderSubMenu.createMenuEntry(-1)
			.setOption("Set Step Destination")
			.setType(MenuAction.RUNELITE)
			.onClick(clickedEntry -> {
				routeBuilderSession.setCurrentStepDestination(tileWorldPoint);
				sendGameMessage("[Builder] Step destination set to " + tileWorldPoint);
			});

		routeBuilderSubMenu.createMenuEntry(-1)
			.setOption("Add Tile Interaction")
			.setType(MenuAction.RUNELITE)
			.onClick(clickedEntry -> promptForTileInteraction(tileWorldPoint));

		routeBuilderSubMenu.createMenuEntry(-1)
			.setOption("Finish Step")
			.setType(MenuAction.RUNELITE)
			.onClick(clickedEntry -> promptForFinishStep());
	}

	private void promptForTileInteraction(WorldPoint tileWorldPoint)
	{
		chatboxPanelManager.openTextInput("Hint (optional):")
			.onDone(hintInput -> {
				String cleanHint = (hintInput == null || hintInput.isBlank()) ? null : hintInput.trim();
				clientThread.invokeLater(() -> {
					InteractionTarget interactionTarget = InteractionTarget.builder()
						.entityId(-1)
						.location(tileWorldPoint)
						.type(InteractionType.TILE)
						.hint(cleanHint)
						.build();

					routeBuilderSession.addInteractionToCurrentStep(interactionTarget);
					sendGameMessage("[Builder] Added tile interaction at " + tileWorldPoint);
				});
			})
			.build();
	}

	private void addRouteBuilderObjectMenu(MenuEntryAdded menuEntryAddedEvent)
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return;
		}

		int objectId = menuEntryAddedEvent.getIdentifier();
		String objectName = Text.removeTags(menuEntryAddedEvent.getTarget());
		WorldPoint objectWorldPoint = resolveObjectWorldLocation(objectId, menuEntryAddedEvent.getActionParam0(), menuEntryAddedEvent.getActionParam1(), localPlayer.getWorldView().getPlane());

		MenuEntry routeBuilderParentEntry = client.getMenu().createMenuEntry(-1)
			.setOption("Route Builder")
			.setTarget(objectName)
			.setType(MenuAction.RUNELITE);

		Menu routeBuilderSubMenu = routeBuilderParentEntry.createSubMenu();

		routeBuilderSubMenu.createMenuEntry(-1)
			.setOption("Add Object Interaction")
			.setType(MenuAction.RUNELITE)
			.onClick(clickedEntry -> promptForObjectInteraction(objectId, objectName, objectWorldPoint, false));

		routeBuilderSubMenu.createMenuEntry(-1)
			.setOption("Add Named Object Interaction")
			.setType(MenuAction.RUNELITE)
			.onClick(clickedEntry -> promptForObjectInteraction(objectId, objectName, objectWorldPoint, true));

		routeBuilderSubMenu.createMenuEntry(-1)
			.setOption("Finish Step")
			.setType(MenuAction.RUNELITE)
			.onClick(clickedEntry -> promptForFinishStep());
	}

	private void promptForObjectInteraction(int objectId, String objectName, WorldPoint objectWorldPoint, boolean matchByName)
	{
		promptForActionsThenHint((actions, hint) -> {
			InteractionTarget.InteractionTargetBuilder interactionTargetBuilder = InteractionTarget.builder()
				.location(objectWorldPoint)
				.type(InteractionType.GAME_OBJECT)
				.hint(hint);

			if (matchByName)
			{
				interactionTargetBuilder.entityId(-1).entity(objectName);
			}
			else
			{
				interactionTargetBuilder.entityId(objectId);
			}

			if (!actions.isEmpty())
			{
				interactionTargetBuilder.actions(actions);
			}

			routeBuilderSession.addInteractionToCurrentStep(interactionTargetBuilder.build());
			sendGameMessage("[Builder] Added object interaction (" + objectName + ")");
		});
	}

	private WorldPoint resolveObjectWorldLocation(int objectId, int sceneX, int sceneY, int plane)
	{
		if (client.getScene() == null || client.getScene().getTiles() == null)
		{
			return null;
		}

		Tile tile = client.getScene().getTiles()[plane][sceneX][sceneY];
		if (tile == null)
		{
			return null;
		}

		for (GameObject gameObject : tile.getGameObjects())
		{
			if (gameObject != null && gameObject.getId() == objectId)
			{
				return gameObject.getWorldLocation();
			}
		}

		if (tile.getDecorativeObject() != null && tile.getDecorativeObject().getId() == objectId)
		{
			return tile.getDecorativeObject().getWorldLocation();
		}

		return WorldPoint.fromScene(client, sceneX, sceneY, plane);
	}

	private void addRouteBuilderNpcMenu(MenuEntryAdded menuEntryAddedEvent)
	{
		int npcIndex = menuEntryAddedEvent.getIdentifier();
		String npcName = Text.removeTags(menuEntryAddedEvent.getTarget());

		MenuEntry routeBuilderParentEntry = client.getMenu().createMenuEntry(-1)
			.setOption("Route Builder")
			.setTarget(npcName)
			.setType(MenuAction.RUNELITE);

		Menu routeBuilderSubMenu = routeBuilderParentEntry.createSubMenu();

		routeBuilderSubMenu.createMenuEntry(-1)
			.setOption("Add NPC Interaction")
			.setType(MenuAction.RUNELITE)
			.onClick(clickedEntry -> promptForNpcInteraction(npcIndex, npcName));

		routeBuilderSubMenu.createMenuEntry(-1)
			.setOption("Finish Step")
			.setType(MenuAction.RUNELITE)
			.onClick(clickedEntry -> promptForFinishStep());
	}

	private void promptForNpcInteraction(int npcIndex, String npcName)
	{
		promptForActionsThenHint((actions, hint) -> {
			NPC npc = findNpcByIndex(npcIndex);
			WorldPoint npcWorldPoint = npc != null ? npc.getWorldLocation()
				: (client.getLocalPlayer() != null ? client.getLocalPlayer().getWorldLocation() : null);

			InteractionTarget.InteractionTargetBuilder interactionTargetBuilder = InteractionTarget.builder()
				.entityId(-1)
				.entity(npcName)
				.location(npcWorldPoint)
				.type(InteractionType.NPC)
				.hint(hint);

			if (!actions.isEmpty())
			{
				interactionTargetBuilder.actions(actions);
			}

			routeBuilderSession.addInteractionToCurrentStep(interactionTargetBuilder.build());
			sendGameMessage("[Builder] Added NPC interaction (" + npcName + ")");
		});
	}

	private NPC findNpcByIndex(int npcIndex)
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return null;
		}
		return localPlayer.getWorldView().npcs().stream()
			.filter(npc -> npc != null && npc.getIndex() == npcIndex)
			.findFirst()
			.orElse(null);
	}

	private void promptForFinishStep()
	{
		if (!routeBuilderSession.hasInProgressStepContent())
		{
			sendGameMessage("[Builder] Nothing to finish yet.");
			return;
		}

		chatboxPanelManager.openTextInput("Step Name (optional):")
			.onDone((Consumer<String>) stepNameInput -> clientThread.invokeLater(() -> {
				boolean stepFinished = routeBuilderSession.finishCurrentStep(stepNameInput);
				sendGameMessage(stepFinished
					? "[Builder] Step finished."
					: "[Builder] Nothing to finish yet.");
			}))
			.build();
	}

	private void promptForActionsThenHint(BiConsumer<List<String>, String> onBothCollected)
	{
		chatboxPanelManager.openTextInput("Menu Actions (comma-separated, optional):")
			.onDone(actionsInput -> {
				List<String> parsedActions = parseCommaSeparatedActions(actionsInput);
				clientThread.invokeLater(() -> chatboxPanelManager.openTextInput("Hint (optional):")
					.onDone(hintInput -> {
						String cleanHint = (hintInput == null || hintInput.isBlank()) ? null : hintInput.trim();
						clientThread.invokeLater(() -> onBothCollected.accept(parsedActions, cleanHint));
					})
					.build());
			})
			.build();
	}

	private List<String> parseCommaSeparatedActions(String actionsInput)
	{
		if (actionsInput == null || actionsInput.isBlank())
		{
			return List.of();
		}
		List<String> parsedActions = new ArrayList<>();
		for (String actionToken : actionsInput.split(","))
		{
			String trimmedAction = actionToken.trim();
			if (!trimmedAction.isEmpty())
			{
				parsedActions.add(trimmedAction);
			}
		}
		return parsedActions;
	}

	@Subscribe
	public void onPluginMessage(PluginMessage pluginMessageEvent)
	{
		if (!pluginMessageEvent.getNamespace().equals(PLUGIN_MESSAGE_NAMESPACE))
		{
			return;
		}

		if (pluginMessageEvent.getName().equals("getStage"))
		{
			MusicTrack currentMusicTrack = trackNavigator.getCurrentTrack();
			String currentTrackTitle = currentMusicTrack == null ? "NOT_TRACKING" : currentMusicTrack.getTitle();
			int currentStageIndex = currentMusicTrack == null ? -1 : trackNavigator.getCurrentStage();
			sendDebugMessage("Current MusicTrack[" + currentTrackTitle + "] Stage: " + currentStageIndex);
			return;
		}

		if (pluginMessageEvent.getName().equals("selectTrack"))
		{
			Map<String, Object> messageData = pluginMessageEvent.getData();
			if (messageData == null)
			{
				return;
			}
			Object trackTitleValue = messageData.get("title");
			if (trackTitleValue instanceof String)
			{
				musicTrackManager.findTrackByTitle((String) trackTitleValue).ifPresent(matchingMusicTrack -> {
					selectTrack(matchingMusicTrack);
					sendDebugMessage("Started tracking via external plugin.");
				});
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChangedEvent)
	{
		if (!configChangedEvent.getGroup().equals(CONFIG_GROUP_NAME))
		{
			return;
		}

		switch (configChangedEvent.getKey())
		{
			case "lockedTracks":
				refreshTrackList();
				break;

			case "showProgress":
				musicTrackPanel.getTrackerContentPanel().setShowProgress(musicTrackerConfig.showProgress());
				break;

			case "saveSkippedTracks":
				if (!musicTrackerConfig.saveSkippedTracks())
				{
					trackingStateService.disableAndWipeSkippedTracks();
				}
				else
				{
					trackingStateService.loadSkippedTracksFromConfig();
				}
				refreshTrackList();
				break;

			case "hideUnlockedTracks":
			case "hideMissingLevel":
			case "hideMissingQuest":
			case "hideMembersTracks":
			case "hideFilteredHeaders":
				if (musicTrackPanel != null)
				{
					SwingUtilities.invokeLater(() -> musicTrackPanel.getTrackerContentPanel().refreshVisibleTracks());
				}
				break;

			case "enableTrackBuilder":
				if (musicTrackPanel != null)
				{
					SwingUtilities.invokeLater(musicTrackPanel::refreshBuilderTabVisibility);
				}
				break;

			case "showWorldMapOverlay":
				musicTrackWorldMapOverlay.rebuildWorldMapPoints();
				break;
		}
	}

	@Provides
	MusicTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MusicTrackerConfig.class);
	}
}