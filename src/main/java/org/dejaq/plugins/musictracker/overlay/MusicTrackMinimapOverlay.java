package org.dejaq.plugins.musictracker.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import org.dejaq.plugins.musictracker.MusicTrackerConfig;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;

public class MusicTrackMinimapOverlay extends Overlay
{

	private static final int ARROW_BREATHING_PERIOD_TICKS = 1;
	private static final int ARROW_LENGTH_PIXELS = 23;
	private static final int ARROW_HALF_WIDTH_PIXELS = 9;
	private static final int ARROW_BACK_NOTCH_DEPTH_PIXELS = 22;
	private static final int ARROW_TIP_BLUNT_INSET_PIXELS = 1;
	private static final int ARROW_TIP_BLUNT_HALF_WIDTH_PIXELS = 0;
	private static final int ARROW_EDGE_INSET_PIXELS = 4;
	private static final int DESTINATION_DOT_DIAMETER_PIXELS = 8;

	private final Client client;
	private final MusicTrackerPlugin musicTrackerPlugin;
	private final MusicTrackerConfig musicTrackerConfig;
	private final EventBus eventBus;

	private int gameTickCounter;

	@Inject
	public MusicTrackMinimapOverlay(Client client, MusicTrackerPlugin musicTrackerPlugin, MusicTrackerConfig musicTrackerConfig, EventBus eventBus)
	{
		this.client = client;
		this.musicTrackerPlugin = musicTrackerPlugin;
		this.musicTrackerConfig = musicTrackerConfig;
		this.eventBus = eventBus;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	public void startUp()
	{
		gameTickCounter = 0;
		eventBus.register(this);
	}

	public void shutDown()
	{
		eventBus.unregister(this);
		gameTickCounter = 0;
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		gameTickCounter++;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!musicTrackerPlugin.isTrackingActive() || !musicTrackerConfig.showMinimapArrow() || !isArrowVisibleThisTick())
		{
			return null;
		}

		WorldPoint targetWorldPoint = musicTrackerPlugin.getTrackNavigator().getCurrentTargetPoint();
		if (targetWorldPoint == null || (targetWorldPoint.getX() == 0 && targetWorldPoint.getY() == 0))
		{
			return null;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null || targetWorldPoint.getPlane() != client.getLocalPlayer().getWorldView().getPlane())
		{
			return null;
		}

		WorldPoint playerWorldPoint = localPlayer.getWorldLocation();
		int totalDistanceTiles = playerWorldPoint.distanceTo(targetWorldPoint);
		if (totalDistanceTiles <= 0 || totalDistanceTiles == Integer.MAX_VALUE)
		{
			return null;
		}

		MinimapProbeResult probeResult = findFarthestVisiblePointTowardTarget(playerWorldPoint, targetWorldPoint, totalDistanceTiles);
		if (probeResult == null)
		{
			return null;
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (probeResult.reachedActualDestination)
		{
			drawDestinationDot(graphics, probeResult.minimapPoint);
			return null;
		}

		drawEdgeArrow(graphics, playerWorldPoint, probeResult.minimapPoint);
		return null;
	}

	private boolean isArrowVisibleThisTick()
	{
		if (musicTrackerConfig.minimapArrowStatic())
		{
			return true;
		}
		return (gameTickCounter / ARROW_BREATHING_PERIOD_TICKS) % 2 == 0;
	}

	private void drawDestinationDot(Graphics2D graphics, Point minimapPoint)
	{
		int outlineDiameter = DESTINATION_DOT_DIAMETER_PIXELS + 2;
		graphics.setColor(Color.BLACK);
		graphics.fillOval(minimapPoint.getX() - outlineDiameter / 2, minimapPoint.getY() - outlineDiameter / 2, outlineDiameter, outlineDiameter);

		graphics.setColor(musicTrackerConfig.minimapArrowColor());
		graphics.fillOval(minimapPoint.getX() - DESTINATION_DOT_DIAMETER_PIXELS / 2, minimapPoint.getY() - DESTINATION_DOT_DIAMETER_PIXELS / 2,
			DESTINATION_DOT_DIAMETER_PIXELS, DESTINATION_DOT_DIAMETER_PIXELS);
	}

	private void drawEdgeArrow(Graphics2D graphics, WorldPoint playerWorldPoint, Point edgeMinimapPoint)
	{
		LocalPoint playerLocalPoint = LocalPoint.fromWorld(client, playerWorldPoint);

		Point playerMinimapPoint = playerLocalPoint != null ? Perspective.localToMinimap(client, playerLocalPoint) : null;
		if (playerMinimapPoint == null)
		{
			return;
		}

		double directionX = edgeMinimapPoint.getX() - playerMinimapPoint.getX();
		double directionY = edgeMinimapPoint.getY() - playerMinimapPoint.getY();
		double directionMagnitude = Math.hypot(directionX, directionY);
		if (directionMagnitude < 1.0)
		{
			return;
		}

		double unitDirectionX = directionX / directionMagnitude;
		double unitDirectionY = directionY / directionMagnitude;
		int insetTipX = (int) Math.round(edgeMinimapPoint.getX() - unitDirectionX * ARROW_EDGE_INSET_PIXELS);
		int insetTipY = (int) Math.round(edgeMinimapPoint.getY() - unitDirectionY * ARROW_EDGE_INSET_PIXELS);

		double arrowAngleRadians = Math.atan2(directionY, directionX);
		drawArrowhead(graphics, insetTipX, insetTipY, arrowAngleRadians);
	}

	private MinimapProbeResult findFarthestVisiblePointTowardTarget(WorldPoint playerWorldPoint, WorldPoint targetWorldPoint, int totalDistanceTiles)
	{
		int lowTileDistance = 0;
		int highTileDistance = totalDistanceTiles;
		Point farthestVisiblePoint = null;
		int farthestVisibleTileDistance = 0;

		while (lowTileDistance <= highTileDistance)
		{
			int midTileDistance = (lowTileDistance + highTileDistance) / 2;
			WorldPoint probeWorldPoint = interpolateWorldPoint(playerWorldPoint, targetWorldPoint, midTileDistance, totalDistanceTiles);
			LocalPoint probeLocalPoint = LocalPoint.fromWorld(client, probeWorldPoint);
			Point probeMinimapPoint = probeLocalPoint != null ? Perspective.localToMinimap(client, probeLocalPoint) : null;

			if (probeMinimapPoint != null)
			{
				farthestVisiblePoint = probeMinimapPoint;
				farthestVisibleTileDistance = midTileDistance;
				lowTileDistance = midTileDistance + 1;
			}
			else
			{
				highTileDistance = midTileDistance - 1;
			}
		}

		if (farthestVisiblePoint == null)
		{
			return null;
		}

		return new MinimapProbeResult(farthestVisiblePoint, farthestVisibleTileDistance == totalDistanceTiles);
	}

	private WorldPoint interpolateWorldPoint(WorldPoint fromPoint, WorldPoint toPoint, int stepDistance, int totalDistance)
	{
		double interpolationRatio = (double) stepDistance / totalDistance;
		int interpolatedX = fromPoint.getX() + (int) Math.round((toPoint.getX() - fromPoint.getX()) * interpolationRatio);
		int interpolatedY = fromPoint.getY() + (int) Math.round((toPoint.getY() - fromPoint.getY()) * interpolationRatio);
		return new WorldPoint(interpolatedX, interpolatedY, fromPoint.getPlane());
	}

	private void drawArrowhead(Graphics2D graphics, int tipX, int tipY, double angleRadians)
	{
		Polygon arrowPolygon = new Polygon();
		arrowPolygon.addPoint(-ARROW_TIP_BLUNT_INSET_PIXELS, -ARROW_TIP_BLUNT_HALF_WIDTH_PIXELS);
		arrowPolygon.addPoint(-ARROW_LENGTH_PIXELS, -ARROW_HALF_WIDTH_PIXELS);
		arrowPolygon.addPoint(-ARROW_BACK_NOTCH_DEPTH_PIXELS, 0);
		arrowPolygon.addPoint(-ARROW_LENGTH_PIXELS, ARROW_HALF_WIDTH_PIXELS);
		arrowPolygon.addPoint(-ARROW_TIP_BLUNT_INSET_PIXELS, ARROW_TIP_BLUNT_HALF_WIDTH_PIXELS);

		Graphics2D arrowGraphics = (Graphics2D) graphics.create();
		arrowGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		arrowGraphics.translate(tipX, tipY);
		arrowGraphics.rotate(angleRadians);

		arrowGraphics.setColor(musicTrackerConfig.minimapArrowColor());
		arrowGraphics.fillPolygon(arrowPolygon);

		arrowGraphics.setColor(Color.BLACK);
		arrowGraphics.setStroke(new BasicStroke(2f));
		arrowGraphics.drawPolygon(arrowPolygon);

		arrowGraphics.dispose();
	}

	private static final class MinimapProbeResult
	{
		private final Point minimapPoint;
		private final boolean reachedActualDestination;

		private MinimapProbeResult(Point minimapPoint, boolean reachedActualDestination)
		{
			this.minimapPoint = minimapPoint;
			this.reachedActualDestination = reachedActualDestination;
		}
	}
}