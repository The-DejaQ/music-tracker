package org.dejaq.plugins.musictracker.track.special;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.ui.ColorScheme;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.requirement.DynamicRequirement;
import org.dejaq.plugins.musictracker.requirement.ItemRequirement;
import org.dejaq.plugins.musictracker.requirement.collections.ItemCollections;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.InteractionType;
import org.dejaq.plugins.musictracker.track.MusicTrackEntityPoint;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.TrackStep;

@Slf4j
public class AbyssTrackHandler implements SpecialTrackHandler
{
	public static final String AIR_ALTAR_TRACK = "Serene";
	public static final String BODY_ALTAR_TRACK = "Heart and Mind";
	public static final String CHAOS_ALTAR_TRACK = "Complication";
	public static final String COSMIC_ALTAR_TRACK = "Stratosphere"; // requires Lost City
	public static final String DEATH_ALTAR_TRACK = "La Mort"; // requires Mourning's End Part II
	public static final String EARTH_ALTAR_TRACK = "Down to Earth";
	public static final String FIRE_ALTAR_TRACK = "Quest";
	public static final String LAW_ALTAR_TRACK = "Righteousness";
	public static final String MIND_ALTAR_TRACK = "Miracle Dance";
	public static final String NATURE_ALTAR_TRACK = "Understanding";
	public static final String SOUL_ALTAR_TRACK = "Soul Fall"; // requires additional steps, we'll just leave default for now
	public static final String WATER_ALTAR_TRACK = "Zealot";

	private static final String AIR_RIFT_NAME = "air rift";
	private static final String MIND_RIFT_NAME = "mind rift";
	private static final String WATER_RIFT_NAME = "water rift";
	private static final String EARTH_RIFT_NAME = "earth rift";
	private static final String FIRE_RIFT_NAME = "fire rift";
	private static final String BODY_RIFT_NAME = "body rift";
	private static final String COSMIC_RIFT_NAME = "cosmic rift";
	private static final String CHAOS_RIFT_NAME = "chaos rift";
	private static final String NATURE_RIFT_NAME = "nature rift";
	private static final String LAW_RIFT_NAME = "law rift";
	private static final String DEATH_RIFT_NAME = "death rift";
	private static final String SOUL_RIFT_NAME = "soul rift";

	private static final String ABYSS_ROUTE_NAME = "Abyss";

	private static final String RIFT_EXPECTED_ACTION = null;

	private static final Map<String, String> TRACK_TITLE_TO_RIFT_NAME = new HashMap<>();

	static
	{
		TRACK_TITLE_TO_RIFT_NAME.put(AIR_ALTAR_TRACK.toLowerCase(Locale.ROOT), AIR_RIFT_NAME);
		TRACK_TITLE_TO_RIFT_NAME.put(MIND_ALTAR_TRACK.toLowerCase(Locale.ROOT), MIND_RIFT_NAME);
		TRACK_TITLE_TO_RIFT_NAME.put(WATER_ALTAR_TRACK.toLowerCase(Locale.ROOT), WATER_RIFT_NAME);
		TRACK_TITLE_TO_RIFT_NAME.put(EARTH_ALTAR_TRACK.toLowerCase(Locale.ROOT), EARTH_RIFT_NAME);
		TRACK_TITLE_TO_RIFT_NAME.put(FIRE_ALTAR_TRACK.toLowerCase(Locale.ROOT), FIRE_RIFT_NAME);
		TRACK_TITLE_TO_RIFT_NAME.put(BODY_ALTAR_TRACK.toLowerCase(Locale.ROOT), BODY_RIFT_NAME);
		TRACK_TITLE_TO_RIFT_NAME.put(COSMIC_ALTAR_TRACK.toLowerCase(Locale.ROOT), COSMIC_RIFT_NAME);
		TRACK_TITLE_TO_RIFT_NAME.put(CHAOS_ALTAR_TRACK.toLowerCase(Locale.ROOT), CHAOS_RIFT_NAME);
		TRACK_TITLE_TO_RIFT_NAME.put(NATURE_ALTAR_TRACK.toLowerCase(Locale.ROOT), NATURE_RIFT_NAME);
		TRACK_TITLE_TO_RIFT_NAME.put(LAW_ALTAR_TRACK.toLowerCase(Locale.ROOT), LAW_RIFT_NAME);
		TRACK_TITLE_TO_RIFT_NAME.put(DEATH_ALTAR_TRACK.toLowerCase(Locale.ROOT), DEATH_RIFT_NAME);
		TRACK_TITLE_TO_RIFT_NAME.put(SOUL_ALTAR_TRACK.toLowerCase(Locale.ROOT), SOUL_RIFT_NAME);
	}

