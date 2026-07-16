package org.dejaq.plugins.musictracker.json;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.track.Region;
import org.dejaq.plugins.musictracker.track.UnlockType;
import org.dejaq.plugins.musictracker.quest.Quest;
import org.dejaq.plugins.musictracker.requirement.ItemRequirement;
import org.dejaq.plugins.musictracker.requirement.LevelRequirement;
import org.dejaq.plugins.musictracker.requirement.collections.ItemCollections;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.InteractionType;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.TrackStep;

@Slf4j
public class RegionLoader
{
	private static final String REGIONS_RESOURCE_DIRECTORY = "/org/dejaq/plugins/musictracker/regions";

	public static String[] getKnownRegionNames()
	{
		String[] knownRegionDisplayNames = new String[Region.values().length];
		Region[] regions = Region.values();
		for (int regionIndex = 0; regionIndex < regions.length; regionIndex++)
		{
			knownRegionDisplayNames[regionIndex] = regions[regionIndex].getDisplayName();
		}
		return knownRegionDisplayNames;
	}

	public static List<MusicTrack> loadRegion(Gson gson, String regionName)
	{
		String regionResourcePath = REGIONS_RESOURCE_DIRECTORY + "/" + sanitizeFileName(regionName) + ".json";

		try (InputStream regionResourceStream = RegionLoader.class.getResourceAsStream(regionResourcePath))
		{
			if (regionResourceStream == null)
			{
				log.warn("Region resource not found on classpath: {}", regionResourcePath);
				return Collections.emptyList();
			}

			try (Reader regionResourceReader = new InputStreamReader(regionResourceStream, StandardCharsets.UTF_8))
			{
				Type musicTrackJsonListType = new TypeToken<List<MusicTrackJson>>()
				{
				}.getType();
				List<MusicTrackJson> musicTrackJsonList = gson.fromJson(regionResourceReader, musicTrackJsonListType);

				if (musicTrackJsonList == null)
				{
					return Collections.emptyList();
				}

				List<MusicTrack> convertedMusicTracks = new ArrayList<>();
				for (MusicTrackJson musicTrackJson : musicTrackJsonList)
				{
					convertedMusicTracks.add(convertToMusicTrack(musicTrackJson));
				}
				return convertedMusicTracks;
			}
		}
		catch (IOException regionLoadException)
		{
			log.warn("Failed to load region: {}", regionName, regionLoadException);
			return Collections.emptyList();
		}
		catch (Exception regionParseException)
		{
			log.warn("Failed to parse region: {}", regionName, regionParseException);
			return Collections.emptyList();
		}
	}

	static MusicTrack convertToMusicTrack(MusicTrackJson musicTrackJson)
	{
		MusicTrack.MusicTrackBuilder musicTrackBuilder = MusicTrack.builder()
			.title(musicTrackJson.title)
			.region(musicTrackJson.region)
			.location(musicTrackJson.location)
			.members(musicTrackJson.members != null && musicTrackJson.members)
			.wikiUrl(musicTrackJson.wikiUrl)
			.unlockMessage(musicTrackJson.unlockMessage)
			.unlockHint(musicTrackJson.unlockHint);

		UnlockType resolvedUnlockType = UnlockType.NORMAL;
		if (musicTrackJson.unlockType != null)
		{
			try
			{
				resolvedUnlockType = UnlockType.valueOf(musicTrackJson.unlockType);
			}
			catch (IllegalArgumentException invalidUnlockTypeException)
			{
				log.warn("Unknown unlockType '{}' for track '{}', defaulting to NORMAL", musicTrackJson.unlockType, musicTrackJson.title);
			}
		}
		musicTrackBuilder.unlockType(resolvedUnlockType);

		if (musicTrackJson.quest != null && !musicTrackJson.quest.isBlank())
		{
			try
			{
				musicTrackBuilder.unlockQuest(Quest.valueOf(musicTrackJson.quest));
			}
			catch (IllegalArgumentException invalidQuestException)
			{
				log.warn("convertToMusicTrack: Unknown quest '{}' for track '{}'", musicTrackJson.quest, musicTrackJson.title);
			}
		}

		List<Route> convertedRoutes = new ArrayList<>();

		if (musicTrackJson.routes != null && !musicTrackJson.routes.isEmpty())
		{
			int routeIndex = 0;
			for (RouteJson routeJson : musicTrackJson.routes)
			{
				convertedRoutes.add(convertRoute(routeJson, routeIndex, musicTrackJson.title));
				routeIndex++;
			}
		}

		if (!convertedRoutes.isEmpty())
		{
			musicTrackBuilder.routes(convertedRoutes);
		}
		return musicTrackBuilder.build();
	}

