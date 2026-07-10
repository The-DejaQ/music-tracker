package org.dejaq.plugins.musictracker.entity;

import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.MusicTrackEntityPoint;

@Singleton
public class SceneEntityFinder
{
	private static final int MAXIMUM_OBJECT_SEARCH_RADIUS_IN_TILES = 2;
	private static final int MAXIMUM_NPC_SEARCH_DISTANCE_IN_TILES = 8;

	@Inject
	private Client client;

	public Optional<NPC> findNpc(MusicTrackEntityPoint entityHighlightPoint)
	{
		WorldPoint targetWorldLocation = new WorldPoint(entityHighlightPoint.getWorldX(), entityHighlightPoint.getWorldY(), entityHighlightPoint.getPlane());
		InteractionTarget interactionTarget = entityHighlightPoint.getInteractionTarget();
		List<String> requiredActions = getRequiredActions(interactionTarget);
		int npcSearchDistanceInTiles = resolveNpcSearchDistanceInTiles(interactionTarget);

		if (entityHighlightPoint.getEntity() != null)
		{
			return findNpcByName(entityHighlightPoint.getEntity(), targetWorldLocation, requiredActions, npcSearchDistanceInTiles);
		}
		if (entityHighlightPoint.getEntityId() > 0)
		{
			return findNpcById(entityHighlightPoint.getEntityId(), targetWorldLocation, requiredActions, npcSearchDistanceInTiles);
		}
		return Optional.empty();
	}

	public Optional<TileObject> findGameObject(MusicTrackEntityPoint entityHighlightPoint)
	{
		WorldPoint targetWorldLocation = new WorldPoint(entityHighlightPoint.getWorldX(), entityHighlightPoint.getWorldY(), entityHighlightPoint.getPlane());
		InteractionTarget interactionTarget = entityHighlightPoint.getInteractionTarget();
		List<String> requiredActions = getRequiredActions(interactionTarget);
		int objectSearchRadiusInTiles = resolveObjectSearchRadiusInTiles(interactionTarget);

		if (entityHighlightPoint.getEntityId() > 0)
		{
			Optional<TileObject> matchingObjectById = findGameObjectById(targetWorldLocation, entityHighlightPoint.getEntityId());
			if (matchingObjectById.isPresent())
			{
				return filterByRequiredActions(matchingObjectById, requiredActions);
			}
		}
		if (entityHighlightPoint.getEntity() != null)
		{
			return filterByRequiredActions(findGameObjectByName(targetWorldLocation, entityHighlightPoint.getEntity(), objectSearchRadiusInTiles), requiredActions);
		}
		return Optional.empty();
	}

	private int resolveObjectSearchRadiusInTiles(InteractionTarget interactionTarget)
	{
		if (interactionTarget != null && interactionTarget.hasCustomSearchRadius())
		{
			return interactionTarget.getSearchRadius();
		}
		return MAXIMUM_OBJECT_SEARCH_RADIUS_IN_TILES;
	}

	private int resolveNpcSearchDistanceInTiles(InteractionTarget interactionTarget)
	{
		if (interactionTarget != null && interactionTarget.hasCustomSearchRadius())
		{
			return interactionTarget.getSearchRadius();
		}
		return MAXIMUM_NPC_SEARCH_DISTANCE_IN_TILES;
	}

	private List<String> getRequiredActions(InteractionTarget interactionTarget)
	{
		if (interactionTarget == null || interactionTarget.getActions() == null)
		{
			return List.of();
		}
		return interactionTarget.getActions();
	}

	private Optional<NPC> findNpcByName(String targetEntityName, WorldPoint targetWorldLocation, List<String> requiredActions, int npcSearchDistanceInTiles)
	{
		for (NPC candidateNpc : client.getLocalPlayer().getWorldView().npcs())
		{
			if (candidateNpc == null)
			{
				continue;
			}

			String candidateNpcName = resolveNpcName(candidateNpc);
			if (candidateNpcName == null)
			{
				continue;
			}

			boolean nameMatches = candidateNpcName.equalsIgnoreCase(targetEntityName)
				|| candidateNpcName.toLowerCase().contains(targetEntityName.toLowerCase());
			if (!nameMatches)
			{
				continue;
			}

			if (!isWithinSearchDistance(candidateNpc.getWorldLocation(), targetWorldLocation, npcSearchDistanceInTiles))
			{
				continue;
			}

			if (!npcSupportsAnyRequiredAction(candidateNpc, requiredActions))
			{
				continue;
			}

			return Optional.of(candidateNpc);
		}
		return Optional.empty();
	}