	private static final WorldPoint ABYSS_INNER_RING_CENTER = new WorldPoint(3039, 4832, 0);
	private static final int ABYSS_RADIUS = 32;

	private static final int ABYSS_INNER_RING_RADIUS_TILES = 12;

	private enum AbyssStage
	{
		OBSTACLE,
		RIFT,
		OUTSIDE_ABYSS
	}

	private static final int OBSTACLE_SEARCH_RADIUS_TILES = 25;
	private static final int RIFT_SEARCH_RADIUS_TILES = 15;
	private static final int TOOL_AVAILABLE_SCORE_BONUS = 1000;
	private static final int TOOL_MISSING_SCORE_PENALTY = 1000;

	private static final List<AbyssObstacle> TOOL_GATED_OBSTACLES = List.of(AbyssObstacle.ROCK, AbyssObstacle.TENDRILS, AbyssObstacle.BOIL);

	@Getter
	@RequiredArgsConstructor
	private enum AbyssObstacle
	{
		PASSAGE("passage", "Passage", null, null, null),
		ROCK("rock", "Rock", Skill.MINING, new ItemRequirement(ItemCollections.PICKAXES, 1).setLabel("Any Pickaxe"), "Mine"),
		GAP("gap", "Gap", Skill.AGILITY, null, "Squeeze-through"),
		TENDRILS("tendril", "Tendrils", Skill.WOODCUTTING, new ItemRequirement(ItemCollections.AXES, 1).setLabel("Any Axe"), "Chop"),
		BOIL("boil", "Boil", Skill.FIREMAKING, new ItemRequirement(ItemID.TINDERBOX, 1), "Burn"),
		EYES("eye", "Eyes", Skill.THIEVING, null, "Distract");

		private final String objectNameFragment;
		private final String displayName;
		private final Skill relevantSkill;
		private final ItemRequirement requiredTool;
		private final String expectedAction;
	}

	@Getter
	@AllArgsConstructor
	private static final class ObstacleSighting
	{
		private final WorldPoint location;
		private final int distanceInTiles;
	}

	private int cachedTick = -1;
	private String cachedTrackTitle;
	private int cachedStageIndex = -1;
	private List<MusicTrackEntityPoint> cachedHighlights;
	private WorldPoint lastScannedPlayerLocation;

	@Override
	public List<DynamicRequirement<ItemRequirement>> getDynamicItemRecommendations(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		if (route != null && !route.isRoute(ABYSS_ROUTE_NAME))
		{
			return List.of();
		}
		List<AbyssObstacle> obstaclesRankedBest = new ArrayList<>(TOOL_GATED_OBSTACLES);
		obstaclesRankedBest.sort(Comparator.comparingInt((AbyssObstacle abyssObstacle) -> scoreObstacle(abyssObstacle, musicTrackerPlugin)).reversed());

		List<DynamicRequirement<ItemRequirement>> recommendations = new ArrayList<>();
		for (AbyssObstacle abyssObstacle : obstaclesRankedBest)
		{
			ItemRequirement requiredTool = abyssObstacle.getRequiredTool();
			boolean playerHasTool = musicTrackerPlugin.playerHasItem(requiredTool);
			String toolDisplayName = requiredTool.getEffectiveName(musicTrackerPlugin.getItemManager());
			String displayText = toolDisplayName + " (" + abyssObstacle.getDisplayName() + ")";
			Color lineColor = playerHasTool ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.PROGRESS_ERROR_COLOR;

			recommendations.add(DynamicRequirement.of(requiredTool, displayText, lineColor));
		}
		return recommendations;
	}

	private int scoreObstacle(AbyssObstacle abyssObstacle, MusicTrackerPlugin musicTrackerPlugin)
	{
		int score = 0;

		if (abyssObstacle.getRelevantSkill() != null)
		{
			score += musicTrackerPlugin.getClient().getRealSkillLevel(abyssObstacle.getRelevantSkill());
		}

		if (abyssObstacle.getRequiredTool() != null)
		{
			score += musicTrackerPlugin.playerHasItem(abyssObstacle.getRequiredTool()) ? TOOL_AVAILABLE_SCORE_BONUS : -TOOL_MISSING_SCORE_PENALTY;
		}

		return score;
	}

