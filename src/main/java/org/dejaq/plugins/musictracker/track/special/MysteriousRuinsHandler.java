package org.dejaq.plugins.musictracker.track.special;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.requirement.ItemRequirement;
import org.dejaq.plugins.musictracker.requirement.collections.ItemCollections;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.InteractionType;
import org.dejaq.plugins.musictracker.track.MusicTrackEntityPoint;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.TrackStep;
import org.dejaq.plugins.musictracker.worldmap.RunecraftingAltarLocation;

public class MysteriousRuinsHandler implements SpecialTrackHandler
{
	private static final List<String> ROUTE_NAMES = List.of(
		"Mysterious Ruins"
	);

	public static final String AIR_ALTAR_TRACK = "Serene";
	public static final String BODY_ALTAR_TRACK = "Heart and Mind";
	public static final String CHAOS_ALTAR_TRACK = "Complication";
	public static final String COSMIC_ALTAR_TRACK = "Stratosphere";
	public static final String DEATH_ALTAR_TRACK = "La Mort";
	public static final String EARTH_ALTAR_TRACK = "Down to Earth";
	public static final String FIRE_ALTAR_TRACK = "Quest";
	public static final String LAW_ALTAR_TRACK = "Righteousness";
	public static final String MIND_ALTAR_TRACK = "Miracle Dance";
	public static final String NATURE_ALTAR_TRACK = "Understanding";
	public static final String SOUL_ALTAR_TRACK = "Soul Fall";
	public static final String WATER_ALTAR_TRACK = "Zealot";

	private static final String MYSTERIOUS_RUINS_ENTITY_NAME = "Mysterious ruins";
	private static final String MYSTERIOUS_RUINS_STEP_NAME = "Ruins";

	private static final String HINT_ENTER = "Enter";
	private static final String HINT_WEAR_THEN_ENTER = "Wear tiara then enter";
	private static final String HINT_USE_TALISMAN = "Use talisman on ruins";
	private static final String HINT_DEFAULT_ENTRY = "Enter / Use talisman on";

	@Getter
	@RequiredArgsConstructor
	private static final class AltarAccess
	{
		private final String trackTitle;
		private final ItemCollections talismanItemCollection;
		private final ItemCollections wearableItemCollection;
		private final RunecraftingAltarLocation runecraftingAltarLocation;
	}

	private static final Map<String, AltarAccess> ALTAR_ACCESS_BY_TRACK_TITLE = new HashMap<>();

	private static void registerAltarAccess(String trackTitle, ItemCollections talismanItemCollection, ItemCollections wearableItemCollection, RunecraftingAltarLocation runecraftingAltarLocation)
	{
		ALTAR_ACCESS_BY_TRACK_TITLE.put(trackTitle.toLowerCase(Locale.ROOT), new AltarAccess(trackTitle, talismanItemCollection, wearableItemCollection, runecraftingAltarLocation));
	}

	static
	{
		registerAltarAccess(AIR_ALTAR_TRACK, ItemCollections.AIR_ALTAR, ItemCollections.AIR_ALTAR_WEARABLE, RunecraftingAltarLocation.AIR_ALTAR);
		registerAltarAccess(BODY_ALTAR_TRACK, ItemCollections.BODY_ALTAR, ItemCollections.BODY_ALTAR_WEARABLE, RunecraftingAltarLocation.BODY_ALTAR);
		registerAltarAccess(CHAOS_ALTAR_TRACK, ItemCollections.CHAOS_ALTAR, ItemCollections.CHAOS_ALTAR_WEARABLE, RunecraftingAltarLocation.CHAOS_ALTAR);
		registerAltarAccess(EARTH_ALTAR_TRACK, ItemCollections.EARTH_ALTAR, ItemCollections.EARTH_ALTAR_WEARABLE, RunecraftingAltarLocation.EARTH_ALTAR);
		registerAltarAccess(FIRE_ALTAR_TRACK, ItemCollections.FIRE_ALTAR, ItemCollections.FIRE_ALTAR_WEARABLE, RunecraftingAltarLocation.FIRE_ALTAR);
		registerAltarAccess(MIND_ALTAR_TRACK, ItemCollections.MIND_ALTAR, ItemCollections.MIND_ALTAR_WEARABLE, RunecraftingAltarLocation.MIND_ALTAR);
		registerAltarAccess(NATURE_ALTAR_TRACK, ItemCollections.NATURE_ALTAR, ItemCollections.NATURE_ALTAR_WEARABLE, RunecraftingAltarLocation.NATURE_ALTAR);
		registerAltarAccess(WATER_ALTAR_TRACK, ItemCollections.WATER_ALTAR, ItemCollections.WATER_ALTAR_WEARABLE, RunecraftingAltarLocation.WATER_ALTAR);
	}

