package org.dejaq.plugins.musictracker;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.json.CustomTrackStore;
import org.dejaq.plugins.musictracker.json.RegionLoader;
import org.dejaq.plugins.musictracker.quest.Quest;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.UnlockType;

@Slf4j
@Singleton
public class MusicTrackManager
{
	@Getter
	private final List<MusicTrack> allTracks = new CopyOnWriteArrayList<>();
	@Getter
	private final Map<String, List<MusicTrack>> regionNameToTracks = new ConcurrentHashMap<>();

	private ExecutorService reloadExecutor;

	private final AtomicBoolean reloadInProgress = new AtomicBoolean(false);

	private final CustomTrackStore customTrackStore;
	private final Gson gson;

	@Inject
	public MusicTrackManager(CustomTrackStore customTrackStore, Gson gson)
	{
		this.customTrackStore = customTrackStore;
		this.gson = gson;
		loadAllRegionsFromJson();
	}

	public void startup()
	{
		reloadInProgress.set(false);

		if (reloadExecutor == null || reloadExecutor.isShutdown())
		{
			reloadExecutor = Executors.newSingleThreadExecutor(runnable -> {
				Thread thread = new Thread(runnable, "MusicTracker-MusicTrackManager-Reload");
				thread.setDaemon(true);
				return thread;
			});
		}
	}

	public void reloadRegionsFromJson(Runnable onReloadComplete)
	{
		if (reloadExecutor == null || reloadExecutor.isShutdown())
		{
			log.warn("Reload executor is not available, skipping region reload.");
			return;
		}

		if (!reloadInProgress.compareAndSet(false, true))
		{
			log.debug("Region reload already in progress, ignoring duplicate request.");
			return;
		}

		reloadExecutor.submit(() -> {
			try
			{
				log.debug("Reloading regions from JSON...");

				allTracks.clear();
				regionNameToTracks.clear();

				populateFromJsonRegions();
				mergeCustomContent();

				log.debug("Reloaded {} tracks from JSON.", allTracks.size());
			}
			catch (Exception reloadException)
			{
				log.warn("Failed to reload regions from JSON", reloadException);
			}
			finally
			{
				reloadInProgress.set(false);
				if (onReloadComplete != null)
				{
					SwingUtilities.invokeLater(onReloadComplete);
				}
			}
		});
	}

	public void shutdown()
	{
		if (reloadExecutor != null)
		{
			reloadExecutor.shutdownNow();
		}
	}

	private void loadAllRegionsFromJson()
	{
		populateFromJsonRegions();
		mergeCustomContent();
		log.debug("Loaded {} tracks from JSON regions.", allTracks.size());
	}

	private void populateFromJsonRegions()
	{
		Map<String, List<MusicTrack>> loadedRegionNameToTracks = RegionLoader.loadAllRegionsFromJson(gson);

		for (Map.Entry<String, List<MusicTrack>> regionEntry : loadedRegionNameToTracks.entrySet())
		{
			String regionName = regionEntry.getKey();
			List<MusicTrack> tracksInRegion = new CopyOnWriteArrayList<>(regionEntry.getValue());

			regionNameToTracks.put(regionName, tracksInRegion);
			allTracks.addAll(tracksInRegion);
		}
	}

	private void mergeCustomContent()
	{
		for (MusicTrack customTrack : customTrackStore.loadCustomTracks())
		{
			addTrackToInMemoryCollections(customTrack);
		}

		Map<String, List<Route>> customRoutesByTrackTitle = customTrackStore.loadCustomRoutesForExistingTracks();
		for (Map.Entry<String, List<Route>> trackEntry : customRoutesByTrackTitle.entrySet())
		{
			findTrackByTitle(trackEntry.getKey()).ifPresent(existingTrack -> {
				List<Route> combinedRoutes = new ArrayList<>(existingTrack.getAllRoutes());
				combinedRoutes.addAll(trackEntry.getValue());
				existingTrack.setRoutes(combinedRoutes);
			});
		}
	}

	private void addTrackToInMemoryCollections(MusicTrack track)
	{
		allTracks.add(track);
		String regionName = track.getRegion() != null && !track.getRegion().isBlank() ? track.getRegion() : "Custom";
		regionNameToTracks.computeIfAbsent(regionName, key -> new CopyOnWriteArrayList<>()).add(track);
	}

	public List<String> getRegionNames()
	{
		return new ArrayList<>(regionNameToTracks.keySet());
	}

