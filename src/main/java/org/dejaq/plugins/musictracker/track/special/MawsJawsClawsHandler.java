package org.dejaq.plugins.musictracker.track.special;

import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.track.Route;

public class MawsJawsClawsHandler implements SpecialTrackHandler
{
	private static final int CAVE_ENTRANCE_MAX_X = 2880;
	private static final int CAVE_ENTRANCE_MIN_Y = 9810;
	private static final int FAST_FORWARDED_STAGE_INDEX = 2;

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