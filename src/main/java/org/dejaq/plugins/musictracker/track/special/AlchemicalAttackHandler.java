package org.dejaq.plugins.musictracker.track.special;

import java.util.List;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.ColorScheme;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.requirement.DynamicRequirement;
import org.dejaq.plugins.musictracker.requirement.ItemRequirement;
import org.dejaq.plugins.musictracker.track.Route;

// TODO Mostly need a quick teleport recommendation - for now we leave "unlocks when"
public class AlchemicalAttackHandler implements SpecialTrackHandler
{
	@Override
	public List<DynamicRequirement<ItemRequirement>> getDynamicItems(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		boolean isEliteKourendDiaryComplete = musicTrackerPlugin.getClient().getVarbitValue(VarbitID.KOUREND_DIARY_ELITE_COMPLETE) > 0;
		if (isEliteKourendDiaryComplete)
		{
			return List.of();
		}

		boolean hasBootsOfBrimstone = musicTrackerPlugin.playerHasItem(new ItemRequirement(ItemID.BOOTS_OF_BRIMSTONE, 1));
		boolean hasGraniteBoots = musicTrackerPlugin.playerHasItem(new ItemRequirement(ItemID.GRANITE_BOOTS, 1));
		boolean hasBootsOfStone = musicTrackerPlugin.playerHasItem(new ItemRequirement(ItemID.BOOTS_OF_STONE, 1));

		if (hasBootsOfBrimstone)
		{
			return List.of(DynamicRequirement.of(new ItemRequirement(ItemID.BOOTS_OF_BRIMSTONE, 1), "Boots of brimstone", ColorScheme.PROGRESS_COMPLETE_COLOR));
		}
		if (hasGraniteBoots)
		{
			return List.of(DynamicRequirement.of(new ItemRequirement(ItemID.GRANITE_BOOTS, 1), "Granite boots", ColorScheme.PROGRESS_COMPLETE_COLOR));
		}
		if (hasBootsOfStone)
		{
			return List.of(DynamicRequirement.of(new ItemRequirement(ItemID.BOOTS_OF_STONE, 1), "Boots of stone", ColorScheme.PROGRESS_COMPLETE_COLOR));
		}

		return List.of(
			DynamicRequirement.of(new ItemRequirement(ItemID.BOOTS_OF_STONE, 1), "Boots of stone or", ColorScheme.PROGRESS_ERROR_COLOR),
			DynamicRequirement.of(new ItemRequirement(ItemID.BOOTS_OF_BRIMSTONE, 1), "Boots of brimstone or", ColorScheme.PROGRESS_ERROR_COLOR),
			DynamicRequirement.of(new ItemRequirement(ItemID.GRANITE_BOOTS, 1), "Granite boots", ColorScheme.PROGRESS_ERROR_COLOR)
		);
	}
}