	@Override
	public List<TrackStep> contributeTrackSteps(MusicTrack currentMusicTrack, Route currentRoute, List<TrackStep> existingTrackSteps)
	{
		AltarAccess altarAccess = resolveAltarAccess(currentMusicTrack);
		if (altarAccess == null || currentRoute == null || !ROUTE_NAMES.contains(currentRoute.getName()))
		{
			return existingTrackSteps;
		}

		if (routeAlreadyEndsAtRuins(existingTrackSteps))
		{
			return existingTrackSteps;
		}

		List<TrackStep> contributedTrackSteps = new ArrayList<>();
		if (existingTrackSteps != null)
		{
			contributedTrackSteps.addAll(existingTrackSteps);
		}
		contributedTrackSteps.add(buildRuinsTrackStep(altarAccess));
		return contributedTrackSteps;
	}

	private boolean routeAlreadyEndsAtRuins(List<TrackStep> existingTrackSteps)
	{
		if (existingTrackSteps == null || existingTrackSteps.isEmpty())
		{
			return false;
		}

		TrackStep lastTrackStep = existingTrackSteps.get(existingTrackSteps.size() - 1);
		return lastTrackStep != null && findRuinsInteraction(lastTrackStep) != null;
	}

	private TrackStep buildRuinsTrackStep(AltarAccess altarAccess)
	{
		WorldPoint canonicalRuinsLocation = altarAccess.getRunecraftingAltarLocation().getLocation();

		InteractionTarget ruinsInteractionTarget = InteractionTarget.builder()
			.entityId(-1)
			.entity(MYSTERIOUS_RUINS_ENTITY_NAME)
			.location(canonicalRuinsLocation)
			.type(InteractionType.GAME_OBJECT)
			.hint(HINT_DEFAULT_ENTRY)
			.build();

		return TrackStep.builder()
			.name(MYSTERIOUS_RUINS_STEP_NAME)
			.destination(canonicalRuinsLocation)
			.interaction(ruinsInteractionTarget)
			.build();
	}

	@Override
	public List<String> getHandledRouteNames()
	{
		return ROUTE_NAMES;
	}

	@Override
	public boolean hasVolatileDynamicHighlights(MusicTrack currentMusicTrack, Route currentRoute, MusicTrackerPlugin musicTrackerPlugin)
	{
		return true;
	}

	@Override
	public List<MusicTrackEntityPoint> getDynamicEntityHighlights(MusicTrack currentMusicTrack, Route currentRoute, TrackStep currentTrackStep, int currentStageIndex, MusicTrackerPlugin musicTrackerPlugin)
	{
		if (currentTrackStep == null)
		{
			return null;
		}

		AltarAccess altarAccess = resolveAltarAccess(currentMusicTrack);
		if (altarAccess == null)
		{
			return null;
		}

		InteractionTarget ruinsInteraction = findRuinsInteraction(currentTrackStep);
		if (ruinsInteraction == null)
		{
			return null;
		}

		String hintText = resolveHintText(altarAccess, musicTrackerPlugin);
		if (hintText == null)
		{
			return null;
		}

		WorldPoint canonicalRuinsLocation = altarAccess.getRunecraftingAltarLocation().getLocation();

		InteractionTarget dynamicRuinsInteraction = InteractionTarget.builder()
			.entityId(ruinsInteraction.getEntityId())
			.entity(ruinsInteraction.getEntity())
			.location(canonicalRuinsLocation)
			.type(ruinsInteraction.getType())
			.hint(hintText)
			.build();

		return List.of(MusicTrackEntityPoint.from(canonicalRuinsLocation, dynamicRuinsInteraction));
	}

	private InteractionTarget findRuinsInteraction(TrackStep currentTrackStep)
	{
		for (InteractionTarget interactionTarget : currentTrackStep.getAllHighlights())
		{
			if (interactionTarget != null && MYSTERIOUS_RUINS_ENTITY_NAME.equalsIgnoreCase(interactionTarget.getEntity()))
			{
				return interactionTarget;
			}
		}
		return null;
	}

	private static AltarAccess resolveAltarAccess(MusicTrack musicTrack)
	{
		if (musicTrack == null || musicTrack.getTitle() == null)
		{
			return null;
		}
		return ALTAR_ACCESS_BY_TRACK_TITLE.get(musicTrack.getTitle().toLowerCase(Locale.ROOT));
	}

	private String resolveHintText(AltarAccess altarAccess, MusicTrackerPlugin musicTrackerPlugin)
	{
		ItemRequirement wearableRequirement = new ItemRequirement(altarAccess.getWearableItemCollection(), 1);
		if (musicTrackerPlugin.playerHasItemEquipped(wearableRequirement))
		{
			return HINT_ENTER;
		}

		if (musicTrackerPlugin.playerHasItem(wearableRequirement))
		{
			return HINT_WEAR_THEN_ENTER;
		}

		ItemRequirement talismanRequirement = new ItemRequirement(altarAccess.getTalismanItemCollection(), 1);
		if (musicTrackerPlugin.playerHasItem(talismanRequirement))
		{
			return HINT_USE_TALISMAN;
		}

		return null;
	}
}