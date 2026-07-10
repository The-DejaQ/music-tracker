package org.dejaq.plugins.musictracker.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.MusicTrackEntityPoint;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.SpecialTrackRegistry;
import org.dejaq.plugins.musictracker.track.TrackStep;
import org.dejaq.plugins.musictracker.track.UnlockType;
import org.dejaq.plugins.musictracker.track.special.SpecialTrackHandler;

@Singleton
public class TrackNavigator
{
	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;

	private final MusicTrackerPlugin musicTrackerPlugin;
	private final StageProgressionEngine stageProgressionEngine;
	@Getter
	private final NavigationCoordinator navigationCoordinator;

	@Getter
	private MusicTrack currentTrack;
	@Getter
	private Route currentRoute;
	@Getter
	private int currentStage = 0;
	@Getter
	private final List<MusicTrackEntityPoint> activeEntityHighlights = new ArrayList<>();

	private WorldPoint lastPlayerLocation;

	@Inject
	public TrackNavigator(MusicTrackerPlugin musicTrackerPlugin, StageProgressionEngine stageProgressionEngine, NavigationCoordinator navigationCoordinator)
	{
		this.musicTrackerPlugin = musicTrackerPlugin;
		this.stageProgressionEngine = stageProgressionEngine;
		this.navigationCoordinator = navigationCoordinator;
	}

	public void setCurrentTrack(MusicTrack musicTrack)
	{
		if (!client.isClientThread())
		{
			clientThread.invokeLater(() -> setCurrentTrack(musicTrack));
			return;
		}
		this.currentTrack = musicTrack;
		this.currentRoute = musicTrack != null ? musicTrack.getDefaultRoute() : null;
		this.currentStage = 0;
		this.lastPlayerLocation = null;

		populateAllFutureHighlights();

		if (musicTrack != null && musicTrack.getUnlockType() == UnlockType.NORMAL) // we keep setting the route for non-pathed tracks
		{
			navigationCoordinator.onNavigationTargetChanged(currentTrack, getCurrentTargetPoint());
		}
	}

	public void setCurrentRoute(Route route)
	{
		if (!client.isClientThread())
		{
			clientThread.invokeLater(() -> setCurrentRoute(route));
			return;
		}
		if (currentTrack == null || route == null)
		{
			return;
		}
		this.currentRoute = route;
		this.currentStage = 0;
		this.lastPlayerLocation = null;

		populateAllFutureHighlights();
		navigationCoordinator.onNavigationTargetChanged(currentTrack, getCurrentTargetPoint());
	}

	public boolean setCurrentRouteByIndex(int routeIndex)
	{
		if (currentTrack == null)
		{
			return false;
		}
		List<Route> allRoutesForTrack = currentTrack.getAllRoutes();
		if (routeIndex < 0 || routeIndex >= allRoutesForTrack.size())
		{
			return false;
		}
		setCurrentRoute(allRoutesForTrack.get(routeIndex));
		return true;
	}

	public void clear()
	{
		this.currentTrack = null;
		this.currentRoute = null;
		this.currentStage = 0;
		this.lastPlayerLocation = null;
		navigationCoordinator.clearWorldMapPoints();
		clearEntityHighlights();
	}

	public boolean hasActiveTrackStep()
	{
		return currentTrack != null && hasSteps();
	}

	public TrackStep getCurrentStep()
	{
		return hasActiveTrackStep() ? getTrackStep(currentStage) : null;
	}

	public List<TrackStep> getCurrentSteps()
	{
		if (currentRoute != null && currentRoute.hasSteps())
		{
			return currentRoute.getTrackSteps();
		}
		return currentTrack != null ? currentTrack.getDefaultSteps() : List.of();
	}

	public int getTrackStepCount()
	{
		return getCurrentSteps().size();
	}

	public TrackStep getTrackStep(int stageIndex)
	{
		List<TrackStep> currentSteps = getCurrentSteps();
		if (stageIndex < 0 || stageIndex >= currentSteps.size())
		{
			return null;
		}
		return currentSteps.get(stageIndex);
	}

	public boolean hasSteps()
	{
		return !getCurrentSteps().isEmpty();
	}