	static Route convertRoute(RouteJson routeJson, int routeIndex, String trackTitleForLogging)
	{
		List<TrackStep> convertedTrackSteps = new ArrayList<>();
		if (routeJson.trackSteps != null)
		{
			for (TrackStepJson trackStepJson : routeJson.trackSteps)
			{
				convertedTrackSteps.add(convertTrackStep(trackStepJson));
			}
		}

		Route.RouteBuilder routeBuilder = Route.builder()
			.name(routeJson.name != null ? routeJson.name : "Route " + (routeIndex + 1))
			.defaultRoute(routeJson.defaultRoute)
			.trackSteps(convertedTrackSteps);

		if (routeJson.items != null)
		{
			routeJson.items.forEach(itemRequirementJson -> routeBuilder.item(convertItemRequirement(itemRequirementJson)));
		}
		if (routeJson.recommendedItems != null)
		{
			routeJson.recommendedItems.forEach(itemRequirementJson -> routeBuilder.recommendedItem(convertItemRequirement(itemRequirementJson)));
		}

		if (routeJson.levels != null)
		{
			for (SkillRequirementJson skillRequirementJson : routeJson.levels)
			{
				convertSkillRequirement(skillRequirementJson).ifPresent(routeBuilder::level);
			}
		}
		if (routeJson.recommendedLevels != null)
		{
			for (SkillRequirementJson skillRequirementJson : routeJson.recommendedLevels)
			{
				convertSkillRequirement(skillRequirementJson).ifPresent(routeBuilder::recommendedLevel);
			}
		}

		if (routeJson.quest != null && !routeJson.quest.isBlank())
		{
			try
			{
				routeBuilder.quest(Quest.valueOf(routeJson.quest));
			}
			catch (IllegalArgumentException invalidQuestException)
			{
				log.warn("convertRoute: Unknown quest '{}' for track '{}'", routeJson.quest, trackTitleForLogging);
			}
		}
		if (routeJson.notes != null && !routeJson.notes.isBlank())
		{
			routeBuilder.notes(routeJson.notes);
		}

		return routeBuilder.build();
	}

	static Optional<LevelRequirement> convertSkillRequirement(SkillRequirementJson skillRequirementJson)
	{
		if (skillRequirementJson.skill == null || skillRequirementJson.skill.isBlank())
		{
			return Optional.empty();
		}
		try
		{
			Skill skill = Skill.valueOf(skillRequirementJson.skill.trim().toUpperCase());
			return Optional.of(LevelRequirement.of(skill, skillRequirementJson.level));
		}
		catch (IllegalArgumentException invalidSkillException)
		{
			log.warn("Unknown skill '{}'", skillRequirementJson.skill);
			return Optional.empty();
		}
	}

