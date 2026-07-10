package org.dejaq.plugins.musictracker.json;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.requirement.ItemRequirement;
import org.dejaq.plugins.musictracker.requirement.LevelRequirement;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.TrackStep;

public class RegionWriter
{
	public static MusicTrackJson toJson(MusicTrack musicTrack)
	{
		MusicTrackJson musicTrackJson = new MusicTrackJson();
		musicTrackJson.title = musicTrack.getTitle();
		musicTrackJson.region = musicTrack.getRegion();
		musicTrackJson.location = musicTrack.getLocation();
		musicTrackJson.members = musicTrack.isMembers();
		musicTrackJson.wikiUrl = musicTrack.getWikiUrl();
		musicTrackJson.unlockType = musicTrack.getUnlockType() != null ? musicTrack.getUnlockType().name() : null;
		musicTrackJson.quest = musicTrack.getUnlockQuest() != null ? musicTrack.getUnlockQuest().name() : null;
		musicTrackJson.unlockMessage = musicTrack.getUnlockMessage();
		musicTrackJson.unlockHint = musicTrack.getUnlockHint();

		List<RouteJson> routeJsonList = new ArrayList<>();
		for (Route route : musicTrack.getAllRoutes())
		{
			routeJsonList.add(toJson(route));
		}
		musicTrackJson.routes = routeJsonList;

		return musicTrackJson;
	}

	public static RouteJson toJson(Route route)
	{
		return toJson(route, null);
	}

	public static RouteJson toJson(Route route, String owningTrackTitle)
	{
		RouteJson routeJson = new RouteJson();
		routeJson.name = route.getName();
		routeJson.track = owningTrackTitle;
		routeJson.defaultRoute = route.isDefaultRoute();
		routeJson.quest = route.getQuest() != null ? route.getQuest().name() : null;
		routeJson.notes = route.getNotes();

		List<TrackStepJson> trackStepJsonList = new ArrayList<>();
		if (route.getTrackSteps() != null)
		{
			for (TrackStep trackStep : route.getTrackSteps())
			{
				trackStepJsonList.add(toJson(trackStep));
			}
		}
		routeJson.trackSteps = trackStepJsonList;

		routeJson.items = toItemRequirementJsonList(route.getItems());
		routeJson.recommendedItems = toItemRequirementJsonList(route.getRecommendedItems());
		routeJson.levels = toSkillRequirementJsonList(route.getLevels());
		routeJson.recommendedLevels = toSkillRequirementJsonList(route.getRecommendedLevels());

		return routeJson;
	}

	public static TrackStepJson toJson(TrackStep trackStep)
	{
		TrackStepJson trackStepJson = new TrackStepJson();
		trackStepJson.name = trackStep.getName();
		trackStepJson.destination = toJson(trackStep.getDestination());

		List<InteractionTargetJson> interactionTargetJsonList = new ArrayList<>();
		if (trackStep.getInteractions() != null)
		{
			for (InteractionTarget interactionTarget : trackStep.getInteractions())
			{
				interactionTargetJsonList.add(toJson(interactionTarget));
			}
		}
		trackStepJson.interactions = interactionTargetJsonList;

		return trackStepJson;
	}

	public static InteractionTargetJson toJson(InteractionTarget interactionTarget)
	{
		InteractionTargetJson interactionTargetJson = new InteractionTargetJson();
		interactionTargetJson.entityId = interactionTarget.getEntityId();
		interactionTargetJson.entity = interactionTarget.getEntity();
		interactionTargetJson.location = toJson(interactionTarget.getLocation());
		interactionTargetJson.type = interactionTarget.getType() != null ? interactionTarget.getType().name() : null;
		interactionTargetJson.hint = interactionTarget.getHint();
		interactionTargetJson.actions = interactionTarget.getActions() != null ? new ArrayList<>(interactionTarget.getActions()) : new ArrayList<>();
		interactionTargetJson.searchRadius = interactionTarget.hasCustomSearchRadius() ? interactionTarget.getSearchRadius() : null;
		return interactionTargetJson;
	}

	public static ItemRequirementJson toJson(ItemRequirement itemRequirement)
	{
		ItemRequirementJson itemRequirementJson = new ItemRequirementJson();
		itemRequirementJson.itemId = itemRequirement.getItemId();
		itemRequirementJson.item = itemRequirement.getItem();
		itemRequirementJson.itemCollection = itemRequirement.getItemCollection() != null ? itemRequirement.getItemCollection().name() : null;
		itemRequirementJson.quantity = itemRequirement.getQuantity();
		itemRequirementJson.label = itemRequirement.getLabel();
		return itemRequirementJson;
	}

	public static SkillRequirementJson toJson(LevelRequirement levelRequirement)
	{
		SkillRequirementJson skillRequirementJson = new SkillRequirementJson();
		skillRequirementJson.skill = levelRequirement.getSkill() != null ? levelRequirement.getSkill().name() : null;
		skillRequirementJson.level = levelRequirement.getLevel();
		return skillRequirementJson;
	}

	public static WorldPointJson toJson(WorldPoint worldPoint)
	{
		if (worldPoint == null)
		{
			return null;
		}
		WorldPointJson worldPointJson = new WorldPointJson();
		worldPointJson.x = worldPoint.getX();
		worldPointJson.y = worldPoint.getY();
		worldPointJson.z = worldPoint.getPlane();
		return worldPointJson;
	}

	private static List<ItemRequirementJson> toItemRequirementJsonList(List<ItemRequirement> itemRequirements)
	{
		List<ItemRequirementJson> itemRequirementJsonList = new ArrayList<>();
		if (itemRequirements != null)
		{
			for (ItemRequirement itemRequirement : itemRequirements)
			{
				itemRequirementJsonList.add(toJson(itemRequirement));
			}
		}
		return itemRequirementJsonList;
	}

	private static List<SkillRequirementJson> toSkillRequirementJsonList(List<LevelRequirement> levelRequirements)
	{
		List<SkillRequirementJson> skillRequirementJsonList = new ArrayList<>();
		if (levelRequirements != null)
		{
			for (LevelRequirement levelRequirement : levelRequirements)
			{
				skillRequirementJsonList.add(toJson(levelRequirement));
			}
		}
		return skillRequirementJsonList;
	}
}