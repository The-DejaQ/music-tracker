package org.dejaq.plugins.musictracker.overlay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackManager;
import org.dejaq.plugins.musictracker.MusicTrackerConfig;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.state.TrackingStateService;
import org.dejaq.plugins.musictracker.track.UnlockType;

@Slf4j
@Singleton
public class MusicTrackWorldMapOverlay
{
	private static final String HARP_UNLOCKED_ICON_RESOURCE_NAME = "harp_unlocked.png";
	private static final String HARP_LOCKED_ICON_RESOURCE_NAME = "harp_locked.png";
	private static final int ICON_SIZE_PIXELS = 20;
	private static final int CLICK_RADIUS_PIXELS = 10;
	private static final int MAX_TOOLTIP_TITLE_COUNT = 4;

	private final MusicTrackManager musicTrackManager;
	private final MusicTrackerConfig musicTrackerConfig;
	private final TrackingStateService trackingStateService;
	private final WorldMapPointManager worldMapPointManager;
	private final WorldMapOverlay worldMapOverlay;
	private final MouseManager mouseManager;

	private final List<WorldMapIconGroup> activeIconGroups = new ArrayList<>();

	private MusicTrackerPlugin musicTrackerPlugin;
	private BufferedImage unlockedHarpIcon;
	private BufferedImage lockedHarpIcon;