	private Optional<NPC> findNpcById(int targetNpcId, WorldPoint targetWorldLocation, List<String> requiredActions, int npcSearchDistanceInTiles)
	{
		for (NPC candidateNpc : client.getLocalPlayer().getWorldView().npcs())
		{
			if (candidateNpc == null || candidateNpc.getId() != targetNpcId)
			{
				continue;
			}

			if (!isWithinSearchDistance(candidateNpc.getWorldLocation(), targetWorldLocation, npcSearchDistanceInTiles))
			{
				continue;
			}

			if (!npcSupportsAnyRequiredAction(candidateNpc, requiredActions))
			{
				continue;
			}

			return Optional.of(candidateNpc);
		}
		return Optional.empty();
	}

	private boolean isWithinSearchDistance(WorldPoint candidateLocation, WorldPoint targetLocation, int maximumDistanceInTiles)
	{
		if (candidateLocation == null || candidateLocation.getPlane() != targetLocation.getPlane())
		{
			return false;
		}
		return candidateLocation.distanceTo(targetLocation) <= maximumDistanceInTiles;
	}

	private String resolveNpcName(NPC npc)
	{
		if (npc.getName() != null)
		{
			return npc.getName();
		}
		if (npc.getComposition() != null && npc.getComposition().getName() != null)
		{
			return npc.getComposition().getName();
		}
		if (npc.getTransformedComposition() != null)
		{
			return npc.getTransformedComposition().getName();
		}
		return null;
	}

	private boolean npcSupportsAnyRequiredAction(NPC npc, List<String> requiredActions)
	{
		if (requiredActions.isEmpty())
		{
			return true;
		}

		NPCComposition npcComposition = npc.getTransformedComposition();
		if (npcComposition == null || npcComposition.getActions() == null)
		{
			return false;
		}

		for (String requiredAction : requiredActions)
		{
			for (String supportedAction : npcComposition.getActions())
			{
				if (supportedAction != null && requiredAction != null && supportedAction.equalsIgnoreCase(requiredAction))
				{
					return true;
				}
			}
		}
		return false;
	}

	private Optional<TileObject> findGameObjectById(WorldPoint targetWorldLocation, int targetObjectId)
	{
		if (client.getLocalPlayer().getWorldView().getPlane() != targetWorldLocation.getPlane())
		{
			return Optional.empty();
		}

		Tile targetTile = resolveTileAt(targetWorldLocation);
		if (targetTile == null)
		{
			return Optional.empty();
		}

		return findMatchingObjectOnTile(targetTile, candidateObjectId -> candidateObjectId == targetObjectId);
	}

	private Optional<TileObject> findGameObjectByName(WorldPoint targetWorldLocation, String targetEntityName, int objectSearchRadiusInTiles)
	{
		String lowerCaseTargetEntityName = targetEntityName.toLowerCase().trim();

		for (int xOffset = -objectSearchRadiusInTiles; xOffset <= objectSearchRadiusInTiles; xOffset++)
		{
			for (int yOffset = -objectSearchRadiusInTiles; yOffset <= objectSearchRadiusInTiles; yOffset++)
			{
				WorldPoint candidateWorldLocation = new WorldPoint(
					targetWorldLocation.getX() + xOffset,
					targetWorldLocation.getY() + yOffset,
					targetWorldLocation.getPlane());

				Tile candidateTile = resolveTileAt(candidateWorldLocation);
				if (candidateTile == null)
				{
					continue;
				}

				Optional<TileObject> matchingObject = findMatchingObjectOnTile(candidateTile,
					candidateObjectId -> objectNameMatches(candidateObjectId, lowerCaseTargetEntityName));

				if (matchingObject.isPresent())
				{
					return matchingObject;
				}
			}
		}
		return Optional.empty();
	}

