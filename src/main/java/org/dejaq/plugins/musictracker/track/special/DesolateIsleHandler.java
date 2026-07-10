package org.dejaq.plugins.musictracker.track.special;

import java.util.List;
import net.runelite.api.gameval.ItemID;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.quest.Quest;
import org.dejaq.plugins.musictracker.quest.QuestState;
import org.dejaq.plugins.musictracker.requirement.DynamicRequirement;
import org.dejaq.plugins.musictracker.requirement.ItemRequirement;
import org.dejaq.plugins.musictracker.track.Route;

public class DesolateIsleHandler implements SpecialTrackHandler
{
	private static final int TRAVEL_COINS_QUANTITY = 1000;

	@Override
	public List<DynamicRequirement<ItemRequirement>> getDynamicItems(
		MusicTrack musicTrack,
		Route route,
		MusicTrackerPlugin musicTrackerPlugin)
	{
		QuestState questState = musicTrackerPlugin.getPlayerState()
			.getCachedQuestState(Quest.THE_FREMENNIK_TRIALS);

		if (questState == QuestState.FINISHED)
		{
			return List.of();
		}

		return List.of(
			DynamicRequirement.of(
				new ItemRequirement(ItemID.COINS, TRAVEL_COINS_QUANTITY),
				"1,000 coins to travel",
				null
			)
		);
	}
}