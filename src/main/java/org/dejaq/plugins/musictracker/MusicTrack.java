package org.dejaq.plugins.musictracker;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.quest.Quest;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.TrackStep;
import org.dejaq.plugins.musictracker.track.UnlockType;

@Data
@Builder
public class MusicTrack
{
	private String title;
	private String region;
	private String location;
	private String wikiUrl;
	private boolean members;
	private UnlockType unlockType;

	private Quest unlockQuest;

	private String unlockMessage;
	private String unlockHint;

	@Singular("route")
	private List<Route> routes;

	private boolean custom;

	public List<Route> getAllRoutes()
	{
		return routes != null ? routes : List.of();
	}

	public Route getDefaultRoute()
	{
		if (routes == null || routes.isEmpty())
		{
			return null;
		}

		return routes.stream()
			.filter(Route::isDefaultRoute)
			.findFirst()
			.orElse(routes.get(0));
	}

	public List<TrackStep> getDefaultSteps()
	{
		Route defaultRoute = getDefaultRoute();
		return defaultRoute != null ? defaultRoute.getTrackSteps() : List.of();
	}

	public boolean hasMultipleRoutes()
	{
		return getAllRoutes().size() > 1;
	}

	public boolean hasSteps()
	{
		return !getDefaultSteps().isEmpty();
	}

	public int getTrackStepCount()
	{
		return getDefaultSteps().size();
	}

	public TrackStep getTrackStep(int stageIndex)
	{
		List<TrackStep> defaultSteps = getDefaultSteps();
		if (stageIndex < 0 || stageIndex >= defaultSteps.size())
		{
			return null;
		}
		return defaultSteps.get(stageIndex);
	}

	public WorldPoint getDestinationForStage(int stageIndex)
	{
		List<TrackStep> defaultSteps = getDefaultSteps();
		if (defaultSteps.size() == 1)
		{
			return getFinalDestination();
		}
		TrackStep trackStep = getTrackStep(stageIndex);
		return trackStep != null ? trackStep.getDestination() : null;
	}

	public WorldPoint getFinalDestination()
	{
		List<TrackStep> defaultSteps = getDefaultSteps();
		if (!defaultSteps.isEmpty())
		{
			TrackStep lastTrackStep = defaultSteps.get(defaultSteps.size() - 1);
			return lastTrackStep != null ? lastTrackStep.getDestination() : null;
		}
		return null;
	}

	public WorldPoint getUnlockPoint()
	{
		return getFinalDestination();
	}

	public WorldPoint getFirstStepDestination()
	{
		List<TrackStep> defaultSteps = getDefaultSteps();
		if (defaultSteps.isEmpty())
		{
			return null;
		}
		TrackStep firstTrackStep = defaultSteps.get(0);
		return firstTrackStep != null ? firstTrackStep.getDestination() : null;
	}

	public Quest getEffectiveUnlockQuest(Route route)
	{
		if (unlockQuest != null)
		{
			return unlockQuest;
		}
		Route effectiveRoute = route != null ? route : getDefaultRoute();
		return effectiveRoute != null ? effectiveRoute.getQuest() : null;
	}

	public String getOverlayUnlockHintText()
	{
		if (unlockType == UnlockType.SPECIAL)
		{
			return unlockMessage;
		}
		return unlockHint;
	}

	public String getUnlockRestrictionMessage(Route route)
	{
		if (unlockType == null)
		{
			return null;
		}

		Route effectiveRoute = route != null ? route : getDefaultRoute();

		switch (unlockType)
		{
			case AUTOMATIC:
				return "This track was unlocked automatically.";

			case SPECIAL:
				return unlockMessage;

			case QUEST:
				Quest requiredQuest = getEffectiveUnlockQuest(effectiveRoute);

				if (requiredQuest != null)
				{
					return "This track can only be unlocked during the quest " + requiredQuest.getName() + ".";
				}
				return "This track can only be unlocked during a quest.";

			case RANDOM_EVENT:
				if (unlockMessage != null && !unlockMessage.isBlank())
				{
					return "This track can only be unlocked during the " + unlockMessage + " random event.";
				}
				return "This track can only be unlocked during a random event.";

			case HOLIDAY_EVENT:
				if (unlockMessage != null && !unlockMessage.isBlank())
				{
					return "This track can only be unlocked during a " + unlockMessage + " holiday event.";
				}
				return "This track can only be unlocked during a holiday event.";

			case MINIGAME:
				return "This track is unlocked by participating in the " + unlockMessage + " minigame.";

			default:
				return null;
		}
	}
}