	public WorldPoint getCurrentTargetPoint()
	{
		TrackStep currentTrackStep = getCurrentStep();
		if (currentTrackStep != null && currentTrackStep.getDestination() != null)
		{
			return currentTrackStep.getDestination();
		}
		return currentTrack != null ? currentTrack.getFinalDestination() : null;
	}

	public void setCurrentStage(int newStageIndex)
	{
		if (!client.isClientThread())
		{
			clientThread.invokeLater(() -> setCurrentStage(newStageIndex));
			return;
		}
		if (!hasActiveTrackStep())
		{
			return;
		}
		int highestValidStageIndex = getTrackStepCount() - 1;
		this.currentStage = Math.max(0, Math.min(newStageIndex, highestValidStageIndex));
		populateAllFutureHighlights();
		navigationCoordinator.onNavigationTargetChanged(currentTrack, getCurrentTargetPoint());
	}

	public void jumpToLastStage()
	{
		if (hasActiveTrackStep())
		{
			setCurrentStage(getTrackStepCount() - 1);
		}
	}

	public void clearEntityHighlights()
	{
		activeEntityHighlights.clear();
	}

	public void populateAllFutureHighlights()
	{
		if (!client.isClientThread())
		{
			clientThread.invokeLater(this::populateAllFutureHighlights);
			return;
		}
		activeEntityHighlights.clear();
		if (!hasActiveTrackStep())
		{
			return;
		}

		SpecialTrackHandler specialTrackHandler = SpecialTrackRegistry.getHandler(currentTrack, currentRoute);

		for (int stageIndex = currentStage; stageIndex < getTrackStepCount(); stageIndex++)
		{
			TrackStep trackStep = getTrackStep(stageIndex);
			if (trackStep == null)
			{
				continue;
			}

			if (stageIndex == currentStage)
			{
				List<MusicTrackEntityPoint> dynamicHighlights = specialTrackHandler.getDynamicEntityHighlights(
					currentTrack, currentRoute, trackStep, stageIndex, musicTrackerPlugin);

				if (dynamicHighlights != null)
				{
					activeEntityHighlights.addAll(dynamicHighlights);
					continue;
				}
			}

			for (InteractionTarget interactionTarget : trackStep.getAllHighlights())
			{
				MusicTrackEntityPoint entityHighlight = createHighlightForInteraction(trackStep, interactionTarget);
				if (entityHighlight != null)
				{
					activeEntityHighlights.add(entityHighlight);
				}
			}
		}
	}

	private MusicTrackEntityPoint createHighlightForInteraction(TrackStep trackStep, InteractionTarget interactionTarget)
	{
		if (interactionTarget == null)
		{
			return null;
		}
		WorldPoint highlightLocation = interactionTarget.getLocation() != null
			? interactionTarget.getLocation()
			: trackStep.getDestination();
		if (highlightLocation == null)
		{
			return null;
		}

		MusicTrackEntityPoint entityHighlight = MusicTrackEntityPoint.from(highlightLocation, interactionTarget);
		entityHighlight.setInteractionTarget(interactionTarget);
		return entityHighlight;
	}