	@Override
	public boolean hasVolatileDynamicHighlights(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		if (route == null || !route.isRoute(ABYSS_ROUTE_NAME))
		{
			return false;
		}

		Player localPlayer = musicTrackerPlugin.getClient().getLocalPlayer();
		if (localPlayer == null)
		{
			return true;
		}

		return localPlayer.getWorldLocation().distanceTo(ABYSS_INNER_RING_CENTER) <= ABYSS_RADIUS;
	}

	@Override
	public List<MusicTrackEntityPoint> getDynamicEntityHighlights(MusicTrack musicTrack, Route route, TrackStep trackStep, int stageIndex, MusicTrackerPlugin musicTrackerPlugin)
	{
		if (route != null && !route.isRoute(ABYSS_ROUTE_NAME))
		{
			return null;
		}

		Client client = musicTrackerPlugin.getClient();
		if (!client.isClientThread())
		{
			log.warn("getDynamicEntityHighlights called off the client thread; ignoring");
			return null;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return null;
		}

		WorldPoint playerLocation = localPlayer.getWorldLocation();

		AbyssStage abyssStage = resolveAbyssStageByLocation(playerLocation);
		if (abyssStage == AbyssStage.OUTSIDE_ABYSS)
		{
			return null;
		}

		int currentTick = client.getTickCount();
		String trackTitle = musicTrack != null ? musicTrack.getTitle() : null;

		boolean playerMovedEnough = lastScannedPlayerLocation == null
			|| playerLocation.distanceTo(lastScannedPlayerLocation) >= 3;

		if (isCacheValid(currentTick, trackTitle, stageIndex) && !playerMovedEnough)
		{
			return cachedHighlights;
		}

		List<MusicTrackEntityPoint> computedHighlights = abyssStage == AbyssStage.OBSTACLE
			? computeObstacleHighlight(client, playerLocation, musicTrackerPlugin)
			: computeRiftHighlight(client, playerLocation, musicTrackerPlugin, musicTrack);

		cachedTick = currentTick;
		cachedTrackTitle = trackTitle;
		cachedStageIndex = stageIndex;
		cachedHighlights = computedHighlights;
		lastScannedPlayerLocation = playerLocation;

		return computedHighlights;
	}

	@Override
	public void reset()
	{
		cachedTick = -1;
		cachedTrackTitle = null;
		cachedStageIndex = -1;
		cachedHighlights = null;
		lastScannedPlayerLocation = null;
	}

	private boolean isCacheValid(int currentTick, String trackTitle, int stageIndex)
	{
		return cachedTick == currentTick
			&& cachedStageIndex == stageIndex
			&& Objects.equals(cachedTrackTitle, trackTitle);
	}

	private AbyssStage resolveAbyssStageByLocation(WorldPoint playerLocation)
	{
		if (playerLocation == null)
		{
			return AbyssStage.OUTSIDE_ABYSS;
		}

		int distanceToAbyssCentre = playerLocation.distanceTo(ABYSS_INNER_RING_CENTER);
		if (distanceToAbyssCentre > ABYSS_RADIUS)
		{
			return AbyssStage.OUTSIDE_ABYSS;
		}
		if (distanceToAbyssCentre <= ABYSS_INNER_RING_RADIUS_TILES)
		{
			return AbyssStage.RIFT;
		}
		return AbyssStage.OBSTACLE;
	}

	private List<MusicTrackEntityPoint> computeObstacleHighlight(Client client, WorldPoint playerLocation, MusicTrackerPlugin musicTrackerPlugin)
	{
		Map<AbyssObstacle, ObstacleSighting> sightings = scanForObstacles(client, playerLocation, OBSTACLE_SEARCH_RADIUS_TILES);
		if (sightings.isEmpty())
		{
			log.debug("Abyss: no obstacles sighted within {} tiles of the player", OBSTACLE_SEARCH_RADIUS_TILES);
			return null;
		}

		AbyssObstacle closestObstacle = findClosestObstacle(sightings);
		AbyssObstacle chosenObstacle = closestObstacle == AbyssObstacle.PASSAGE
			? AbyssObstacle.PASSAGE
			: chooseBestNonPassageObstacle(sightings, musicTrackerPlugin);

		if (chosenObstacle == null)
		{
			return null;
		}

		ObstacleSighting chosenSighting = sightings.get(chosenObstacle);
		if (chosenSighting == null)
		{
			return null;
		}

		musicTrackerPlugin.getTrackNavigator().getNavigationCoordinator().requestShortestPathTo(chosenSighting.getLocation());

		String hintText = chosenObstacle.getDisplayName() + " - Recommended";
		return List.of(buildHighlightPoint(chosenSighting.getLocation(), chosenObstacle.getObjectNameFragment(), hintText));
	}

