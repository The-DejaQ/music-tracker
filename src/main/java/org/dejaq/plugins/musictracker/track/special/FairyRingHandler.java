package org.dejaq.plugins.musictracker.track.special;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ItemManager;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.requirement.DynamicRequirement;
import org.dejaq.plugins.musictracker.requirement.ItemRequirement;
import org.dejaq.plugins.musictracker.requirement.collections.ItemCollections;
import org.dejaq.plugins.musictracker.track.Route;

public class FairyRingHandler implements SpecialTrackHandler
{
	private static final String FAIRY_RING_ACCESS_LABEL = "Fairy ring access";

	@Override
	public List<String> getHandledRouteNames()
	{
		return null;
	}

	@Override
	public List<SpecialEntity> getHandledEntities()
	{
		return List.of(SpecialEntity.FAIRY_RING);
	}

	@Override
	public List<DynamicRequirement<ItemRequirement>> getDynamicItems(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		if (isEliteLumbridgeDiaryComplete(musicTrackerPlugin))
		{
			return List.of();
		}

		List<ItemRequirement> baseItems = route != null ? route.getItems() : null;
		if (baseItems == null)
		{
			baseItems = List.of();
		}

		return addFairyStaffRequirement(musicTrackerPlugin.getItemManager(), baseItems);
	}

	private boolean isEliteLumbridgeDiaryComplete(MusicTrackerPlugin musicTrackerPlugin)
	{
		return musicTrackerPlugin.getClient().getVarbitValue(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE) > 0;
	}

	private List<DynamicRequirement<ItemRequirement>> addFairyStaffRequirement(ItemManager itemManager, List<ItemRequirement> baseItems)
	{
		List<DynamicRequirement<ItemRequirement>> result = new ArrayList<>();

		for (ItemRequirement item : baseItems)
		{
			result.add(DynamicRequirement.of(
				item,
				item.getDisplayText(itemManager),
				null
			));
		}

		ItemRequirement fairyStaff = new ItemRequirement(ItemCollections.FAIRY_STAFF, 1);
		fairyStaff.setLabel(FAIRY_RING_ACCESS_LABEL);

		result.add(DynamicRequirement.of(
			fairyStaff,
			FAIRY_RING_ACCESS_LABEL,
			null
		));

		return result;
	}
}