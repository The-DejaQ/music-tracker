package org.dejaq.plugins.musictracker.track.special;

import java.util.List;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.track.Route;

public class MawsJawsClawsHandler implements SpecialTrackHandler
{
	private static final int CAVE_ENTRANCE_MAX_X = 2880;
	private static final int CAVE_ENTRANCE_MIN_Y = 9810;
	private static final int FAST_FORWARDED_STAGE_INDEX = 2;

	private static final List<String> ROUTE_NAMES = List.of(
		"Route 1"
	);

	@Override
	public List<String> getHandledRouteNames()
	{
		return ROUTE_NAMES;
	}

	@Override
	public Integer getForcedStageIndex(MusicTrack musicTrack, Route route, int currentStageIndex, WorldPoint currentPlayerLocation, MusicTrackerPlugin musicTrackerPlugin)
	{
		boolean playerIsInKeldagrimMineCore = currentPlayerLocation.getX() < CAVE_ENTRANCE_MAX_X
			&& currentPlayerLocation.getY() > CAVE_ENTRANCE_MIN_Y;

		if (playerIsInKeldagrimMineCore && currentStageIndex < FAST_FORWARDED_STAGE_INDEX)
		{
			return FAST_FORWARDED_STAGE_INDEX;
		}
		return null;
	}
}