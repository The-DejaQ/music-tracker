package org.dejaq.plugins.musictracker.state;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import org.dejaq.plugins.musictracker.MusicTrackerConfig;

@Slf4j
@Singleton
public class TrackingStateService
{
	private static final String CONFIG_GROUP_NAME = "music-tracker";
	private static final String SKIPPED_TRACKS_CONFIG_KEY = "skippedTracks";
	private static final String DEFAULT_STARTING_REGION_NAME = "Misthalin";

	@Inject
	private ConfigManager configManager;
	@Inject
	private MusicTrackerConfig musicTrackerConfig;
	@Inject
	private Gson gson;

	@Setter
	@Getter
	private boolean trackingActive = false;

	@Setter
	@Getter
	private String currentRegionName = DEFAULT_STARTING_REGION_NAME;

	private final Set<String> unlockedTrackTitles = ConcurrentHashMap.newKeySet();
	private final Set<String> fakeUnlockedTrackTitles = ConcurrentHashMap.newKeySet();
	@Getter
	private final Set<String> skippedTrackTitles = ConcurrentHashMap.newKeySet();

	public Set<String> getCurrentUnlockedTrackTitles()
	{
		return musicTrackerConfig.lockedTracks() ? fakeUnlockedTrackTitles : unlockedTrackTitles;
	}

	public boolean isTrackUnlocked(String trackTitle)
	{
		return getCurrentUnlockedTrackTitles().contains(trackTitle);
	}

	public boolean recordTrackUnlocked(String trackTitle)
	{
		return getCurrentUnlockedTrackTitles().add(trackTitle);
	}

	public void clearUnlockedTracks()
	{
		getCurrentUnlockedTrackTitles().clear();
	}

	public void resetAccountScopedState()
	{
		unlockedTrackTitles.clear();
		fakeUnlockedTrackTitles.clear();
	}

	public boolean isTrackSkipped(String trackTitle)
	{
		return skippedTrackTitles.contains(trackTitle);
	}

	public void setTrackSkipped(String trackTitle, boolean skipped)
	{
		boolean changed = skipped ? skippedTrackTitles.add(trackTitle) : skippedTrackTitles.remove(trackTitle);
		if (changed)
		{
			persistSkippedTracksIfEnabled();
		}
	}

	public void clearSkippedTracks()
	{
		skippedTrackTitles.clear();
		persistSkippedTracksIfEnabled();
	}

	public void disableAndWipeSkippedTracks()
	{
		skippedTrackTitles.clear();
		configManager.setConfiguration(CONFIG_GROUP_NAME, SKIPPED_TRACKS_CONFIG_KEY, "");
	}

	public void loadSkippedTracksFromConfig()
	{
		if (!musicTrackerConfig.saveSkippedTracks())
		{
			skippedTrackTitles.clear();
			return;
		}

		String persistedSkippedTracksJson = musicTrackerConfig.skippedTracks();
		if (persistedSkippedTracksJson == null || persistedSkippedTracksJson.isBlank())
		{
			skippedTrackTitles.clear();
			return;
		}

		try
		{
			Type skippedTrackSetType = new TypeToken<Set<String>>()
			{
			}.getType();
			Set<String> loadedSkippedTrackTitles = gson.fromJson(persistedSkippedTracksJson, skippedTrackSetType);
			skippedTrackTitles.clear();
			if (loadedSkippedTrackTitles != null)
			{
				skippedTrackTitles.addAll(loadedSkippedTrackTitles);
			}
		}
		catch (Exception exception)
		{
			log.warn("Failed to load skipped tracks", exception);
		}
	}

	private void persistSkippedTracksIfEnabled()
	{
		if (!musicTrackerConfig.saveSkippedTracks())
		{
			return;
		}
		try
		{
			configManager.setConfiguration(CONFIG_GROUP_NAME, SKIPPED_TRACKS_CONFIG_KEY, gson.toJson(skippedTrackTitles));
		}
		catch (Exception exception)
		{
			log.warn("Failed to save skipped tracks", exception);
		}
	}
}