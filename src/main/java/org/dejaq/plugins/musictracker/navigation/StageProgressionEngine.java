package org.dejaq.plugins.musictracker.navigation;

import java.util.List;
import java.util.Optional;
import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.TrackStep;

@Singleton
public class StageProgressionEngine
{
	private static final int MAXIMUM_INTERACTION_SNAP_DISTANCE = 3;
	private static final double INTERACTION_PROXIMITY_SCORE_MULTIPLIER = 0.25;
	private static final double FORWARD_STAGE_BIAS_WEIGHT = 0.4;

	public boolean hasCompletedStep(TrackStep currentTrackStep, WorldPoint currentPlayerLocation)
	{
		if (currentTrackStep == null || currentTrackStep.getDestination() == null || currentPlayerLocation == null)
		{
			return false;
		}

		if (!currentTrackStep.hasAdvancementInteraction()
			&& currentPlayerLocation.distanceTo(currentTrackStep.getDestination()) <= 1)
		{
			return true;
		}

		for (InteractionTarget advancementInteraction : currentTrackStep.getAdvancementInteractions())
		{
			WorldPoint interactionLocation = advancementInteraction.getEffectiveLocation(currentTrackStep.getDestination());
			if (interactionLocation != null && currentPlayerLocation.distanceTo(interactionLocation) == 0)
			{
				return true;
			}
		}
		return false;
	}

	public boolean hasCrossedInteractionPoint(TrackStep currentTrackStep, WorldPoint previousPlayerLocation, WorldPoint currentPlayerLocation)
	{
		if (currentTrackStep == null || !currentTrackStep.hasAdvancementInteraction() || previousPlayerLocation == null)
		{
			return false;
		}

		for (InteractionTarget advancementInteraction : currentTrackStep.getAdvancementInteractions())
		{
			WorldPoint interactionPoint = advancementInteraction.getEffectiveLocation(currentTrackStep.getDestination());
			if (hasPassedThroughPoint(previousPlayerLocation, currentPlayerLocation, interactionPoint))
			{
				return true;
			}
		}
		return false;
	}

	private boolean hasPassedThroughPoint(WorldPoint previousLocation, WorldPoint currentLocation, WorldPoint targetPoint)
	{
		if (targetPoint == null || previousLocation == null || currentLocation == null)
		{
			return false;
		}
		if (previousLocation.getPlane() != targetPoint.getPlane() || currentLocation.getPlane() != targetPoint.getPlane())
		{
			return false;
		}

		boolean crossedOnXAxis = (previousLocation.getX() < targetPoint.getX() && currentLocation.getX() >= targetPoint.getX())
			|| (previousLocation.getX() > targetPoint.getX() && currentLocation.getX() <= targetPoint.getX());

		boolean crossedOnYAxis = (previousLocation.getY() < targetPoint.getY() && currentLocation.getY() >= targetPoint.getY())
			|| (previousLocation.getY() > targetPoint.getY() && currentLocation.getY() <= targetPoint.getY());

		if (!crossedOnXAxis && !crossedOnYAxis)
		{
			return false;
		}

		return currentLocation.distanceTo(targetPoint) <= 1;
	}

	public int findClosestStageIndex(List<TrackStep> trackSteps, WorldPoint currentPlayerLocation)
	{
		if (trackSteps == null || trackSteps.isEmpty() || currentPlayerLocation == null)
		{
			return 0;
		}

		int closestStageIndex = 0;
		double bestScore = Double.MAX_VALUE;

		for (int stageIndex = 0; stageIndex < trackSteps.size(); stageIndex++)
		{
			TrackStep trackStep = trackSteps.get(stageIndex);
			if (trackStep == null || trackStep.getDestination() == null)
			{
				continue;
			}

			double distanceToDestination = currentPlayerLocation.distanceTo(trackStep.getDestination());

			if (trackStep.hasAdvancementInteraction())
			{
				for (InteractionTarget advancementInteraction : trackStep.getAdvancementInteractions())
				{
					WorldPoint interactionLocation = advancementInteraction.getEffectiveLocation(trackStep.getDestination());
					if (interactionLocation == null)
					{
						continue;
					}
					double distanceToInteraction = currentPlayerLocation.distanceTo(interactionLocation);
					if (distanceToInteraction <= MAXIMUM_INTERACTION_SNAP_DISTANCE)
					{
						distanceToDestination = Math.min(distanceToDestination, distanceToInteraction * INTERACTION_PROXIMITY_SCORE_MULTIPLIER);
					}
				}
			}

			double score = distanceToDestination + (stageIndex * FORWARD_STAGE_BIAS_WEIGHT);
			if (score < bestScore)
			{
				bestScore = score;
				closestStageIndex = stageIndex;
			}
		}
		return closestStageIndex;
	}

	public Optional<Integer> findMatchingUpcomingStage(List<TrackStep> trackSteps, int currentStageIndex,
													   WorldPoint currentPlayerLocation, boolean planeChanged, boolean regionChanged)
	{
		if (trackSteps == null || (!planeChanged && !regionChanged))
		{
			return Optional.empty();
		}

		for (int stageIndex = currentStageIndex + 1; stageIndex < trackSteps.size(); stageIndex++)
		{
			TrackStep trackStep = trackSteps.get(stageIndex);
			if (trackStep == null || trackStep.getDestination() == null)
			{
				continue;
			}

			boolean matchesNewPlane = planeChanged && trackStep.getDestination().getPlane() == currentPlayerLocation.getPlane();
			boolean matchesNewRegion = regionChanged && trackStep.getDestination().getRegionID() == currentPlayerLocation.getRegionID();

			if (matchesNewPlane || matchesNewRegion)
			{
				return Optional.of(stageIndex);
			}
		}
		return Optional.empty();
	}
}