	private AbyssObstacle findClosestObstacle(Map<AbyssObstacle, ObstacleSighting> sightings)
	{
		AbyssObstacle closestObstacle = null;
		int closestDistance = Integer.MAX_VALUE;

		for (Map.Entry<AbyssObstacle, ObstacleSighting> sightingEntry : sightings.entrySet())
		{
			int distance = sightingEntry.getValue().getDistanceInTiles();
			if (distance < closestDistance)
			{
				closestDistance = distance;
				closestObstacle = sightingEntry.getKey();
			}
		}
		return closestObstacle;
	}

	private AbyssObstacle chooseBestNonPassageObstacle(Map<AbyssObstacle, ObstacleSighting> sightings, MusicTrackerPlugin musicTrackerPlugin)
	{
		AbyssObstacle bestObstacle = null;
		int bestScore = Integer.MIN_VALUE;

		for (AbyssObstacle candidateObstacle : sightings.keySet())
		{
			if (candidateObstacle == AbyssObstacle.PASSAGE)
			{
				continue;
			}

			int candidateScore = scoreObstacle(candidateObstacle, musicTrackerPlugin);
			if (candidateScore > bestScore)
			{
				bestScore = candidateScore;
				bestObstacle = candidateObstacle;
			}
		}

		if (bestObstacle != null)
		{
			return bestObstacle;
		}

		return sightings.containsKey(AbyssObstacle.PASSAGE) ? AbyssObstacle.PASSAGE : null;
	}

	private List<MusicTrackEntityPoint> computeRiftHighlight(Client client, WorldPoint playerLocation, MusicTrackerPlugin musicTrackerPlugin, MusicTrack musicTrack)
	{
		if (musicTrack == null || musicTrack.getTitle() == null)
		{
			return null;
		}

		String riftNameFragment = TRACK_TITLE_TO_RIFT_NAME.get(musicTrack.getTitle().toLowerCase(Locale.ROOT));
		if (riftNameFragment == null)
		{
			return null;
		}

		Optional<WorldPoint> riftLocation = findNearestTileObjectLocationByName(client, playerLocation, riftNameFragment, RIFT_EXPECTED_ACTION, RIFT_SEARCH_RADIUS_TILES);
		if (riftLocation.isEmpty())
		{
			return null;
		}

		musicTrackerPlugin.getTrackNavigator().getNavigationCoordinator().requestShortestPathTo(riftLocation.get());

		String hintText = capitalizeFirstLetter(riftNameFragment) + " - enter here";
		return List.of(buildHighlightPoint(riftLocation.get(), riftNameFragment, hintText));
	}

	private MusicTrackEntityPoint buildHighlightPoint(WorldPoint location, String objectNameFragment, String hintText)
	{
		InteractionTarget interactionTarget = new InteractionTarget(objectNameFragment, hintText, location, InteractionType.GAME_OBJECT);
		return MusicTrackEntityPoint.from(location, interactionTarget);
	}