	public List<MusicTrack> getTracksForRegion(String regionName)
	{
		List<MusicTrack> musicTracks = regionNameToTracks.get(regionName);
		return musicTracks != null ? List.copyOf(musicTracks) : List.of();
	}

	public Optional<MusicTrack> findTrackByTitle(String trackTitle)
	{
		if (trackTitle == null)
		{
			return Optional.empty();
		}
		return allTracks.stream()
			.filter(candidateTrack -> candidateTrack.getTitle().equalsIgnoreCase(trackTitle))
			.findFirst();
	}

	public Map<String, MusicTrack> buildTrackTitleIndex()
	{
		Map<String, MusicTrack> trackTitleIndex = new HashMap<>(allTracks.size() * 2);
		for (MusicTrack track : allTracks)
		{
			if (track.getTitle() != null)
			{
				trackTitleIndex.put(normalizeTrackTitleKey(track.getTitle()), track);
			}
		}
		return trackTitleIndex;
	}

	public static String normalizeTrackTitleKey(String trackTitle)
	{
		return trackTitle.toLowerCase(Locale.ROOT);
	}

	public MusicTrack getNextClosestInRegion(String regionName, Client client, Set<String> skippedTrackTitles, Set<String> unlockedTrackTitles)
	{
		List<MusicTrack> eligibleTracksInRegion = getTracksForRegion(regionName).stream()
			.filter(candidateTrack -> !skippedTrackTitles.contains(candidateTrack.getTitle()))
			.filter(candidateTrack -> !unlockedTrackTitles.contains(candidateTrack.getTitle()))
			.filter(this::isNormalUnlockType)
			.collect(Collectors.toList());

		if (eligibleTracksInRegion.isEmpty())
		{
			return null;
		}

		WorldPoint currentPlayerLocation = client.getLocalPlayer() != null ? client.getLocalPlayer().getWorldLocation() : null;

		if (currentPlayerLocation == null)
		{
			return eligibleTracksInRegion.get(0);
		}

		return eligibleTracksInRegion.stream()
			.min(Comparator.comparingDouble(candidateTrack -> distanceTo(currentPlayerLocation, candidateTrack.getUnlockPoint())))
			.orElse(null);
	}

	public MusicTrack getNextClosestToCurrentTrack(MusicTrack currentMusicTrack, Set<String> skippedTrackTitles, Set<String> unlockedTrackTitles)
	{
		if (currentMusicTrack == null)
		{
			return null;
		}

		WorldPoint referenceWorldPoint = currentMusicTrack.getFinalDestination();
		if (referenceWorldPoint == null || (referenceWorldPoint.getX() == 0 && referenceWorldPoint.getY() == 0))
		{
			return null;
		}

		List<MusicTrack> eligibleTracks = allTracks.stream()
			.filter(candidateTrack -> !candidateTrack.getTitle().equals(currentMusicTrack.getTitle()))
			.filter(candidateTrack -> !skippedTrackTitles.contains(candidateTrack.getTitle()))
			.filter(candidateTrack -> !unlockedTrackTitles.contains(candidateTrack.getTitle()))
			.filter(this::isNormalUnlockType)
			.collect(Collectors.toList());

		if (eligibleTracks.isEmpty())
		{
			return null;
		}

		return eligibleTracks.stream()
			.min(Comparator.comparingDouble(candidateTrack -> distanceTo(referenceWorldPoint, candidateTrack.getUnlockPoint())))
			.orElse(null);
	}

	private boolean isNormalUnlockType(MusicTrack musicTrack)
	{
		return musicTrack.getUnlockType() == null || musicTrack.getUnlockType() == UnlockType.NORMAL;
	}

	public double distanceTo(WorldPoint firstWorldPoint, WorldPoint secondWorldPoint)
	{
		if (firstWorldPoint == null || secondWorldPoint == null)
		{
			return Double.MAX_VALUE;
		}
		return firstWorldPoint.distanceTo(secondWorldPoint);
	}

	public String getNextPendingRegion(List<String> regionNamesInOrder, Set<String> skippedTrackTitles, int startingRegionIndex)
	{
		for (int regionIndex = startingRegionIndex; regionIndex < regionNamesInOrder.size(); regionIndex++)
		{
			String regionName = regionNamesInOrder.get(regionIndex);
			boolean regionHasPendingTracks = getTracksForRegion(regionName).stream()
				.anyMatch(candidateTrack -> !skippedTrackTitles.contains(candidateTrack.getTitle()));
			if (regionHasPendingTracks)
			{
				return regionName;
			}
		}
		return null;
	}

