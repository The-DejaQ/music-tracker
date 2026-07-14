package org.dejaq.plugins.musictracker.navigation;

import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.TrackStep;

@Singleton
public class ProximityStageProgressionEngine extends AbstractStageProgressionEngine
{
	private static final int DESTINATION_PROXIMITY_THRESHOLD = 2;
	private static final int INTERACTION_PROXIMITY_THRESHOLD = 1;

	@Override
	public boolean hasCompletedStep(ProgressionContext progressionContext)
	{
		TrackStep currentTrackStep = progressionContext.getCurrentStep();
		WorldPoint currentPlayerLocation = progressionContext.getCurrentPlayerLocation();

		if (currentTrackStep == null || currentTrackStep.getDestination() == null || currentPlayerLocation == null)
		{
			return false;
		}

		if (currentTrackStep.hasAdvancementInteraction())
		{
			for (InteractionTarget advancementInteraction : currentTrackStep.getAdvancementInteractions())
			{
				WorldPoint interactionLocation = resolveAdvancementPoint(currentTrackStep, advancementInteraction);
				if (interactionLocation != null
					&& isSamePlane(currentPlayerLocation, interactionLocation)
					&& currentPlayerLocation.distanceTo(interactionLocation) <= INTERACTION_PROXIMITY_THRESHOLD)
				{
					return true;
				}
			}
			return false;
		}

		return isSamePlane(currentPlayerLocation, currentTrackStep.getDestination())
			&& currentPlayerLocation.distanceTo(currentTrackStep.getDestination()) <= DESTINATION_PROXIMITY_THRESHOLD;
	}

	@Override
	public boolean hasCrossedInteractionPoint(TrackStep currentTrackStep, WorldPoint previousPlayerLocation, WorldPoint currentPlayerLocation)
	{
		return false;
	}
}