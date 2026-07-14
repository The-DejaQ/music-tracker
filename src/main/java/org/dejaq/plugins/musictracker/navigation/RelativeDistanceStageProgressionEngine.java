package org.dejaq.plugins.musictracker.navigation;

import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.track.TrackStep;

@Singleton
public class RelativeDistanceStageProgressionEngine extends AbstractStageProgressionEngine
{
	private static final int CURRENT_DESTINATION_SLACK_DISTANCE = 5;
	private static final int DIRECT_ARRIVAL_DISTANCE = 1;

	@Override
	public boolean hasCompletedStep(ProgressionContext progressionContext)
	{
		TrackStep currentTrackStep = progressionContext.getCurrentStep();
		WorldPoint currentPlayerLocation = progressionContext.getCurrentPlayerLocation();

		if (currentTrackStep == null || currentTrackStep.getDestination() == null || currentPlayerLocation == null)
		{
			return false;
		}

		WorldPoint currentDestination = currentTrackStep.getDestination();

		if (isSamePlane(currentPlayerLocation, currentDestination)
			&& currentPlayerLocation.distanceTo(currentDestination) <= DIRECT_ARRIVAL_DISTANCE)
		{
			return true;
		}

		TrackStep nextTrackStep = progressionContext.getNextStep();
		if (nextTrackStep == null || nextTrackStep.getDestination() == null)
		{
			return false;
		}

		WorldPoint nextDestination = nextTrackStep.getDestination();
		if (!isSamePlane(currentPlayerLocation, currentDestination) || !isSamePlane(currentPlayerLocation, nextDestination))
		{
			return false;
		}

		int distanceToCurrent = currentPlayerLocation.distanceTo(currentDestination);
		int distanceToNext = currentPlayerLocation.distanceTo(nextDestination);

		boolean nearCurrentDestination = distanceToCurrent <= CURRENT_DESTINATION_SLACK_DISTANCE;
		boolean closerToNextThanCurrent = distanceToNext < distanceToCurrent;

		return nearCurrentDestination && closerToNextThanCurrent;
	}

	@Override
	public boolean hasCrossedInteractionPoint(TrackStep currentTrackStep, WorldPoint previousPlayerLocation, WorldPoint currentPlayerLocation)
	{
		return false;
	}
}