	private Tile resolveTileAt(WorldPoint worldLocation)
	{
		LocalPoint localPoint = LocalPoint.fromWorld(client, worldLocation);
		if (localPoint == null)
		{
			return null;
		}
		return client.getScene().getTiles()[worldLocation.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];
	}

	private Optional<TileObject> findMatchingObjectOnTile(Tile tile, IntPredicate objectIdMatcher)
	{
		for (GameObject candidateGameObject : tile.getGameObjects())
		{
			if (candidateGameObject != null && objectIdMatcher.test(candidateGameObject.getId()))
			{
				return Optional.of(candidateGameObject);
			}
		}

		WallObject wallObject = tile.getWallObject();
		if (wallObject != null && objectIdMatcher.test(wallObject.getId()))
		{
			return Optional.of(wallObject);
		}

		DecorativeObject decorativeObject = tile.getDecorativeObject();
		if (decorativeObject != null && objectIdMatcher.test(decorativeObject.getId()))
		{
			return Optional.of(decorativeObject);
		}

		GroundObject groundObject = tile.getGroundObject();
		if (groundObject != null && objectIdMatcher.test(groundObject.getId()))
		{
			return Optional.of(groundObject);
		}

		return Optional.empty();
	}

	private boolean objectNameMatches(int objectId, String lowerCaseTargetEntityName)
	{
		if (objectId <= 0)
		{
			return false;
		}

		try
		{
			ObjectComposition objectComposition = client.getObjectDefinition(objectId);
			if (objectComposition == null)
			{
				return false;
			}

			if (objectComposition.getImpostorIds() != null && objectComposition.getImpostorIds().length > 0)
			{
				ObjectComposition impostorComposition = objectComposition.getImpostor();
				if (impostorComposition != null)
				{
					objectComposition = impostorComposition;
				}
			}

			String objectName = objectComposition.getName();
			if (objectName == null || objectName.equals("null") || objectName.isBlank())
			{
				return false;
			}

			return objectName.toLowerCase().contains(lowerCaseTargetEntityName);
		}
		catch (Exception exception)
		{
			return false;
		}
	}

	private Optional<TileObject> filterByRequiredActions(Optional<TileObject> candidateTileObject, List<String> requiredActions)
	{
		if (candidateTileObject.isEmpty() || requiredActions.isEmpty())
		{
			return candidateTileObject;
		}
		return candidateTileObject.filter(tileObject -> objectSupportsAnyRequiredAction(tileObject, requiredActions));
	}

	private boolean objectSupportsAnyRequiredAction(TileObject tileObject, List<String> requiredActions)
	{
		ObjectComposition objectComposition = getObjectComposition(tileObject);
		if (objectComposition == null)
		{
			return false;
		}

		if (objectComposition.getImpostorIds() != null && objectComposition.getImpostorIds().length > 0)
		{
			ObjectComposition impostorComposition = objectComposition.getImpostor();
			if (impostorComposition != null)
			{
				objectComposition = impostorComposition;
			}
		}

		String[] supportedActions = objectComposition.getActions();
		if (supportedActions == null)
		{
			return false;
		}

		for (String requiredAction : requiredActions)
		{
			for (String supportedAction : supportedActions)
			{
				if (supportedAction != null && requiredAction != null && supportedAction.equalsIgnoreCase(requiredAction))
				{
					return true;
				}
			}
		}
		return false;
	}

	private ObjectComposition getObjectComposition(TileObject tileObject)
	{
		if (tileObject instanceof GameObject)
		{
			return client.getObjectDefinition(((GameObject) tileObject).getId());
		}
		if (tileObject instanceof WallObject)
		{
			return client.getObjectDefinition(((WallObject) tileObject).getId());
		}
		if (tileObject instanceof DecorativeObject)
		{
			return client.getObjectDefinition(((DecorativeObject) tileObject).getId());
		}
		if (tileObject instanceof GroundObject)
		{
			return client.getObjectDefinition(((GroundObject) tileObject).getId());
		}
		return null;
	}
}