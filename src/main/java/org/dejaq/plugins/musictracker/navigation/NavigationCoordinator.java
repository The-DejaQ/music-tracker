package org.dejaq.plugins.musictracker.navigation;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerConfig;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;

@Slf4j
@Singleton
public class NavigationCoordinator
{
	private static final String SHORTEST_PATH_PLUGIN_NAMESPACE = "shortestpath";
	private static final String SHORTEST_PATH_MESSAGE_NAME = "path";
	private static final String SHORTEST_PATH_TARGET_DATA_KEY = "target";
	private static final String WORLD_MAP_POINT_ICON_RESOURCE_NAME = "icon_cape.png";

	private static final BufferedImage WORLD_MAP_POINT_ICON = ImageUtil.loadImageResource(
		MusicTrackerPlugin.class, WORLD_MAP_POINT_ICON_RESOURCE_NAME);

	@Inject
	private ClientThread clientThread;
	@Inject
	private EventBus eventBus;
	@Inject
	private WorldMapPointManager worldMapPointManager;
	@Inject
	private MusicTrackerConfig musicTrackerConfig;

	private final List<WorldMapPoint> activeWorldMapPoints = new ArrayList<>();
	private WorldPoint lastRequestedTargetWorldPoint;

	public void onNavigationTargetChanged(MusicTrack currentMusicTrack, WorldPoint currentTargetWorldPoint)
	{
		if (currentTargetWorldPoint != null)
		{
			requestShortestPathTo(currentTargetWorldPoint);
		}
		if (currentMusicTrack != null)
		{
			updateWorldMapPointForTrack(currentMusicTrack, currentTargetWorldPoint);
		}
	}

	public void requestShortestPathTo(WorldPoint targetWorldPoint)
	{
		if (targetWorldPoint == null || (targetWorldPoint.getX() == 0 && targetWorldPoint.getY() == 0))
		{
			return;
		}
		if (!musicTrackerConfig.useShortestPath())
		{
			return;
		}
		if (targetWorldPoint.equals(lastRequestedTargetWorldPoint))
		{
			return;
		}
		lastRequestedTargetWorldPoint = targetWorldPoint;

		clientThread.invokeLater(() -> {
			Map<String, Object> shortestPathRequestData = new HashMap<>();
			shortestPathRequestData.put(SHORTEST_PATH_TARGET_DATA_KEY, targetWorldPoint);
			eventBus.post(new PluginMessage(SHORTEST_PATH_PLUGIN_NAMESPACE, SHORTEST_PATH_MESSAGE_NAME, shortestPathRequestData));
		});
	}

	public void updateWorldMapPointForTrack(MusicTrack musicTrack, WorldPoint preferredTargetWorldPoint)
	{
		if (!musicTrackerConfig.showWorldMapPoints())
		{
			return;
		}

		clearWorldMapPoints();

		WorldPoint worldMapPointLocation = preferredTargetWorldPoint != null ? preferredTargetWorldPoint : musicTrack.getUnlockPoint();
		if (worldMapPointLocation == null || (worldMapPointLocation.getX() == 0 && worldMapPointLocation.getY() == 0))
		{
			log.debug("No unlock point found for track {}", musicTrack.getTitle());
			return;
		}

		WorldMapPoint worldMapPoint = new WorldMapPoint(worldMapPointLocation, WORLD_MAP_POINT_ICON);
		worldMapPoint.setName(musicTrack.getTitle());
		worldMapPoint.setTooltip(musicTrack.getTitle());
		worldMapPoint.setJumpOnClick(true);
		worldMapPoint.setSnapToEdge(true);

		worldMapPointManager.add(worldMapPoint);
		activeWorldMapPoints.add(worldMapPoint);

		log.debug("Added world map point at {} for track {}", worldMapPointLocation, musicTrack.getTitle());
	}

	public void clearWorldMapPoints()
	{
		for (WorldMapPoint worldMapPoint : activeWorldMapPoints)
		{
			worldMapPointManager.remove(worldMapPoint);
		}
		activeWorldMapPoints.clear();
	}

	public void forgetLastRequestedTarget()
	{
		lastRequestedTargetWorldPoint = null;
	}
}