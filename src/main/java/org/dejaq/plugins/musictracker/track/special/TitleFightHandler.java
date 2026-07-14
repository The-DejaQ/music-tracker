package org.dejaq.plugins.musictracker.track.special;

import java.util.List;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.ui.ColorScheme;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.requirement.DynamicRequirement;
import org.dejaq.plugins.musictracker.track.Route;

public class TitleFightHandler implements SpecialTrackHandler
{
	private static final List<String> ROUTE_NAMES = List.of(
		"Route 1"
	);

	private static final int REQUIRED_QUEST_POINTS = 32;

	@Override
	public List<String> getHandledRouteNames()
	{
		return ROUTE_NAMES;
	}

	@Override
	public DynamicRequirement<String> getDynamicQuest(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		int currentQuestPoints = musicTrackerPlugin.getClient().getVarpValue(VarPlayerID.QP);
		return DynamicRequirement.of(
			null,
			REQUIRED_QUEST_POINTS + " Quest Points",
			currentQuestPoints >= REQUIRED_QUEST_POINTS ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.PROGRESS_ERROR_COLOR
		);
	}
}