	static ItemRequirement convertItemRequirement(ItemRequirementJson itemRequirementJson)
	{
		ItemRequirement convertedItemRequirement;

		if (itemRequirementJson.itemCollection != null && !itemRequirementJson.itemCollection.isBlank())
		{
			try
			{
				ItemCollections itemCollectionGroup = ItemCollections.valueOf(itemRequirementJson.itemCollection.toUpperCase());
				convertedItemRequirement = new ItemRequirement(itemCollectionGroup, itemRequirementJson.quantity);
			}
			catch (IllegalArgumentException invalidItemCollectionException)
			{
				log.warn("Unknown itemCollection '{}'", itemRequirementJson.itemCollection);
				convertedItemRequirement = new ItemRequirement(-1, itemRequirementJson.quantity);
			}
		}
		else if (itemRequirementJson.itemId > 0)
		{
			convertedItemRequirement = new ItemRequirement(itemRequirementJson.itemId, itemRequirementJson.quantity);
		}
		else
		{
			convertedItemRequirement = new ItemRequirement(-1, 1);
		}

		if (itemRequirementJson.label != null && !itemRequirementJson.label.isBlank())
		{
			convertedItemRequirement.setLabel(itemRequirementJson.label);
		}

		convertedItemRequirement.setEquipped(itemRequirementJson.equipped);

		return convertedItemRequirement;
	}

	static TrackStep convertTrackStep(TrackStepJson trackStepJson)
	{
		TrackStep.TrackStepBuilder trackStepBuilder = TrackStep.builder();

		trackStepBuilder.name(trackStepJson.name);

		if (trackStepJson.destination != null)
		{
			trackStepBuilder.destination(new WorldPoint(
				trackStepJson.destination.x,
				trackStepJson.destination.y,
				trackStepJson.destination.z
			));
		}

		if (trackStepJson.interactions != null && !trackStepJson.interactions.isEmpty())
		{
			for (InteractionTargetJson interactionTargetJson : trackStepJson.interactions)
			{
				trackStepBuilder.interaction(convertInteractionTarget(interactionTargetJson));
			}
		}
		else if (trackStepJson.interaction != null)
		{
			trackStepBuilder.interaction(convertInteractionTarget(trackStepJson.interaction));
		}

		return trackStepBuilder.build();
	}

	static InteractionTarget convertInteractionTarget(InteractionTargetJson interactionTargetJson)
	{
		WorldPoint interactionLocation = null;
		if (interactionTargetJson.location != null)
		{
			interactionLocation = new WorldPoint(interactionTargetJson.location.x, interactionTargetJson.location.y, interactionTargetJson.location.z);
		}

		InteractionType interactionType = InteractionType.GAME_OBJECT;
		if (interactionTargetJson.type != null)
		{
			try
			{
				interactionType = InteractionType.valueOf(interactionTargetJson.type);
			}
			catch (IllegalArgumentException invalidInteractionTypeException)
			{
				log.warn("Unknown interaction type '{}'", interactionTargetJson.type);
			}
		}

		InteractionTarget.InteractionTargetBuilder interactionTargetBuilder = InteractionTarget.builder()
			.hint(interactionTargetJson.hint)
			.location(interactionLocation)
			.type(interactionType);

		if (interactionTargetJson.entityId > 0)
		{
			interactionTargetBuilder.entityId(interactionTargetJson.entityId);
		}
		else if (interactionTargetJson.entity != null)
		{
			interactionTargetBuilder.entityId(-1).entity(interactionTargetJson.entity);
		}
		else
		{
			interactionTargetBuilder.entityId(-1);
		}

		if (interactionTargetJson.actions != null)
		{
			interactionTargetBuilder.actions(interactionTargetJson.actions);
		}

		interactionTargetBuilder.searchRadius(interactionTargetJson.searchRadius != null ? interactionTargetJson.searchRadius : -1);

		if (interactionTargetJson.data != null)
		{
			interactionTargetBuilder.data(interactionTargetJson.data);
		}

		return interactionTargetBuilder.build();
	}

	private static String sanitizeFileName(String regionName)
	{
		return regionName.replaceAll("[^a-zA-Z0-9]", "");
	}

	public static Map<String, List<MusicTrack>> loadAllRegionsFromJson(Gson gson)
	{
		Map<String, List<MusicTrack>> regionNameToTracks = new LinkedHashMap<>();

		for (Region region : Region.values())
		{
			List<MusicTrack> tracksForRegion = loadRegion(gson, region.getResourceFileBaseName());
			if (!tracksForRegion.isEmpty())
			{
				regionNameToTracks.put(region.getDisplayName(), tracksForRegion);
			}
		}
		return regionNameToTracks;
	}
}