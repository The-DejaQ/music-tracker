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

public class MorUlRekHandler implements SpecialTrackHandler
{
	@Override
	public List<DynamicRequirement<ItemRequirement>> getDynamicItems(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		boolean hasSacrificedFireCape = musicTrackerPlugin.getClient().getVarbitValue(VarbitID.INFERNO_SACRIFICED_FIRECAPE) > 0;

		if (hasSacrificedFireCape)
		{
			return List.of(
				DynamicRequirement.of(null, "Fire cape (sacrificed)", ColorScheme.PROGRESS_COMPLETE_COLOR)
			);
		}

		return List.of(
			DynamicRequirement.of(
				new ItemRequirement(ItemID.TZHAAR_CAPE_FIRE, 1),
				"Fire cape (will be sacrificed)",
				null
			)
		);
	}
}