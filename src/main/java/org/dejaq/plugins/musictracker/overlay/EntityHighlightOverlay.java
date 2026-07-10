package org.dejaq.plugins.musictracker.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import org.dejaq.plugins.musictracker.MusicTrackerConfig;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.entity.SceneEntityFinder;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.InteractionType;
import org.dejaq.plugins.musictracker.track.MusicTrackEntityPoint;

public class EntityHighlightOverlay extends Overlay
{
	private static final int HIGHLIGHT_OUTLINE_STROKE_WIDTH = 2;
	private static final int HINT_TEXT_VERTICAL_OFFSET = 8;

	private final Client client;
	private final MusicTrackerPlugin musicTrackerPlugin;
	private final MusicTrackerConfig musicTrackerConfig;
	private final SceneEntityFinder sceneEntityFinder;

	private final Map<MusicTrackEntityPoint, NPC> resolvedNpcsByEntityPoint = new IdentityHashMap<>();
	private final Map<MusicTrackEntityPoint, TileObject> resolvedGameObjectsByEntityPoint = new IdentityHashMap<>();
	private List<MusicTrackEntityPoint> lastSeenHighlightList;
	private int lastResolvedAtTick = -1;

	@Inject
	public EntityHighlightOverlay(Client client, MusicTrackerPlugin musicTrackerPlugin, MusicTrackerConfig musicTrackerConfig, SceneEntityFinder sceneEntityFinder)
	{
		this.client = client;
		this.musicTrackerPlugin = musicTrackerPlugin;
		this.musicTrackerConfig = musicTrackerConfig;
		this.sceneEntityFinder = sceneEntityFinder;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<MusicTrackEntityPoint> activeEntityHighlights = musicTrackerPlugin.getTrackNavigator().getActiveEntityHighlights();
		if (activeEntityHighlights == null || activeEntityHighlights.isEmpty())
		{
			lastSeenHighlightList = null;
			resolvedNpcsByEntityPoint.clear();
			resolvedGameObjectsByEntityPoint.clear();
			return null;
		}

		refreshResolvedEntitiesIfNeeded(activeEntityHighlights);

		for (MusicTrackEntityPoint entityHighlightPoint : activeEntityHighlights)
		{
			if (entityHighlightPoint == null)
			{
				continue;
			}
			renderEntityHighlight(graphics, entityHighlightPoint);
		}

		return null;
	}

	private void refreshResolvedEntitiesIfNeeded(List<MusicTrackEntityPoint> activeEntityHighlights)
	{
		int currentTick = client.getTickCount();
		if (activeEntityHighlights == lastSeenHighlightList && currentTick == lastResolvedAtTick)
		{
			return;
		}

		resolvedNpcsByEntityPoint.clear();
		resolvedGameObjectsByEntityPoint.clear();

		for (MusicTrackEntityPoint entityHighlightPoint : activeEntityHighlights)
		{
			if (entityHighlightPoint == null || entityHighlightPoint.getInteractionType() == InteractionType.TILE)
			{
				continue;
			}

			if (entityHighlightPoint.getInteractionType() == InteractionType.NPC)
			{
				sceneEntityFinder.findNpc(entityHighlightPoint)
					.ifPresent(resolvedNpc -> resolvedNpcsByEntityPoint.put(entityHighlightPoint, resolvedNpc));
			}
			else
			{
				sceneEntityFinder.findGameObject(entityHighlightPoint)
					.ifPresent(resolvedGameObject -> resolvedGameObjectsByEntityPoint.put(entityHighlightPoint, resolvedGameObject));
			}
		}

		lastSeenHighlightList = activeEntityHighlights;
		lastResolvedAtTick = currentTick;
	}

	private void renderEntityHighlight(Graphics2D graphics, MusicTrackEntityPoint entityHighlightPoint)
	{
		Shape highlightShape = resolveHighlightShape(entityHighlightPoint);
		if (highlightShape == null)
		{
			return;
		}

		Color highlightFillColor = musicTrackerConfig.entityHighlightFillColor();
		Color highlightOutlineColor = musicTrackerConfig.entityHighlightOutlineColor();

		graphics.setColor(highlightFillColor);
		graphics.fill(highlightShape);

		graphics.setColor(highlightOutlineColor);
		graphics.setStroke(new BasicStroke(HIGHLIGHT_OUTLINE_STROKE_WIDTH));
		graphics.draw(highlightShape);

		InteractionTarget interactionTarget = entityHighlightPoint.getInteractionTarget();
		if (interactionTarget != null && interactionTarget.getHint() != null && !interactionTarget.getHint().isBlank())
		{
			Point hintTextPosition = getTextPositionAboveShape(highlightShape, graphics, interactionTarget.getHint());
			OverlayUtil.renderTextLocation(graphics, hintTextPosition, interactionTarget.getHint(), highlightOutlineColor);
		}
	}

	private Shape resolveHighlightShape(MusicTrackEntityPoint entityHighlightPoint)
	{
		if (entityHighlightPoint.getInteractionType() == InteractionType.TILE)
		{
			return resolveTileHighlightShape(entityHighlightPoint);
		}
		if (entityHighlightPoint.getInteractionType() == InteractionType.NPC)
		{
			NPC resolvedNpc = resolvedNpcsByEntityPoint.get(entityHighlightPoint);
			return resolvedNpc != null ? resolvedNpc.getConvexHull() : null;
		}
		TileObject resolvedGameObject = resolvedGameObjectsByEntityPoint.get(entityHighlightPoint);
		return resolvedGameObject != null ? getObjectConvexHull(resolvedGameObject) : null;
	}

	private Shape resolveTileHighlightShape(MusicTrackEntityPoint entityHighlightPoint)
	{
		WorldPoint tileWorldLocation = new WorldPoint(entityHighlightPoint.getWorldX(), entityHighlightPoint.getWorldY(), entityHighlightPoint.getPlane());
		LocalPoint tileLocalPoint = LocalPoint.fromWorld(client, tileWorldLocation);
		if (tileLocalPoint == null)
		{
			return null;
		}
		return Perspective.getCanvasTilePoly(client, tileLocalPoint);
	}

	private Shape getObjectConvexHull(TileObject tileObject)
	{
		if (tileObject == null) {
			return null;
		}
		if (tileObject instanceof GameObject)
		{
			return ((GameObject) tileObject).getConvexHull();
		}
		if (tileObject instanceof WallObject)
		{
			return ((WallObject) tileObject).getConvexHull();
		}
		if (tileObject instanceof DecorativeObject)
		{
			return ((DecorativeObject) tileObject).getConvexHull();
		}
		if (tileObject instanceof GroundObject)
		{
			return ((GroundObject) tileObject).getConvexHull();
		}
		return null;
	}

	private Point getTextPositionAboveShape(Shape highlightShape, Graphics2D graphics, String hintText)
	{
		Rectangle shapeBounds = highlightShape.getBounds();
		FontMetrics fontMetrics = graphics.getFontMetrics();

		int hintTextWidth = fontMetrics.stringWidth(hintText);
		int hintTextX = shapeBounds.x + (shapeBounds.width / 2) - (hintTextWidth / 2);
		int hintTextY = shapeBounds.y - HINT_TEXT_VERTICAL_OFFSET;

		return new Point(hintTextX, hintTextY);
	}
}