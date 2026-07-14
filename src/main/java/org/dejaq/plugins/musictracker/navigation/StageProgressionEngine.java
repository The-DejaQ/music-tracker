package org.dejaq.plugins.musictracker.navigation;

import java.util.List;
import java.util.Optional;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.track.TrackStep;

public interface StageProgressionEngine
{
	boolean hasCompletedStep(ProgressionContext progressionContext);

	boolean hasCrossedInteractionPoint(TrackStep currentTrackStep, WorldPoint previousPlayerLocation, WorldPoint currentPlayerLocation);

	int findClosestStageIndex(List<TrackStep> trackSteps, WorldPoint currentPlayerLocation);

	Optional<Integer> findMatchingUpcomingStage(List<TrackStep> trackSteps, int currentStageIndex,
												WorldPoint currentPlayerLocation, boolean planeChanged, boolean regionChanged);
}