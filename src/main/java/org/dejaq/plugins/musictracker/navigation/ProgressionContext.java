package org.dejaq.plugins.musictracker.navigation;

import java.util.List;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.track.TrackStep;

@Getter
public class ProgressionContext
{
	private final List<TrackStep> trackSteps;
	private final int currentStageIndex;
	private final WorldPoint previousPlayerLocation;
	private final WorldPoint currentPlayerLocation;

	public ProgressionContext(List<TrackStep> trackSteps, int currentStageIndex, WorldPoint previousPlayerLocation, WorldPoint currentPlayerLocation)
	{
		this.trackSteps = trackSteps;
		this.currentStageIndex = currentStageIndex;
		this.previousPlayerLocation = previousPlayerLocation;
		this.currentPlayerLocation = currentPlayerLocation;
	}

	public TrackStep getCurrentStep()
	{
		if (trackSteps == null || currentStageIndex < 0 || currentStageIndex >= trackSteps.size())
		{
			return null;
		}
		return trackSteps.get(currentStageIndex);
	}

	public TrackStep getNextStep()
	{
		int nextStageIndex = currentStageIndex + 1;
		if (trackSteps == null || nextStageIndex < 0 || nextStageIndex >= trackSteps.size())
		{
			return null;
		}
		return trackSteps.get(nextStageIndex);
	}

	public TrackStep getPreviousStep()
	{
		int previousStageIndex = currentStageIndex - 1;
		if (trackSteps == null || previousStageIndex < 0 || previousStageIndex >= trackSteps.size())
		{
			return null;
		}
		return trackSteps.get(previousStageIndex);
	}
}