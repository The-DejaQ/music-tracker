package org.dejaq.plugins.musictracker.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.track.Route;

@Slf4j
@Singleton
public class CustomTrackStore
{
	private static final File DATA_DIRECTORY = new File(RuneLite.RUNELITE_DIR, "music-tracker");
	private static final File CUSTOM_TRACKS_FILE = new File(DATA_DIRECTORY, "custom-tracks.json");
	private static final File CUSTOM_ROUTES_FILE = new File(DATA_DIRECTORY, "custom-routes.json");

	@Inject
	private Gson gson;

	private Gson prettyPrintingGson()
	{
		return gson.newBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	}

	public enum ImportedContentType
	{
		TRACK,
		ROUTE,
		UNKNOWN
	}

	public List<MusicTrack> loadCustomTracks()
	{
		List<MusicTrack> customTracks = new ArrayList<>();

		if (!CUSTOM_TRACKS_FILE.exists())
		{
			return customTracks;
		}

		try (FileReader fileReader = new FileReader(CUSTOM_TRACKS_FILE))
		{
			Type listType = new TypeToken<List<MusicTrackJson>>()
			{
			}.getType();
			List<MusicTrackJson> musicTrackJsonList = prettyPrintingGson().fromJson(fileReader, listType);

			if (musicTrackJsonList == null)
			{
				return customTracks;
			}

			for (MusicTrackJson musicTrackJson : musicTrackJsonList)
			{
				MusicTrack customTrack = RegionLoader.convertToMusicTrack(musicTrackJson);
				customTrack.setCustom(true);
				for (Route route : customTrack.getAllRoutes())
				{
					route.setCustom(true);
				}
				customTracks.add(customTrack);
			}
		}
		catch (Exception loadException)
		{
			log.warn("Failed to load custom tracks from {}", CUSTOM_TRACKS_FILE, loadException);
		}

		return customTracks;
	}

	public Map<String, List<Route>> loadCustomRoutesForExistingTracks()
	{
		Map<String, List<Route>> customRoutesByTrackTitle = new HashMap<>();

		if (!CUSTOM_ROUTES_FILE.exists())
		{
			return customRoutesByTrackTitle;
		}

		try (FileReader fileReader = new FileReader(CUSTOM_ROUTES_FILE))
		{
			Type mapType = new TypeToken<Map<String, List<RouteJson>>>()
			{
			}.getType();
			Map<String, List<RouteJson>> routeJsonMap = prettyPrintingGson().fromJson(fileReader, mapType);

			if (routeJsonMap == null)
			{
				return customRoutesByTrackTitle;
			}

			for (Map.Entry<String, List<RouteJson>> trackEntry : routeJsonMap.entrySet())
			{
				String trackTitleLower = trackEntry.getKey().toLowerCase(Locale.ROOT);
				List<Route> customRoutes = new ArrayList<>();

				int routeIndex = 0;
				for (RouteJson routeJson : trackEntry.getValue())
				{
					Route customRoute = RegionLoader.convertRoute(routeJson, routeIndex, trackEntry.getKey());
					customRoute.setCustom(true);
					customRoutes.add(customRoute);
					routeIndex++;
				}

				customRoutesByTrackTitle.put(trackTitleLower, customRoutes);
			}
		}
		catch (Exception loadException)
		{
			log.warn("Failed to load custom routes from {}", CUSTOM_ROUTES_FILE, loadException);
		}

		return customRoutesByTrackTitle;
	}

	public void saveCustomTracks(List<MusicTrack> customTracks)
	{
		ensureDataDirectoryExists();

		List<MusicTrackJson> musicTrackJsonList = new ArrayList<>();
		for (MusicTrack customTrack : customTracks)
		{
			musicTrackJsonList.add(RegionWriter.toJson(customTrack));
		}

		try (FileWriter fileWriter = new FileWriter(CUSTOM_TRACKS_FILE))
		{
			prettyPrintingGson().toJson(musicTrackJsonList, fileWriter);
		}
		catch (IOException saveException)
		{
			log.warn("Failed to save custom tracks to {}", CUSTOM_TRACKS_FILE, saveException);
		}
	}