	public void checkProgress()
	{
		if (!hasActiveTrackStep())
		{
			return;
		}

		clientThread.invokeLater(() -> {
			Player localPlayer = client.getLocalPlayer();
			if (localPlayer == null)
			{
				return;
			}

			WorldPoint currentPlayerLocation = localPlayer.getWorldLocation();
			SpecialTrackHandler specialTrackHandler = SpecialTrackRegistry.getHandler(currentTrack, currentRoute);

			if (specialTrackHandler.hasVolatileDynamicHighlights(currentTrack, currentRoute, musicTrackerPlugin))
			{
				populateAllFutureHighlights();
			}

			Integer forcedStageIndex = specialTrackHandler.getForcedStageIndex(currentTrack, currentRoute, currentStage, currentPlayerLocation, musicTrackerPlugin);
			if (forcedStageIndex != null && forcedStageIndex != currentStage)
			{
				setCurrentStage(forcedStageIndex);
				lastPlayerLocation = currentPlayerLocation;
				return;
			}

			if (!specialTrackHandler.canProgress(currentTrack, currentRoute, musicTrackerPlugin))
			{
				return;
			}

			if (stageProgressionEngine.hasCompletedStep(getCurrentStep(), currentPlayerLocation))
			{
				advanceStage();
				lastPlayerLocation = currentPlayerLocation;
				return;
			}

			boolean isFirstTickSinceStart = (lastPlayerLocation == null);
			boolean hasChangedPlane = lastPlayerLocation != null && lastPlayerLocation.getPlane() != currentPlayerLocation.getPlane();
			boolean hasChangedRegion = lastPlayerLocation != null && lastPlayerLocation.getRegionID() != currentPlayerLocation.getRegionID();

			if (isFirstTickSinceStart)
			{
				resyncToClosestStage(currentPlayerLocation);
			}
			else if (hasChangedPlane || hasChangedRegion)
			{
				resyncAfterPlaneOrRegionChange(currentPlayerLocation, hasChangedPlane, hasChangedRegion);
			}
			else
			{
				resetIfMovedSignificantlyBackward(currentPlayerLocation);
			}

			if (stageProgressionEngine.hasCrossedInteractionPoint(getCurrentStep(), lastPlayerLocation, currentPlayerLocation))
			{
				advanceStage();
			}

			lastPlayerLocation = currentPlayerLocation;
		});
	}

	private void advanceStage()
	{
		currentStage++;
		navigationCoordinator.onNavigationTargetChanged(currentTrack, getCurrentTargetPoint());
	}

	private void resyncToClosestStage(WorldPoint currentPlayerLocation)
	{
		int closestStageIndex = stageProgressionEngine.findClosestStageIndex(getCurrentSteps(), currentPlayerLocation);
		if (closestStageIndex != currentStage)
		{
			currentStage = closestStageIndex;
			navigationCoordinator.onNavigationTargetChanged(currentTrack, getCurrentTargetPoint());
		}
	}

	private void resyncAfterPlaneOrRegionChange(WorldPoint currentPlayerLocation, boolean hasChangedPlane, boolean hasChangedRegion)
	{
		Optional<Integer> matchingUpcomingStage = stageProgressionEngine.findMatchingUpcomingStage(
			getCurrentSteps(), currentStage, currentPlayerLocation, hasChangedPlane, hasChangedRegion);

		if (matchingUpcomingStage.isPresent())
		{
			currentStage = matchingUpcomingStage.get();
			navigationCoordinator.onNavigationTargetChanged(currentTrack, getCurrentTargetPoint());
			return;
		}

		int closestStageIndex = stageProgressionEngine.findClosestStageIndex(getCurrentSteps(), currentPlayerLocation);
		if (closestStageIndex == currentStage)
		{
			return;
		}

		TrackStep closestTrackStep = getTrackStep(closestStageIndex);
		boolean closestStepMatchesCurrentPlane = closestTrackStep != null && closestTrackStep.getDestination() != null
			&& closestTrackStep.getDestination().getPlane() == currentPlayerLocation.getPlane();

		boolean isAllowedBackwardMove = (closestStageIndex < currentStage) && closestStepMatchesCurrentPlane;
		boolean isSignificantBackwardMove = closestStageIndex < currentStage - 1;
		boolean isForwardMove = closestStageIndex > currentStage;

		if (isForwardMove || isSignificantBackwardMove || isAllowedBackwardMove)
		{
			currentStage = closestStageIndex;
			navigationCoordinator.onNavigationTargetChanged(currentTrack, getCurrentTargetPoint());
		}
	}

	private void resetIfMovedSignificantlyBackward(WorldPoint currentPlayerLocation)
	{
		if (currentStage == 0)
		{
			return;
		}
		int closestStageIndex = stageProgressionEngine.findClosestStageIndex(getCurrentSteps(), currentPlayerLocation);
		if (closestStageIndex < currentStage - 1)
		{
			currentStage = closestStageIndex;
			navigationCoordinator.onNavigationTargetChanged(currentTrack, getCurrentTargetPoint());
		}
	}
}