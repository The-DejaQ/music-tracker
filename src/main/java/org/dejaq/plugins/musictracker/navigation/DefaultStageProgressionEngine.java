package org.dejaq.plugins.musictracker.navigation;

import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.TrackStep;

@Singleton
public class DefaultStageProgressionEngine extends AbstractStageProgressionEngine
{
	@Override
	public boolean hasCompletedStep(ProgressionContext progressionContext)
	{
		TrackStep currentTrackStep = progressionContext.getCurrentStep();
		WorldPoint currentPlayerLocation = progressionContext.getCurrentPlayerLocation();

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
			WorldPoint interactionLocation = resolveAdvancementPoint(currentTrackStep, advancementInteraction);
			if (interactionLocation != null && currentPlayerLocation.distanceTo(interactionLocation) == 0)
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasCrossedInteractionPoint(TrackStep currentTrackStep, WorldPoint previousPlayerLocation, WorldPoint currentPlayerLocation)
	{
		if (currentTrackStep == null || !currentTrackStep.hasAdvancementInteraction() || previousPlayerLocation == null)
		{
			return false;
		}

		for (InteractionTarget advancementInteraction : currentTrackStep.getAdvancementInteractions())
		{
			WorldPoint interactionPoint = resolveAdvancementPoint(currentTrackStep, advancementInteraction);
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
}