	private final MouseAdapter worldMapMouseListener = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent mouseEvent)
		{
			return handleWorldMapMousePressed(mouseEvent);
		}
	};

	@Inject
	public MusicTrackWorldMapOverlay(MusicTrackManager musicTrackManager, MusicTrackerConfig musicTrackerConfig,
									 TrackingStateService trackingStateService, WorldMapPointManager worldMapPointManager,
									 WorldMapOverlay worldMapOverlay, MouseManager mouseManager)
	{
		this.musicTrackManager = musicTrackManager;
		this.musicTrackerConfig = musicTrackerConfig;
		this.trackingStateService = trackingStateService;
		this.worldMapPointManager = worldMapPointManager;
		this.worldMapOverlay = worldMapOverlay;
		this.mouseManager = mouseManager;
	}

	public void startUp(MusicTrackerPlugin musicTrackerPlugin)
	{
		this.musicTrackerPlugin = musicTrackerPlugin;
		mouseManager.registerMouseListener(worldMapMouseListener);
		loadHarpIcons();
		rebuildWorldMapPoints();
	}

	public void shutDown()
	{
		mouseManager.unregisterMouseListener(worldMapMouseListener);
		clearWorldMapPoints();
		this.musicTrackerPlugin = null;
	}

	public synchronized void rebuildWorldMapPoints()
	{
		clearWorldMapPoints();

		if (!musicTrackerConfig.showWorldMapOverlay())
		{
			return;
		}

		Map<WorldPoint, List<MusicTrack>> tracksByUnlockLocation = groupTracksByUnlockLocation();
		for (Map.Entry<WorldPoint, List<MusicTrack>> locationEntry : tracksByUnlockLocation.entrySet())
		{
			WorldPoint unlockLocation = locationEntry.getKey();
			List<MusicTrack> tracksAtLocation = locationEntry.getValue();

			WorldMapPoint worldMapPoint = createWorldMapPointForLocation(unlockLocation, tracksAtLocation);
			worldMapPointManager.add(worldMapPoint);
			activeIconGroups.add(new WorldMapIconGroup(unlockLocation, tracksAtLocation, worldMapPoint));
		}

		log.debug("Built {} music track world map icon group(s)", activeIconGroups.size());
	}

	public synchronized void clearWorldMapPoints()
	{
		for (WorldMapIconGroup iconGroup : activeIconGroups)
		{
			worldMapPointManager.remove(iconGroup.worldMapPoint);
		}
		activeIconGroups.clear();
	}

	private Map<WorldPoint, List<MusicTrack>> groupTracksByUnlockLocation()
	{
		Map<WorldPoint, List<MusicTrack>> tracksByUnlockLocation = new LinkedHashMap<>();

		for (MusicTrack musicTrack : musicTrackManager.getAllTracks())
		{
			UnlockType effectiveUnlockType = musicTrack.getUnlockType() != null ? musicTrack.getUnlockType() : UnlockType.NORMAL;
			if (effectiveUnlockType != UnlockType.NORMAL)
			{
				continue;
			}

			WorldPoint unlockLocation = musicTrack.getFirstStepDestination();
			if (unlockLocation == null || (unlockLocation.getX() == 0 && unlockLocation.getY() == 0))
			{
				continue;
			}

			tracksByUnlockLocation.computeIfAbsent(unlockLocation, key -> new ArrayList<>()).add(musicTrack);
		}

		return tracksByUnlockLocation;
	}

	private WorldMapPoint createWorldMapPointForLocation(WorldPoint unlockLocation, List<MusicTrack> tracksAtLocation)
	{
		boolean allTracksUnlocked = tracksAtLocation.stream()
			.allMatch(musicTrack -> trackingStateService.isTrackUnlocked(musicTrack.getTitle()));

		WorldMapPoint worldMapPoint = new WorldMapPoint(unlockLocation, allTracksUnlocked ? unlockedHarpIcon : lockedHarpIcon);
		worldMapPoint.setName("Music Tracker");
		worldMapPoint.setTooltip(buildTooltipText(tracksAtLocation));
		worldMapPoint.setJumpOnClick(false);
		worldMapPoint.setSnapToEdge(false);
		return worldMapPoint;
	}

	private String buildTooltipText(List<MusicTrack> tracksAtLocation)
	{
		if (tracksAtLocation.size() == 1)
		{
			MusicTrack onlyTrack = tracksAtLocation.get(0);
			String lockStateText = trackingStateService.isTrackUnlocked(onlyTrack.getTitle()) ? "Unlocked" : "Locked";
			return "Music Track: " + onlyTrack.getTitle() + " (" + lockStateText + ")";
		}

		List<MusicTrack> lockedFirstTracks = new ArrayList<>(tracksAtLocation);
		lockedFirstTracks.sort(Comparator.comparing(musicTrack -> trackingStateService.isTrackUnlocked(musicTrack.getTitle())));

		StringBuilder tooltipBuilder = new StringBuilder("Music Tracks:");
		int titleCountToShow = Math.min(MAX_TOOLTIP_TITLE_COUNT, lockedFirstTracks.size());
		for (int trackIndex = 0; trackIndex < titleCountToShow; trackIndex++)
		{
			tooltipBuilder.append("<br>").append(lockedFirstTracks.get(trackIndex).getTitle());
		}

		int remainingTrackCount = lockedFirstTracks.size() - titleCountToShow;
		if (remainingTrackCount > 0)
		{
			tooltipBuilder.append("<br>... and ").append(remainingTrackCount).append(" more");
		}

		return tooltipBuilder.toString();
	}

	private MouseEvent handleWorldMapMousePressed(MouseEvent mouseEvent)
	{
		if (mouseEvent.isConsumed() || !SwingUtilities.isLeftMouseButton(mouseEvent))
		{
			return mouseEvent;
		}

		if (musicTrackerPlugin == null || !musicTrackerConfig.showWorldMapOverlay() || !musicTrackerConfig.worldMapClickToTrack())
		{
			return mouseEvent;
		}

		WorldMapIconGroup clickedIconGroup = findIconGroupNearScreenPoint(mouseEvent.getX(), mouseEvent.getY());
		if (clickedIconGroup == null || clickedIconGroup.tracksAtLocation.isEmpty())
		{
			return mouseEvent;
		}

		MusicTrack firstTrackAtLocation = clickedIconGroup.tracksAtLocation.get(0);
		mouseEvent.consume();
		musicTrackerPlugin.selectTrack(firstTrackAtLocation);

		return mouseEvent;
	}

	private synchronized WorldMapIconGroup findIconGroupNearScreenPoint(int clickScreenX, int clickScreenY)
	{
		for (WorldMapIconGroup iconGroup : activeIconGroups)
		{
			Point iconScreenPoint = worldMapOverlay.mapWorldPointToGraphicsPoint(iconGroup.unlockLocation);
			if (iconScreenPoint == null)
			{
				continue;
			}

			double distanceToClick = Math.hypot(clickScreenX - iconScreenPoint.getX(), clickScreenY - iconScreenPoint.getY());
			if (distanceToClick <= CLICK_RADIUS_PIXELS)
			{
				return iconGroup;
			}
		}
		return null;
	}

	private void loadHarpIcons()
	{
		unlockedHarpIcon = loadHarpIcon(HARP_UNLOCKED_ICON_RESOURCE_NAME, true);
		lockedHarpIcon = loadHarpIcon(HARP_LOCKED_ICON_RESOURCE_NAME, false);
	}

	private BufferedImage loadHarpIcon(String resourceName, boolean unlockedVariant)
	{
		BufferedImage loadedHarpIcon = ImageUtil.loadImageResource(MusicTrackerPlugin.class, resourceName);
		if (loadedHarpIcon != null)
		{
			return ImageUtil.resizeImage(loadedHarpIcon, ICON_SIZE_PIXELS, ICON_SIZE_PIXELS);
		}

		log.warn("Could not load world map icon resource '{}', using a generated fallback icon", resourceName);
		return buildFallbackHarpIcon(unlockedVariant);
	}

	private BufferedImage buildFallbackHarpIcon(boolean unlockedVariant)
	{
		BufferedImage fallbackIcon = new BufferedImage(ICON_SIZE_PIXELS, ICON_SIZE_PIXELS, BufferedImage.TYPE_INT_ARGB);
		Graphics2D fallbackGraphics = fallbackIcon.createGraphics();
		fallbackGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		fallbackGraphics.setColor(unlockedVariant ? new Color(40, 170, 60) : new Color(210, 40, 40));
		fallbackGraphics.fillOval(1, 1, ICON_SIZE_PIXELS - 2, ICON_SIZE_PIXELS - 2);

		fallbackGraphics.setColor(Color.WHITE);
		fallbackGraphics.drawLine(ICON_SIZE_PIXELS / 3, ICON_SIZE_PIXELS / 2, ICON_SIZE_PIXELS - ICON_SIZE_PIXELS / 3, ICON_SIZE_PIXELS / 2);

		fallbackGraphics.dispose();
		return fallbackIcon;
	}

	private static final class WorldMapIconGroup
	{
		private final WorldPoint unlockLocation;
		private final List<MusicTrack> tracksAtLocation;
		private final WorldMapPoint worldMapPoint;

		private WorldMapIconGroup(WorldPoint unlockLocation, List<MusicTrack> tracksAtLocation, WorldMapPoint worldMapPoint)
		{
			this.unlockLocation = unlockLocation;
			this.tracksAtLocation = tracksAtLocation;
			this.worldMapPoint = worldMapPoint;
		}
	}
}