	public MusicTrack createCustomTrack(String title, String region, String location, String wikiUrl,
										UnlockType unlockType, Quest unlockQuest, String unlockMessage, String unlockHint)
	{
		MusicTrack customTrack = MusicTrack.builder()
			.title(title)
			.region(region)
			.location(location)
			.wikiUrl(wikiUrl)
			.unlockType(unlockType)
			.unlockQuest(unlockQuest)
			.unlockMessage(unlockMessage)
			.unlockHint(unlockHint)
			.custom(true)
			.build();

		addTrackToInMemoryCollections(customTrack);
		persistCustomTracks();
		return customTrack;
	}

	public void registerImportedCustomTrack(MusicTrack importedTrack)
	{
		addTrackToInMemoryCollections(importedTrack);
		persistCustomTracks();
	}

	public void applyTrackMetadataInMemory(MusicTrack track, String title, String region, String location,
										   String wikiUrl, UnlockType unlockType, Quest unlockQuest,
										   String unlockMessage, String unlockHint)
	{
		track.setTitle(title);
		track.setRegion(region);
		track.setLocation(location);
		track.setWikiUrl(wikiUrl);
		track.setUnlockType(unlockType);
		track.setUnlockQuest(unlockQuest);
		track.setUnlockMessage(unlockMessage);
		track.setUnlockHint(unlockHint);
	}

	public void updateCustomTrackMetadata(MusicTrack track, String title, String region, String location,
										  String wikiUrl, UnlockType unlockType, Quest unlockQuest,
										  String unlockMessage, String unlockHint)
	{
		if (!track.isCustom())
		{
			return;
		}

		applyTrackMetadataInMemory(track, title, region, location, wikiUrl, unlockType, unlockQuest, unlockMessage, unlockHint);

		persistCustomTracks();
	}

	public void deleteCustomTrack(MusicTrack track)
	{
		if (!track.isCustom())
		{
			return;
		}

		allTracks.remove(track);

		List<MusicTrack> regionTracks = regionNameToTracks.get(track.getRegion());
		if (regionTracks != null)
		{
			regionTracks.remove(track);
		}

		persistCustomTracks();
	}

	public Route addCustomRouteToTrack(MusicTrack track, Route newRoute)
	{
		List<Route> existingRoutes = new ArrayList<>(track.getAllRoutes());
		String resolvedName = customTrackStore.resolveNonConflictingRouteName(newRoute.getName(), existingRoutes);
		newRoute.setName(resolvedName);
		newRoute.setCustom(true);

		existingRoutes.add(newRoute);
		track.setRoutes(existingRoutes);

		persistTrackRoutes(track);
		return newRoute;
	}

	public void saveCustomRouteEdits(MusicTrack track)
	{
		persistTrackRoutes(track);
	}

	public void deleteCustomRoute(MusicTrack track, Route route)
	{
		if (!route.isCustom())
		{
			return;
		}

		List<Route> updatedRoutes = new ArrayList<>(track.getAllRoutes());
		updatedRoutes.remove(route);
		track.setRoutes(updatedRoutes);

		persistTrackRoutes(track);
	}

	public List<MusicTrack> getCustomTracks()
	{
		return allTracks.stream().filter(MusicTrack::isCustom).collect(Collectors.toList());
	}

	public List<Route> getCustomRoutesForTrack(MusicTrack track)
	{
		return track.getAllRoutes().stream().filter(Route::isCustom).collect(Collectors.toList());
	}

	private void persistTrackRoutes(MusicTrack track)
	{
		if (track.isCustom())
		{
			persistCustomTracks();
		}
		else
		{
			persistCustomRoutesForExistingTracks();
		}
	}

	private void persistCustomTracks()
	{
		customTrackStore.saveCustomTracks(getCustomTracks());
	}

	private void persistCustomRoutesForExistingTracks()
	{
		Map<String, List<Route>> customRoutesByTitle = new LinkedHashMap<>();
		for (MusicTrack track : allTracks)
		{
			if (track.isCustom())
			{
				continue;
			}
			List<Route> customRoutesOnThisTrack = getCustomRoutesForTrack(track);
			if (!customRoutesOnThisTrack.isEmpty())
			{
				customRoutesByTitle.put(track.getTitle(), customRoutesOnThisTrack);
			}
		}
		customTrackStore.saveCustomRoutesForExistingTracks(customRoutesByTitle);
	}
}