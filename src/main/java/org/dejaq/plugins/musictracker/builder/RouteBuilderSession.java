package org.dejaq.plugins.musictracker.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
	private volatile MusicTrack targetTrack;
	@Getter
	private volatile Route routeBeingEdited;
	@Getter
	private volatile boolean active;

	private volatile WorldPoint currentStepDestination;
	private final List<InteractionTarget> currentStepInteractions = new CopyOnWriteArrayList<>();
	private final List<TrackStep> finishedSteps = new CopyOnWriteArrayList<>();

	public synchronized void start(MusicTrack musicTrack, Route route)
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

	public synchronized void stop()
	{
		this.active = false;
		this.targetTrack = null;
		this.routeBeingEdited = null;
		this.currentStepDestination = null;
		this.currentStepInteractions.clear();
		this.finishedSteps.clear();
	}

	public synchronized void setCurrentStepDestination(WorldPoint destination)
	{
		if (destination != null)
		{
			this.currentStepDestination = destination;
		}
	}

	public synchronized void addInteractionToCurrentStep(InteractionTarget interactionTarget)
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

	public synchronized boolean finishCurrentStep(String stepName)
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

	public synchronized boolean hasInProgressStepContent()
	{
		return currentStepDestination != null || !currentStepInteractions.isEmpty();
	}

	public synchronized int getCurrentStepInteractionCount()
	{
		return currentStepInteractions.size();
	}

	public List<TrackStep> getFinishedSteps()
	{
		return Collections.unmodifiableList(new ArrayList<>(finishedSteps));
	}
}
