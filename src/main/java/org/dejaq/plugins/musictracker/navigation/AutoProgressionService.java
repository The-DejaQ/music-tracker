package org.dejaq.plugins.musictracker.navigation;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackManager;
import org.dejaq.plugins.musictracker.track.UnlockType;
import org.dejaq.plugins.musictracker.state.TrackingStateService;
import org.dejaq.plugins.musictracker.track.TrackStep;

@Singleton
public class AutoProgressionService
{

	private static final double SIGNIFICANT_STEP_PROXIMITY_ADVANTAGE_IN_TILES = 5.0;

	@Inject
	private Client client;
	@Inject
	private MusicTrackManager musicTrackManager;
	@Inject
	private TrackingStateService trackingStateService;
	@Inject
	private StageProgressionEngine stageProgressionEngine;

	public Optional<MusicTrack> findNextAutoProgressableTrackInRegion(String regionName)
	{
		if (regionName == null)
		{
			return Optional.empty();
		}

		List<MusicTrack> eligibleTracksInRegion = musicTrackManager.getTracksForRegion(regionName).stream()
			.filter(candidateTrack -> !trackingStateService.isTrackSkipped(candidateTrack.getTitle()))
			.filter(candidateTrack -> !trackingStateService.isTrackUnlocked(candidateTrack.getTitle()))
			.filter(this::isAutoProgressable)
			.collect(Collectors.toList());

		if (eligibleTracksInRegion.isEmpty())
		{
			return Optional.empty();
		}

		WorldPoint currentPlayerLocation = client.getLocalPlayer() != null ? client.getLocalPlayer().getWorldLocation() : null;
		if (currentPlayerLocation == null)
		{
			return Optional.of(eligibleTracksInRegion.get(0));
		}

		Optional<MusicTrack> closestByFinalDestination = findClosestByFinalDestination(eligibleTracksInRegion, currentPlayerLocation);
		Optional<MusicTrack> closestByIntermediateStep = findClosestByIntermediateStep(eligibleTracksInRegion, currentPlayerLocation);

		if (closestByFinalDestination.isEmpty())
		{
			return closestByIntermediateStep;
		}
		if (closestByIntermediateStep.isEmpty())
		{
			return closestByFinalDestination;
		}

		double finalDestinationDistance = musicTrackManager.distanceTo(currentPlayerLocation, closestByFinalDestination.get().getUnlockPoint());
		double intermediateStepDistance = distanceToClosestStep(closestByIntermediateStep.get(), currentPlayerLocation);

		boolean intermediateStepIsSignificantlyCloser =
			(finalDestinationDistance - intermediateStepDistance) >= SIGNIFICANT_STEP_PROXIMITY_ADVANTAGE_IN_TILES;

		return intermediateStepIsSignificantlyCloser ? closestByIntermediateStep : closestByFinalDestination;
	}

	private Optional<MusicTrack> findClosestByFinalDestination(List<MusicTrack> eligibleTracksInRegion, WorldPoint currentPlayerLocation)
	{
		return eligibleTracksInRegion.stream()
			.min(Comparator.comparingDouble(candidateTrack -> musicTrackManager.distanceTo(currentPlayerLocation, candidateTrack.getUnlockPoint())));
	}

	private Optional<MusicTrack> findClosestByIntermediateStep(List<MusicTrack> eligibleTracksInRegion, WorldPoint currentPlayerLocation)
	{
		return eligibleTracksInRegion.stream()
			.min(Comparator.comparingDouble(candidateTrack -> distanceToClosestStep(candidateTrack, currentPlayerLocation)));
	}

	private double distanceToClosestStep(MusicTrack musicTrack, WorldPoint currentPlayerLocation)
	{
		List<TrackStep> defaultSteps = musicTrack.getDefaultSteps();
		if (defaultSteps.isEmpty())
		{
			return musicTrackManager.distanceTo(currentPlayerLocation, musicTrack.getUnlockPoint());
		}

		int closestStageIndex = stageProgressionEngine.findClosestStageIndex(defaultSteps, currentPlayerLocation);
		TrackStep closestTrackStep = defaultSteps.get(closestStageIndex);
		return musicTrackManager.distanceTo(currentPlayerLocation, closestTrackStep.getDestination());
	}

	public Optional<String> findNextPendingRegion(String currentRegionName)
	{
		List<String> regionNamesInOrder = musicTrackManager.getRegionNames();
		int currentRegionIndex = regionNamesInOrder.indexOf(currentRegionName);
		return Optional.ofNullable(musicTrackManager.getNextPendingRegion(
			regionNamesInOrder, trackingStateService.getSkippedTrackTitles(), currentRegionIndex + 1));
	}

	public Optional<MusicTrack> findNextClosestTrackToCurrent(MusicTrack currentMusicTrack)
	{
		if (currentMusicTrack == null)
		{
			return Optional.empty();
		}
		return Optional.ofNullable(musicTrackManager.getNextClosestToCurrentTrack(
			currentMusicTrack, trackingStateService.getSkippedTrackTitles(), trackingStateService.getCurrentUnlockedTrackTitles()));
	}

	private boolean isAutoProgressable(MusicTrack musicTrack)
	{
		return musicTrack.getUnlockType() == null
			|| musicTrack.getUnlockType() == UnlockType.NORMAL;
	}

	public String buildCompletionMessage()
	{
		Set<String> remainingTrackCategories = new HashSet<>();

		for (MusicTrack musicTrack : musicTrackManager.getAllTracks())
		{
			if (trackingStateService.isTrackUnlocked(musicTrack.getTitle()) || trackingStateService.isTrackSkipped(musicTrack.getTitle()))
			{
				continue;
			}
			if (musicTrack.getUnlockType() == UnlockType.HOLIDAY_EVENT)
			{
				remainingTrackCategories.add("holiday");
			}
			else if (musicTrack.getUnlockType() == UnlockType.QUEST)
			{
				remainingTrackCategories.add("quest");
			}
			else
			{
				remainingTrackCategories.add("normal");
			}
		}

		boolean hasSkippedTracks = !trackingStateService.getSkippedTrackTitles().isEmpty();

		if (remainingTrackCategories.isEmpty())
		{
			return hasSkippedTracks
				? "Congratulations! You've unlocked all music tracks (excluding skipped ones). You can clear skipped tracks in the settings."
				: "Congratulations! You've unlocked every music track in the game.";
		}

		StringBuilder completionMessageBuilder = new StringBuilder("Congratulations! You've unlocked all ");

		if (remainingTrackCategories.contains("normal")
			|| (remainingTrackCategories.contains("quest") && remainingTrackCategories.contains("holiday")))
		{
			completionMessageBuilder.append("non-quest and non-holiday ");
		}
		else if (remainingTrackCategories.contains("quest"))
		{
			completionMessageBuilder.append("non-quest ");
		}
		else if (remainingTrackCategories.contains("holiday"))
		{
			completionMessageBuilder.append("non-holiday ");
		}

		completionMessageBuilder.append("music tracks.");

		if (hasSkippedTracks)
		{
			completionMessageBuilder.append(" You still have skipped tracks. You can clear them in the settings.");
		}

		return completionMessageBuilder.toString();
	}
}