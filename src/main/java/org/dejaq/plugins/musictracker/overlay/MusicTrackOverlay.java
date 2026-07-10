package org.dejaq.plugins.musictracker.overlay;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerConfig;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.overlay.components.RequirementSectionRenderer;
import org.dejaq.plugins.musictracker.quest.Quest;
import org.dejaq.plugins.musictracker.quest.QuestState;
import org.dejaq.plugins.musictracker.requirement.ItemRequirement;
import org.dejaq.plugins.musictracker.requirement.LevelRequirement;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.SpecialTrackRegistry;
import org.dejaq.plugins.musictracker.track.special.SpecialTrackHandler;

@Slf4j
public class MusicTrackOverlay extends Overlay
{
	private static final int MINIMUM_PANEL_WIDTH = 200;
	private static final int WIDTH_SAFETY_MARGIN_PIXELS = 20;

	private final Client client;
	private final MusicTrackerPlugin musicTrackerPlugin;
	private final MusicTrackerConfig musicTrackerConfig;
	private final RequirementSectionRenderer requirementSectionRenderer = new RequirementSectionRenderer();

	private final PanelComponent panelComponent = new PanelComponent();

	private MusicTrack cachedNextSuggestedTrack;
	private MusicTrack cachedNextSuggestedTrackForCurrentTrack;
	private int cachedNextSuggestedTrackAtTick = -1;

