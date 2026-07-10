package org.dejaq.plugins.musictracker.builder;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.TrackStep;

@Singleton
public class RouteBuilderSession
{
	@Getter
	private MusicTrack targetTrack;
	@Getter
	private Route routeBeingEdited;
	@Getter
	private boolean active;

	private WorldPoint currentStepDestination;
	private final List<InteractionTarget> currentStepInteractions = new ArrayList<>();
	private final List<TrackStep> finishedSteps = new ArrayList<>();

	public void start(MusicTrack musicTrack, Route route)
	{
		this.targetTrack = musicTrack;
		this.routeBeingEdited = route;
		this.finishedSteps.clear();
		if (route.getTrackSteps() != null)
		{
			this.finishedSteps.addAll(route.getTrackSteps());
		}
		this.currentStepDestination = null;
		this.currentStepInteractions.clear();
		this.active = true;
	}

	public void stop()
	{
		this.active = false;
		this.targetTrack = null;
		this.routeBeingEdited = null;
		this.currentStepDestination = null;
		this.currentStepInteractions.clear();
		this.finishedSteps.clear();
	}

	public void setCurrentStepDestination(WorldPoint destination)
	{
		if (destination != null)
		{
			this.currentStepDestination = destination;
		}
	}

	public void addInteractionToCurrentStep(InteractionTarget interactionTarget)
	{
		if (interactionTarget == null)
		{
			return;
		}
		currentStepInteractions.add(interactionTarget);
		if (currentStepDestination == null && interactionTarget.getLocation() != null)
		{
			currentStepDestination = interactionTarget.getLocation();
		}
	}

	public boolean finishCurrentStep(String stepName)
	{
		if (currentStepDestination == null && currentStepInteractions.isEmpty())
		{
			return false;
		}

		TrackStep.TrackStepBuilder trackStepBuilder = TrackStep.builder()
			.destination(currentStepDestination != null ? currentStepDestination : new WorldPoint(0, 0, 0))
			.interactions(new ArrayList<>(currentStepInteractions));

		if (stepName != null && !stepName.isBlank())
		{
			trackStepBuilder.name(stepName.trim());
		}

		finishedSteps.add(trackStepBuilder.build());

		currentStepDestination = null;
		currentStepInteractions.clear();
		return true;
	}

	public boolean hasInProgressStepContent()
	{
		return currentStepDestination != null || !currentStepInteractions.isEmpty();
	}

	public int getCurrentStepInteractionCount()
	{
		return currentStepInteractions.size();
	}

	public List<TrackStep> getFinishedSteps()
	{
		return new ArrayList<>(finishedSteps);
	}
}