	public void saveCustomRoutesForExistingTracks(Map<String, List<Route>> customRoutesByTrackTitle)
	{
		ensureDataDirectoryExists();

		Map<String, List<RouteJson>> routeJsonMap = new HashMap<>();
		for (Map.Entry<String, List<Route>> trackEntry : customRoutesByTrackTitle.entrySet())
		{
			if (trackEntry.getValue().isEmpty())
			{
				continue;
			}
			List<RouteJson> routeJsonList = new ArrayList<>();
			for (Route route : trackEntry.getValue())
			{
				routeJsonList.add(RegionWriter.toJson(route));
			}
			routeJsonMap.put(trackEntry.getKey(), routeJsonList);
		}

		try (FileWriter fileWriter = new FileWriter(CUSTOM_ROUTES_FILE))
		{
			prettyPrintingGson().toJson(routeJsonMap, fileWriter);
		}
		catch (IOException saveException)
		{
			log.warn("Failed to save custom routes to {}", CUSTOM_ROUTES_FILE, saveException);
		}
	}

	private void ensureDataDirectoryExists()
	{
		if (!DATA_DIRECTORY.exists())
		{
			DATA_DIRECTORY.mkdirs();
		}
	}

	public String resolveNonConflictingRouteName(String desiredName, List<Route> existingRoutesOnTrack)
	{
		String candidateName = desiredName == null || desiredName.isBlank() ? "New Route" : desiredName;

		if (!routeNameIsTaken(candidateName, existingRoutesOnTrack))
		{
			return candidateName;
		}

		int suffixNumber = 1;
		String renamedCandidate;
		do
		{
			renamedCandidate = candidateName + " (" + suffixNumber + ")";
			suffixNumber++;
		}
		while (routeNameIsTaken(renamedCandidate, existingRoutesOnTrack));

		return renamedCandidate;
	}

	private boolean routeNameIsTaken(String candidateName, List<Route> existingRoutesOnTrack)
	{
		if (existingRoutesOnTrack == null)
		{
			return false;
		}
		for (Route existingRoute : existingRoutesOnTrack)
		{
			if (existingRoute.getName() != null && existingRoute.getName().equalsIgnoreCase(candidateName))
			{
				return true;
			}
		}
		return false;
	}

	public String exportTrackToJson(MusicTrack musicTrack)
	{
		return prettyPrintingGson().toJson(RegionWriter.toJson(musicTrack));
	}

	public String exportRouteToJson(Route route)
	{
		return prettyPrintingGson().toJson(RegionWriter.toJson(route));
	}

	public String exportRouteToJson(Route route, String owningTrackTitle)
	{
		return prettyPrintingGson().toJson(RegionWriter.toJson(route, owningTrackTitle));
	}

	public ImportedContentType detectContentType(String jsonText)
	{
		try
		{
			JsonElement parsedElement = gson.fromJson(jsonText, JsonElement.class);
			if (!parsedElement.isJsonObject())
			{
				return ImportedContentType.UNKNOWN;
			}

			JsonObject jsonObject = parsedElement.getAsJsonObject();

			if (jsonObject.has("title") || jsonObject.has("region") || jsonObject.has("wikiUrl")
				|| jsonObject.has("unlockType") || jsonObject.has("routes"))
			{
				return ImportedContentType.TRACK;
			}

			if (jsonObject.has("defaultRoute") || jsonObject.has("trackSteps") || jsonObject.has("name"))
			{
				return ImportedContentType.ROUTE;
			}

			return ImportedContentType.UNKNOWN;
		}
		catch (Exception detectException)
		{
			return ImportedContentType.UNKNOWN;
		}
	}

	public MusicTrack importTrackFromJson(String jsonText)
	{
		MusicTrackJson musicTrackJson = gson.fromJson(jsonText, MusicTrackJson.class);
		MusicTrack importedTrack = RegionLoader.convertToMusicTrack(musicTrackJson);
		importedTrack.setCustom(true);
		for (Route route : importedTrack.getAllRoutes())
		{
			route.setCustom(true);
		}
		return importedTrack;
	}

	public String peekRouteTrackTitle(String jsonText)
	{
		try
		{
			JsonElement parsedElement = gson.fromJson(jsonText, JsonElement.class);
			if (!parsedElement.isJsonObject())
			{
				return null;
			}

			JsonElement trackElement = parsedElement.getAsJsonObject().get("track");
			if (trackElement == null || !trackElement.isJsonPrimitive())
			{
				return null;
			}

			String trackTitle = trackElement.getAsString();
			return (trackTitle == null || trackTitle.isBlank()) ? null : trackTitle.trim();
		}
		catch (Exception peekException)
		{
			return null;
		}
	}

	public Route importRouteFromJson(String jsonText, int fallbackRouteIndex, String trackTitleForLogging)
	{
		RouteJson routeJson = gson.fromJson(jsonText, RouteJson.class);
		Route importedRoute = RegionLoader.convertRoute(routeJson, fallbackRouteIndex, trackTitleForLogging);
		importedRoute.setCustom(true);
		return importedRoute;
	}
}