	@Inject
	public MusicTrackOverlay(Client client, MusicTrackerPlugin musicTrackerPlugin, MusicTrackerConfig musicTrackerConfig)
	{
		this.client = client;
		this.musicTrackerPlugin = musicTrackerPlugin;
		this.musicTrackerConfig = musicTrackerConfig;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setResizable(true);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!musicTrackerPlugin.isTrackingActive())
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder().text("Music Tracker").color(Color.WHITE).build());

		MusicTrack currentMusicTrack = musicTrackerPlugin.getTrackNavigator().getCurrentTrack();

		int requiredPanelWidth = MINIMUM_PANEL_WIDTH;

		if (currentMusicTrack == null || currentMusicTrack.getTitle() == null)
		{
			panelComponent.getChildren().add(TitleComponent.builder().text("Please select a Music Track").color(Color.LIGHT_GRAY).build());
		}
		else
		{
			requiredPanelWidth = Math.max(requiredPanelWidth, computeRequiredPanelWidth(graphics, currentMusicTrack));
			renderCurrentTrackDetails(currentMusicTrack);
		}

		panelComponent.setPreferredSize(new Dimension(requiredPanelWidth, 0));
		return panelComponent.render(graphics);
	}

	private int computeRequiredPanelWidth(Graphics2D graphics, MusicTrack currentMusicTrack)
	{
		FontMetrics fontMetrics = graphics.getFontMetrics();
		int requiredWidth = MINIMUM_PANEL_WIDTH;

		Route currentRoute = resolveCurrentRoute(currentMusicTrack);
		String trackingValueText = currentMusicTrack.getTitle()
			+ (currentRoute != null && currentMusicTrack.hasMultipleRoutes() ? " [" + currentRoute.getName() + "]" : "");
		int trackingLineWidth = fontMetrics.stringWidth("Tracking:") + fontMetrics.stringWidth(trackingValueText);
		requiredWidth = Math.max(requiredWidth, trackingLineWidth + WIDTH_SAFETY_MARGIN_PIXELS);

		if (musicTrackerConfig.showNextTrackOverlay())
		{
			try
			{
				MusicTrack nextSuggestedTrack = getCachedNextSuggestedTrack(currentMusicTrack);
				if (nextSuggestedTrack != null && nextSuggestedTrack.getTitle() != null && !nextSuggestedTrack.getTitle().equals(currentMusicTrack.getTitle()))
				{
					int nextLineWidth = fontMetrics.stringWidth("Next:") + fontMetrics.stringWidth(nextSuggestedTrack.getTitle());
					requiredWidth = Math.max(requiredWidth, nextLineWidth + WIDTH_SAFETY_MARGIN_PIXELS);
				}
			}
			catch (Exception exception)
			{
				log.debug("Error getting next track while sizing overlay", exception);
			}
		}

		return requiredWidth;
	}

	private Route resolveCurrentRoute(MusicTrack currentMusicTrack)
	{
		Route currentRoute = musicTrackerPlugin.getTrackNavigator().getCurrentRoute();
		if (currentRoute == null)
		{
			currentRoute = currentMusicTrack.getDefaultRoute();
		}
		return currentRoute;
	}

	private void renderCurrentTrackDetails(MusicTrack currentMusicTrack)
	{
		Route currentRoute = resolveCurrentRoute(currentMusicTrack);
		if (currentRoute == null)
		{
			renderNoRouteTrackDetails(currentMusicTrack);
			return;
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Tracking:")
			.right(currentMusicTrack.getTitle() + (currentMusicTrack.hasMultipleRoutes() ? " [" + currentRoute.getName() + "]" : ""))
			.leftColor(Color.WHITE)
			.rightColor(ColorScheme.PROGRESS_INPROGRESS_COLOR)
			.build());

		if (musicTrackerConfig.showRegionOverlay())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Region:")
				.right(currentMusicTrack.getRegion() != null ? currentMusicTrack.getRegion() : "Unknown")
				.leftColor(Color.WHITE)
				.rightColor(Color.LIGHT_GRAY)
				.build());
		}

		if (musicTrackerConfig.showLocationOverlay())
		{
			panelComponent.getChildren().add(LineComponent.builder().left("Location:").leftColor(Color.WHITE).build());
			panelComponent.getChildren().add(LineComponent.builder()
				.left(currentMusicTrack.getLocation() != null ? currentMusicTrack.getLocation() : "Gielinor")
				.leftColor(Color.LIGHT_GRAY)
				.build());
		}

		SpecialTrackHandler specialTrackHandler = SpecialTrackRegistry.getHandler(currentMusicTrack, currentRoute);

		if (musicTrackerConfig.showQuestOverlay())
		{
			requirementSectionRenderer.renderQuestSection(panelComponent, musicTrackerPlugin,
				specialTrackHandler.getDynamicQuest(currentMusicTrack, currentRoute, musicTrackerPlugin), currentRoute);
		}

		if (musicTrackerConfig.showItemsOverlay())
		{
			requirementSectionRenderer.renderRequirementList(
				panelComponent,
				"Items Required:",
				specialTrackHandler.getDynamicItems(currentMusicTrack, currentRoute, musicTrackerPlugin),
				currentRoute.getItems(),
				itemRequirement -> itemRequirement.getDisplayText(musicTrackerPlugin.getItemManager()),
				musicTrackerPlugin::playerHasItem,
				ColorScheme.PROGRESS_ERROR_COLOR,
				ColorScheme.PROGRESS_ERROR_COLOR,
				ItemRequirement::hasItem);
		}

		if (musicTrackerConfig.recommendedItemsOverlay())
		{
			requirementSectionRenderer.renderRequirementList(
				panelComponent,
				"Items Recommended:",
				specialTrackHandler.getDynamicItemRecommendations(currentMusicTrack, currentRoute, musicTrackerPlugin),
				currentRoute.getRecommendedItems(),
				itemRequirement -> itemRequirement.getDisplayText(musicTrackerPlugin.getItemManager()),
				musicTrackerPlugin::playerHasItem,
				ColorScheme.PROGRESS_ERROR_COLOR,
				ColorScheme.PROGRESS_INPROGRESS_COLOR,
				ItemRequirement::hasItem);
		}

		if (musicTrackerConfig.showLevelsOverlay())
		{
			requirementSectionRenderer.renderRequirementList(
				panelComponent,
				"Levels Required:",
				specialTrackHandler.getDynamicLevels(currentMusicTrack, currentRoute, musicTrackerPlugin),
				currentRoute.getLevels(),
				this::formatLevelRequirement,
				levelRequirement -> client.getRealSkillLevel(levelRequirement.getSkill()) >= levelRequirement.getLevel(),
				ColorScheme.PROGRESS_ERROR_COLOR,
				ColorScheme.PROGRESS_ERROR_COLOR);
		}

		if (musicTrackerConfig.recommendedLevelsOverlay())
		{
			requirementSectionRenderer.renderRequirementList(
				panelComponent,
				"Levels Recommended:",
				specialTrackHandler.getDynamicLevelRecommendations(currentMusicTrack, currentRoute, musicTrackerPlugin),
				currentRoute.getRecommendedLevels(),
				this::formatLevelRequirement,
				levelRequirement -> client.getRealSkillLevel(levelRequirement.getSkill()) >= levelRequirement.getLevel(),
				ColorScheme.PROGRESS_INPROGRESS_COLOR,
				ColorScheme.PROGRESS_INPROGRESS_COLOR);
		}

		if (musicTrackerConfig.showNotesOverlay())
		{
			requirementSectionRenderer.renderNotesSection(panelComponent,
				specialTrackHandler.getDynamicNotes(currentMusicTrack, currentRoute, musicTrackerPlugin), currentRoute);
		}

		if (musicTrackerConfig.showUnlockHintOverlay())
		{
			renderUnlockHintSection(currentMusicTrack);
		}

		if (musicTrackerConfig.showDistanceOverlay())
		{
			renderDistanceSection(currentMusicTrack);
		}

		if (musicTrackerConfig.showNextTrackOverlay())
		{
			renderNextTrackSection(currentMusicTrack);
		}
	}

	private void renderNoRouteTrackDetails(MusicTrack currentMusicTrack)
	{
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Tracking:")
			.right(currentMusicTrack.getTitle())
			.leftColor(Color.WHITE)
			.rightColor(new Color(0, 176, 255))
			.build());

		if (musicTrackerConfig.showRegionOverlay())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Region:")
				.right(currentMusicTrack.getRegion() != null ? currentMusicTrack.getRegion() : "Unknown")
				.leftColor(Color.WHITE)
				.rightColor(Color.LIGHT_GRAY)
				.build());
		}

		if (musicTrackerConfig.showLocationOverlay())
		{
			panelComponent.getChildren().add(LineComponent.builder().left("Location:").leftColor(Color.WHITE).build());
			panelComponent.getChildren().add(LineComponent.builder()
				.left(currentMusicTrack.getLocation() != null ? currentMusicTrack.getLocation() : "Gielinor")
				.leftColor(Color.LIGHT_GRAY)
				.build());
		}

		if (musicTrackerConfig.showQuestOverlay())
		{
			renderNoRouteQuestSection(currentMusicTrack);
		}

		if (musicTrackerConfig.showUnlockHintOverlay())
		{
			renderUnlockHintSection(currentMusicTrack);
		}
	}

	private void renderNoRouteQuestSection(MusicTrack currentMusicTrack)
	{
		Quest requiredQuest = currentMusicTrack.getEffectiveUnlockQuest(null);
		if (requiredQuest == null)
		{
			return;
		}

		requirementSectionRenderer.addSpace(panelComponent);
		panelComponent.getChildren().add(LineComponent.builder().left("Quest Required:").leftColor(Color.WHITE).build());

		QuestState questState = musicTrackerPlugin.getPlayerState().getCachedQuestState(requiredQuest);

		Color questLineColor = questState == QuestState.FINISHED
			? ColorScheme.PROGRESS_COMPLETE_COLOR
			: ColorScheme.PROGRESS_ERROR_COLOR;
		panelComponent.getChildren().add(LineComponent.builder().left(requiredQuest.getName()).leftColor(questLineColor).build());
	}

	private String formatLevelRequirement(LevelRequirement levelRequirement)
	{
		return levelRequirement.getLevel() + " " + levelRequirement.getSkill().getName();
	}

	private void renderUnlockHintSection(MusicTrack currentMusicTrack)
	{
		String unlockHintText = currentMusicTrack.getOverlayUnlockHintText();
		if (unlockHintText == null)
		{
			return;
		}
		requirementSectionRenderer.addSpace(panelComponent);
		panelComponent.getChildren().add(LineComponent.builder().left("Unlock Hint:").leftColor(Color.WHITE).build());
		panelComponent.getChildren().add(LineComponent.builder().left(unlockHintText).leftColor(Color.LIGHT_GRAY).build());
	}

	private void renderDistanceSection(MusicTrack currentMusicTrack)
	{
		WorldPoint targetWorldPoint = currentMusicTrack.getDestinationForStage(musicTrackerPlugin.getTrackNavigator().getCurrentStage());
		if (targetWorldPoint == null)
		{
			return;
		}

		int distanceInTiles = client.getLocalPlayer().getWorldLocation().distanceTo(targetWorldPoint);
		if (distanceInTiles <= 0 || distanceInTiles == Integer.MAX_VALUE)
		{
			return;
		}

		requirementSectionRenderer.addSpace(panelComponent);
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Distance:")
			.right(distanceInTiles + " tiles")
			.leftColor(Color.WHITE)
			.rightColor(Color.CYAN)
			.build());
	}

	private MusicTrack getCachedNextSuggestedTrack(MusicTrack currentMusicTrack)
	{
		int currentTick = client.getTickCount();
		if (currentTick == cachedNextSuggestedTrackAtTick && currentMusicTrack == cachedNextSuggestedTrackForCurrentTrack)
		{
			return cachedNextSuggestedTrack;
		}

		cachedNextSuggestedTrack = musicTrackerPlugin.getNextSuggestedTrack();
		cachedNextSuggestedTrackForCurrentTrack = currentMusicTrack;
		cachedNextSuggestedTrackAtTick = currentTick;
		return cachedNextSuggestedTrack;
	}

	private void renderNextTrackSection(MusicTrack currentMusicTrack)
	{
		try
		{
			MusicTrack nextSuggestedTrack = getCachedNextSuggestedTrack(currentMusicTrack);
			if (nextSuggestedTrack != null && nextSuggestedTrack.getTitle() != null && !nextSuggestedTrack.getTitle().equals(currentMusicTrack.getTitle()))
			{
				requirementSectionRenderer.addSpace(panelComponent);
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Next:")
					.right(nextSuggestedTrack.getTitle())
					.leftColor(Color.WHITE)
					.rightColor(ColorScheme.PROGRESS_ERROR_COLOR)
					.build());
			}
		}
		catch (Exception exception)
		{
			log.debug("Error getting next track", exception);
		}
	}
}