	private Map<AbyssObstacle, ObstacleSighting> scanForObstacles(Client client, WorldPoint worldPoint, int radiusInTiles)
	{
		Map<AbyssObstacle, ObstacleSighting> sightingsByObstacle = new EnumMap<>(AbyssObstacle.class);

		for (int xOffset = -radiusInTiles; xOffset <= radiusInTiles; xOffset++)
		{
			for (int yOffset = -radiusInTiles; yOffset <= radiusInTiles; yOffset++)
			{
				WorldPoint candidateLocation = new WorldPoint(worldPoint.getX() + xOffset, worldPoint.getY() + yOffset, worldPoint.getPlane());
				Tile candidateTile = resolveTileAt(client, candidateLocation);
				if (candidateTile == null)
				{
					continue;
				}

				int distanceToPlayer = worldPoint.distanceTo(candidateLocation);

				for (AbyssObstacle obstacle : AbyssObstacle.values())
				{
					ObstacleSighting existing = sightingsByObstacle.get(obstacle);
					if (existing != null && existing.getDistanceInTiles() <= distanceToPlayer)
					{
						continue;
					}

					if (tileHasObjectMatchingName(client, candidateTile, obstacle.getObjectNameFragment(), obstacle.getExpectedAction()))
					{
						sightingsByObstacle.put(obstacle, new ObstacleSighting(candidateLocation, distanceToPlayer));
					}
				}
			}
		}
		return sightingsByObstacle;
	}

	private Optional<WorldPoint> findNearestTileObjectLocationByName(Client client, WorldPoint centerPoint, String nameFragment, String expectedAction, int radiusInTiles)
	{
		WorldPoint bestLocation = null;
		int bestDistance = Integer.MAX_VALUE;

		for (int xOffset = -radiusInTiles; xOffset <= radiusInTiles; xOffset++)
		{
			for (int yOffset = -radiusInTiles; yOffset <= radiusInTiles; yOffset++)
			{
				WorldPoint candidateLocation = new WorldPoint(centerPoint.getX() + xOffset, centerPoint.getY() + yOffset, centerPoint.getPlane());
				int distanceToCenter = centerPoint.distanceTo(candidateLocation);
				if (distanceToCenter >= bestDistance)
				{
					continue;
				}

				Tile candidateTile = resolveTileAt(client, candidateLocation);
				if (candidateTile == null)
				{
					continue;
				}

				if (tileHasObjectMatchingName(client, candidateTile, nameFragment, expectedAction))
				{
					bestDistance = distanceToCenter;
					bestLocation = candidateLocation;
				}
			}
		}

		return Optional.ofNullable(bestLocation);
	}

	private Tile resolveTileAt(Client client, WorldPoint worldPoint)
	{
		LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
		if (localPoint == null)
		{
			return null;
		}

		Tile[][][] tilesByPlane = client.getScene().getTiles();
		int plane = worldPoint.getPlane();
		if (plane < 0 || plane >= tilesByPlane.length)
		{
			return null;
		}

		int sceneX = localPoint.getSceneX();
		int sceneY = localPoint.getSceneY();
		if (sceneX < 0 || sceneX >= tilesByPlane[plane].length || sceneY < 0 || sceneY >= tilesByPlane[plane][sceneX].length)
		{
			return null;
		}

		return tilesByPlane[plane][sceneX][sceneY];
	}

	private boolean tileHasObjectMatchingName(Client client, Tile tile, String nameFragment, String expectedAction)
	{
		for (GameObject gameObject : tile.getGameObjects())
		{
			if (gameObject != null && hasNameAndAction(client, gameObject.getId(), nameFragment, expectedAction))
			{
				return true;
			}
		}

		WallObject wallObject = tile.getWallObject();
		if (wallObject != null && hasNameAndAction(client, wallObject.getId(), nameFragment, expectedAction))
		{
			return true;
		}

		DecorativeObject decorativeObject = tile.getDecorativeObject();
		if (decorativeObject != null && hasNameAndAction(client, decorativeObject.getId(), nameFragment, expectedAction))
		{
			return true;
		}

		GroundObject groundObject = tile.getGroundObject();
		return groundObject != null && hasNameAndAction(client, groundObject.getId(), nameFragment, expectedAction);
	}

	private boolean hasNameAndAction(Client client, int objectId, String nameFragment, String expectedAction)
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
			if (objectName == null || objectName.equals("null") || !objectName.toLowerCase(Locale.ROOT).contains(nameFragment))
			{
				return false;
			}

			if (expectedAction == null)
			{
				return true;
			}

			String[] actions = objectComposition.getActions();
			if (actions == null)
			{
				return false;
			}

			for (String action : actions)
			{
				if (action != null && action.equalsIgnoreCase(expectedAction))
				{
					return true;
				}
			}
			return false;
		}
		catch (Exception objectLookupException)
		{
			return false;
		}
	}

	private String capitalizeFirstLetter(String text)
	{
		if (text == null || text.isBlank())
		{
			return text;
		}
		return Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}
}