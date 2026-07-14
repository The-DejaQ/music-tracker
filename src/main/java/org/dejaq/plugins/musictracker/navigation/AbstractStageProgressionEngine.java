package org.dejaq.plugins.musictracker.navigation;

import java.util.List;
import java.util.Optional;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.TrackStep;

public abstract class AbstractStageProgressionEngine implements StageProgressionEngine
{
	protected static final int MAXIMUM_INTERACTION_SNAP_DISTANCE = 3;
	protected static final double INTERACTION_PROXIMITY_SCORE_MULTIPLIER = 0.25;
	protected static final double FORWARD_STAGE_BIAS_WEIGHT = 0.4;

	@Override
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

	@Override
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

	protected boolean isSamePlane(WorldPoint firstPoint, WorldPoint secondPoint)
	{
		return firstPoint != null && secondPoint != null && firstPoint.getPlane() == secondPoint.getPlane();
	}

	protected WorldPoint resolveAdvancementPoint(TrackStep trackStep, InteractionTarget advancementInteraction)
	{
		return advancementInteraction.getEffectiveLocation(trackStep.getDestination());
	}
}