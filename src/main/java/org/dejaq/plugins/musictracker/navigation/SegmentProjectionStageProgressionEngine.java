package org.dejaq.plugins.musictracker.navigation;

import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.track.TrackStep;

@Singleton
public class SegmentProjectionStageProgressionEngine extends AbstractStageProgressionEngine
{
	private static final int DIRECT_ARRIVAL_DISTANCE = 1;
	private static final double SEGMENT_PASS_THRESHOLD = 1.0;

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

		TrackStep previousTrackStep = progressionContext.getPreviousStep();
		WorldPoint segmentStart = previousTrackStep != null && previousTrackStep.getDestination() != null
			? previousTrackStep.getDestination()
			: progressionContext.getPreviousPlayerLocation();

		if (segmentStart == null || !isSamePlane(currentPlayerLocation, currentDestination) || !isSamePlane(segmentStart, currentDestination))
		{
			return false;
		}

		return hasProjectionPassedSegmentEnd(segmentStart, currentDestination, currentPlayerLocation);
	}

	@Override
	public boolean hasCrossedInteractionPoint(TrackStep currentTrackStep, WorldPoint previousPlayerLocation, WorldPoint currentPlayerLocation)
	{
		return false;
	}

	private boolean hasProjectionPassedSegmentEnd(WorldPoint segmentStart, WorldPoint segmentEnd, WorldPoint playerLocation)
	{
		double segmentDeltaX = segmentEnd.getX() - segmentStart.getX();
		double segmentDeltaY = segmentEnd.getY() - segmentStart.getY();

		double segmentLengthSquared = (segmentDeltaX * segmentDeltaX) + (segmentDeltaY * segmentDeltaY);
		if (segmentLengthSquared == 0.0)
		{
			return false;
		}

		double playerDeltaX = playerLocation.getX() - segmentStart.getX();
		double playerDeltaY = playerLocation.getY() - segmentStart.getY();

		double projectionFraction = ((playerDeltaX * segmentDeltaX) + (playerDeltaY * segmentDeltaY)) / segmentLengthSquared;

		return projectionFraction >= SEGMENT_PASS_THRESHOLD;
	}
}