package org.dejaq.plugins.musictracker.track.special;

import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.requirement.DynamicRequirement;
import org.dejaq.plugins.musictracker.requirement.ItemRequirement;
import org.dejaq.plugins.musictracker.requirement.LevelRequirement;
import org.dejaq.plugins.musictracker.track.MusicTrackEntityPoint;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.TrackStep;

public interface SpecialTrackHandler
{
	default String getHandledRouteName()
	{
		return null;
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

	default List<Route> getDynamicRoutes(MusicTrack musicTrack, MusicTrackerPlugin musicTrackerPlugin)
	{
		return List.of();
	}

	default void onChatMessage(MusicTrack musicTrack, Route route, ChatMessage chatMessageEvent, MusicTrackerPlugin musicTrackerPlugin)
	{
	}
}