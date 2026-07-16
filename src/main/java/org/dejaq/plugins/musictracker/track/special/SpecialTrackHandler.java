package org.dejaq.plugins.musictracker.track.special;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.requirement.DynamicRequirement;
import org.dejaq.plugins.musictracker.requirement.ItemRequirement;
import org.dejaq.plugins.musictracker.requirement.LevelRequirement;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.MusicTrackEntityPoint;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.TrackStep;

public interface SpecialTrackHandler
{
	List<String> getHandledRouteNames();

	default List<SpecialEntity> getHandledEntities()
	{
		return List.of();
	}

	default List<TrackStep> contributeTrackSteps(MusicTrack musicTrack, Route route, List<TrackStep> existingTrackSteps)
	{
		return existingTrackSteps;
	}

	default DynamicRequirement<String> getDynamicQuest(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		return null;
	}

	default List<DynamicRequirement<ItemRequirement>> getDynamicItems(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		return List.of();
	}

	default List<DynamicRequirement<ItemRequirement>> getDynamicItemRecommendations(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		return List.of();
	}

	default List<DynamicRequirement<LevelRequirement>> getDynamicLevels(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		return List.of();
	}

	default List<DynamicRequirement<LevelRequirement>> getDynamicLevelRecommendations(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		return List.of();
	}

	default DynamicRequirement<String> getDynamicNotes(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		return null;
	}

	default boolean canProgress(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		return true;
	}

	default Integer getForcedStageIndex(MusicTrack musicTrack, Route route, int currentStageIndex, WorldPoint currentPlayerLocation, MusicTrackerPlugin musicTrackerPlugin)
	{
		return null;
	}

	default List<MusicTrackEntityPoint> getDynamicEntityHighlights(MusicTrack musicTrack, Route route, TrackStep trackStep, int stageIndex, MusicTrackerPlugin musicTrackerPlugin)
	{
		return null;
	}

	default boolean hasVolatileDynamicHighlights(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		return false;
	}

	default void onChatMessage(MusicTrack musicTrack, Route route, ChatMessage chatMessageEvent, MusicTrackerPlugin musicTrackerPlugin)
	{
	}

	default void reset()
	{
	}

	default List<DynamicRequirement<ItemRequirement>> addDynamicItems(
		Route route, MusicTrackerPlugin musicTrackerPlugin, List<DynamicRequirement<ItemRequirement>> additionalItems)
	{
		List<ItemRequirement> staticItems = route != null ? route.getItems() : null;
		return mergeStaticWithDynamicAdditions(staticItems, item -> item.getDisplayText(musicTrackerPlugin.getItemManager()), additionalItems);
	}

	default List<DynamicRequirement<ItemRequirement>> addDynamicItems(
		Route route, MusicTrackerPlugin musicTrackerPlugin, DynamicRequirement<ItemRequirement> additionalItem)
	{
		return addDynamicItems(route, musicTrackerPlugin, List.of(additionalItem));
	}

	default List<DynamicRequirement<ItemRequirement>> addDynamicItemRecommendations(
		Route route, MusicTrackerPlugin musicTrackerPlugin, List<DynamicRequirement<ItemRequirement>> additionalItemRecommendations)
	{
		List<ItemRequirement> staticItemRecommendations = route != null ? route.getRecommendedItems() : null;
		return mergeStaticWithDynamicAdditions(staticItemRecommendations, item -> item.getDisplayText(musicTrackerPlugin.getItemManager()), additionalItemRecommendations);
	}

	default List<DynamicRequirement<ItemRequirement>> addDynamicItemRecommendations(
		Route route, MusicTrackerPlugin musicTrackerPlugin, DynamicRequirement<ItemRequirement> additionalItemRecommendation)
	{
		return addDynamicItemRecommendations(route, musicTrackerPlugin, List.of(additionalItemRecommendation));
	}

	default List<DynamicRequirement<LevelRequirement>> addDynamicLevels(
		Route route, List<DynamicRequirement<LevelRequirement>> additionalLevels)
	{
		List<LevelRequirement> staticLevels = route != null ? route.getLevels() : null;
		return mergeStaticWithDynamicAdditions(staticLevels, SpecialTrackHandler::formatLevelRequirement, additionalLevels);
	}

	default List<DynamicRequirement<LevelRequirement>> addDynamicLevels(
		Route route, DynamicRequirement<LevelRequirement> additionalLevel)
	{
		return addDynamicLevels(route, List.of(additionalLevel));
	}

	default List<DynamicRequirement<LevelRequirement>> addDynamicLevelRecommendations(
		Route route, List<DynamicRequirement<LevelRequirement>> additionalLevelRecommendations)
	{
		List<LevelRequirement> staticLevelRecommendations = route != null ? route.getRecommendedLevels() : null;
		return mergeStaticWithDynamicAdditions(staticLevelRecommendations, SpecialTrackHandler::formatLevelRequirement, additionalLevelRecommendations);
	}

	default List<DynamicRequirement<LevelRequirement>> addDynamicLevelRecommendations(
		Route route, DynamicRequirement<LevelRequirement> additionalLevelRecommendation)
	{
		return addDynamicLevelRecommendations(route, List.of(additionalLevelRecommendation));
	}

	default List<MusicTrackEntityPoint> addDynamicEntityHighlights(TrackStep trackStep, List<MusicTrackEntityPoint> additionalHighlights)
	{
		List<MusicTrackEntityPoint> combinedHighlights = new ArrayList<>();
		if (trackStep != null && trackStep.getAllHighlights() != null)
		{
			for (InteractionTarget interactionTarget : trackStep.getAllHighlights())
			{
				MusicTrackEntityPoint staticHighlight = toEntityHighlight(trackStep, interactionTarget);
				if (staticHighlight != null)
				{
					combinedHighlights.add(staticHighlight);
				}
			}
		}
		if (additionalHighlights != null)
		{
			combinedHighlights.addAll(additionalHighlights);
		}
		return combinedHighlights;
	}

	default List<MusicTrackEntityPoint> addDynamicEntityHighlights(TrackStep trackStep, MusicTrackEntityPoint additionalHighlights)
	{
		return addDynamicEntityHighlights(trackStep, List.of(additionalHighlights));
	}

	private static MusicTrackEntityPoint toEntityHighlight(TrackStep trackStep, InteractionTarget interactionTarget)
	{
		if (interactionTarget == null)
		{
			return null;
		}
		WorldPoint highlightLocation = interactionTarget.getLocation() != null
			? interactionTarget.getLocation()
			: trackStep.getDestination();
		if (highlightLocation == null)
		{
			return null;
		}
		return MusicTrackEntityPoint.from(highlightLocation, interactionTarget);
	}

	private static <T> List<DynamicRequirement<T>> mergeStaticWithDynamicAdditions(
		List<T> staticRequirements, Function<T, String> displayTextResolver, List<DynamicRequirement<T>> additions)
	{
		List<DynamicRequirement<T>> combined = new ArrayList<>();
		if (staticRequirements != null)
		{
			for (T staticRequirement : staticRequirements)
			{
				combined.add(DynamicRequirement.of(staticRequirement, displayTextResolver.apply(staticRequirement), null));
			}
		}
		if (additions != null)
		{
			combined.addAll(additions);
		}
		return combined;
	}

	private static String formatLevelRequirement(LevelRequirement levelRequirement)
	{
		return levelRequirement.getLevel() + " " + levelRequirement.getSkill().getName();
	}
}