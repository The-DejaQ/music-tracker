package org.dejaq.plugins.musictracker.track.special;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
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
public class RaceAgainstClockHandler implements SpecialTrackHandler
{
	private static final List<String> ROUTE_NAMES = List.of(
		"Route 1"
	);

	private static final int REQUIRED_BARRONITE_SHARD_QUANTITY = 750;

	private static final WorldPoint CAMDOZAAL_CENTER = new WorldPoint(2968, 5798, 0);
	private static final WorldPoint BARRONITE_ROCKS_MINING_LOCATION = new WorldPoint(2913, 5810, 0);
	private static final WorldPoint SACRED_FORGE_LOCATION = new WorldPoint(2956, 5810, 0);
	private static final WorldPoint VAULT_DOOR_LOCATION = new WorldPoint(2969, 5824, 0);

	private static final String BARRONITE_ROCKS_OBJECT_NAME = "Barronite Rocks";
	private static final String SACRED_FORGE_OBJECT_NAME = "Sacred Forge";
	private static final String VAULT_DOOR_OBJECT_NAME = "Vault Door";
	private static final String MINE_ACTION = "mine";
	private static final String FUEL_ACTION = "fuel";
	private static final String CHECK_ACTION = "check";
	private static final String OPEN_ACTION = "open";
	private static final String MINING_HINT_TEXT = "Mine 750 Barronite shards";
	private static final String PICKAXE_REQUIREMENT_TEXT = "Pickaxe to mine Barronite shards";
	private static final String CHECK_FORGE_HINT_TEXT = "Check Sacred Forge";
	private static final String VAULT_DOOR_HINT_TEXT = "Enter the Vault Door";
	private static final String RUINS_STEP_NAME = "Ruins";
	private static final int BARRONITE_ROCKS_SEARCH_RADIUS_IN_TILES = 12;
	private static final int CAMDOZAAL_RADIUS = 64;

	private static final Pattern SACRED_FORGE_LEFT_MESSAGE_PATTERN =
		Pattern.compile("You have (\\d+) Barronite shards? left", Pattern.CASE_INSENSITIVE);

	private static final Pattern SACRED_FORGE_CHECK_MESSAGE_PATTERN =
		Pattern.compile("You currently have (\\d+) Barronite shards? stored", Pattern.CASE_INSENSITIVE);

	private static final Pattern SACRED_FORGE_FUEL_MESSAGE_PATTERN =
		Pattern.compile("You stored (\\d+) Barronite shards? in the Sacred Forge", Pattern.CASE_INSENSITIVE);

	private int cachedTick = -1;
	private int cachedStageIndex = -1;
	private List<MusicTrackEntityPoint> cachedHighlights;

	private boolean hasCheckedSacredForge;
	private int storedBarroniteShardQuantity;


	@Override
	public List<String> getHandledRouteNames()
	{
		return ROUTE_NAMES;
	}

	@Override
	public List<DynamicRequirement<ItemRequirement>> getDynamicItems(
		MusicTrack musicTrack,
		Route route,
		MusicTrackerPlugin musicTrackerPlugin)
	{
		int currentBarroniteShardQuantity = musicTrackerPlugin.getPlayerState()
			.getItemQuantity(ItemID.CAMDOZAAL_BARRONITE_SHARD);
		int totalBarroniteShardQuantity = currentBarroniteShardQuantity + getKnownStoredBarroniteShardQuantity();

		boolean forgeHasRequiredShardQuantity = hasCheckedSacredForge
			&& storedBarroniteShardQuantity >= REQUIRED_BARRONITE_SHARD_QUANTITY;
		boolean readyToFuelToCompletion = !forgeHasRequiredShardQuantity
			&& totalBarroniteShardQuantity >= REQUIRED_BARRONITE_SHARD_QUANTITY;

		ItemRequirement barroniteShardRequirement = new ItemRequirement(
			ItemID.CAMDOZAAL_BARRONITE_SHARD,
			REQUIRED_BARRONITE_SHARD_QUANTITY
		);

		String shardDisplayText = Math.min(totalBarroniteShardQuantity, REQUIRED_BARRONITE_SHARD_QUANTITY)
			+ " / " + REQUIRED_BARRONITE_SHARD_QUANTITY + " Barronite Shards"
			+ (hasCheckedSacredForge ? "" : " (unverified)");

		Color shardColor = forgeHasRequiredShardQuantity
			? ColorScheme.PROGRESS_COMPLETE_COLOR
			: readyToFuelToCompletion
			? ColorScheme.PROGRESS_INPROGRESS_COLOR
			: ColorScheme.PROGRESS_ERROR_COLOR;

		boolean hasPickaxe = musicTrackerPlugin.getPlayerState()
			.hasItemFromCollection(ItemCollections.PICKAXES.getItems());

		ItemRequirement pickaxeRequirement = new ItemRequirement(
			ItemCollections.PICKAXES,
			1
		);
		pickaxeRequirement.setLabel(PICKAXE_REQUIREMENT_TEXT);

		Color pickaxeColor = hasPickaxe
			? ColorScheme.PROGRESS_COMPLETE_COLOR
			: ColorScheme.PROGRESS_ERROR_COLOR;

		return List.of(
			DynamicRequirement.of(barroniteShardRequirement, shardDisplayText, shardColor),
			DynamicRequirement.of(pickaxeRequirement, PICKAXE_REQUIREMENT_TEXT, pickaxeColor)
		);
	}

	@Override
	public DynamicRequirement<String> getDynamicNotes(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		int currentBarroniteShardQuantity = musicTrackerPlugin.getPlayerState().getItemQuantity(ItemID.CAMDOZAAL_BARRONITE_SHARD);
		int totalBarroniteShardQuantity = currentBarroniteShardQuantity + getKnownStoredBarroniteShardQuantity();

		if (hasCheckedSacredForge && storedBarroniteShardQuantity >= REQUIRED_BARRONITE_SHARD_QUANTITY)
		{
			return DynamicRequirement.of(null, "The Sacred Forge has enough shards - head to the Vault Door.", ColorScheme.PROGRESS_COMPLETE_COLOR);
		}

		if (totalBarroniteShardQuantity >= REQUIRED_BARRONITE_SHARD_QUANTITY)
		{
			return DynamicRequirement.of(null,
				"You have " + totalBarroniteShardQuantity + " / " + REQUIRED_BARRONITE_SHARD_QUANTITY
					+ " Barronite shards total - fuel the Sacred Forge with what you're carrying to finish.",
				ColorScheme.PROGRESS_INPROGRESS_COLOR);
		}

		if (!hasCheckedSacredForge)
		{
			return DynamicRequirement.of(null,
				"Check the Sacred Forge to see how many shards you've already stored.",
				ColorScheme.PROGRESS_ERROR_COLOR);
		}

		int remainingShardsNeeded = REQUIRED_BARRONITE_SHARD_QUANTITY - totalBarroniteShardQuantity;
		String remainingShardsLabel = remainingShardsNeeded == 1 ? "shard" : "shards";
		return DynamicRequirement.of(null,
			"Mine " + remainingShardsNeeded + " more Barronite " + remainingShardsLabel
				+ " (" + totalBarroniteShardQuantity + " / " + REQUIRED_BARRONITE_SHARD_QUANTITY + " total).",
			ColorScheme.PROGRESS_ERROR_COLOR);
	}

	@Override
	public boolean hasVolatileDynamicHighlights(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		return true;
	}

	@Override
	public List<MusicTrackEntityPoint> getDynamicEntityHighlights(MusicTrack musicTrack, Route route, TrackStep trackStep, int stageIndex, MusicTrackerPlugin musicTrackerPlugin)
	{
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

		boolean playerIsInsideCamdozaal = localPlayer.getWorldLocation().distanceTo(CAMDOZAAL_CENTER) <= CAMDOZAAL_RADIUS;

		if (trackStep != null && RUINS_STEP_NAME.equalsIgnoreCase(trackStep.getName()) && !playerIsInsideCamdozaal)
		{
			musicTrackerPlugin.getTrackNavigator().getNavigationCoordinator().forgetLastRequestedTarget();
			hasCheckedSacredForge = false;
			storedBarroniteShardQuantity = 0;
			invalidateHighlightCache();
			return null;
		}

		int currentTick = client.getTickCount();
		if (currentTick == cachedTick && stageIndex == cachedStageIndex && cachedHighlights != null)
		{
			return cachedHighlights;
		}

		int currentBarroniteShardQuantity = musicTrackerPlugin.getPlayerState().getItemQuantity(ItemID.CAMDOZAAL_BARRONITE_SHARD);
		int totalBarroniteShardQuantity = currentBarroniteShardQuantity + getKnownStoredBarroniteShardQuantity();

		List<MusicTrackEntityPoint> computedHighlights;
		WorldPoint targetWorldPoint;

		if (hasCheckedSacredForge && storedBarroniteShardQuantity >= REQUIRED_BARRONITE_SHARD_QUANTITY)
		{
			computedHighlights = buildVaultDoorHighlights();
			targetWorldPoint = VAULT_DOOR_LOCATION;
		}
		else if (totalBarroniteShardQuantity >= REQUIRED_BARRONITE_SHARD_QUANTITY)
		{
			computedHighlights = buildSacredForgeHighlights(false, currentBarroniteShardQuantity);
			targetWorldPoint = SACRED_FORGE_LOCATION;
		}
		else if (!hasCheckedSacredForge)
		{
			computedHighlights = buildSacredForgeHighlights(true, currentBarroniteShardQuantity);
			targetWorldPoint = SACRED_FORGE_LOCATION;
		}
		else
		{
			computedHighlights = buildMiningHighlights();
			targetWorldPoint = BARRONITE_ROCKS_MINING_LOCATION;
		}

		musicTrackerPlugin.getTrackNavigator().getNavigationCoordinator().requestShortestPathTo(targetWorldPoint);

		cachedTick = currentTick;
		cachedStageIndex = stageIndex;
		cachedHighlights = computedHighlights;
		return computedHighlights;
	}

	@Override
	public void onChatMessage(MusicTrack musicTrack, Route route, ChatMessage chatMessageEvent, MusicTrackerPlugin musicTrackerPlugin)
	{
		String message = chatMessageEvent.getMessage();
		if (message == null)
		{
			return;
		}

		Matcher checkMessageMatcher = SACRED_FORGE_CHECK_MESSAGE_PATTERN.matcher(message);
		if (checkMessageMatcher.find())
		{
			storedBarroniteShardQuantity = parseShardQuantity(checkMessageMatcher.group(1));
			hasCheckedSacredForge = true;
			invalidateHighlightCache();
			return;
		}

		Matcher emptyMessageMatcher = SACRED_FORGE_LEFT_MESSAGE_PATTERN.matcher(message);
		if (emptyMessageMatcher.find())
		{
			storedBarroniteShardQuantity = parseShardQuantity(emptyMessageMatcher.group(1));
			hasCheckedSacredForge = true;
			invalidateHighlightCache();
			return;
		}

		Matcher fuelMessageMatcher = SACRED_FORGE_FUEL_MESSAGE_PATTERN.matcher(message);
		if (fuelMessageMatcher.find())
		{
			int fueledShardQuantity = parseShardQuantity(fuelMessageMatcher.group(1));
			if (hasCheckedSacredForge)
			{
				storedBarroniteShardQuantity += fueledShardQuantity;
			}
			invalidateHighlightCache();
		}
	}

	private int parseShardQuantity(String rawShardQuantity)
	{
		try
		{
			return Integer.parseInt(rawShardQuantity);
		}
		catch (NumberFormatException shardQuantityParseException)
		{
			return 0;
		}
	}

	private int getKnownStoredBarroniteShardQuantity()
	{
		return hasCheckedSacredForge ? storedBarroniteShardQuantity : 0;
	}

	private void invalidateHighlightCache()
	{
		cachedTick = -1;
		cachedStageIndex = -1;
		cachedHighlights = null;
	}

	@Override
	public void reset()
	{
		invalidateHighlightCache();
		hasCheckedSacredForge = false;
		storedBarroniteShardQuantity = 0;
	}

	private List<MusicTrackEntityPoint> buildMiningHighlights()
	{
		List<MusicTrackEntityPoint> miningHighlights = new ArrayList<>();

		InteractionTarget barroniteRocksInteraction = InteractionTarget.builder()
			.entityId(-1)
			.entity(BARRONITE_ROCKS_OBJECT_NAME)
			.location(BARRONITE_ROCKS_MINING_LOCATION)
			.type(InteractionType.GAME_OBJECT)
			.action(MINE_ACTION)
			.searchRadius(BARRONITE_ROCKS_SEARCH_RADIUS_IN_TILES)
			.build();
		miningHighlights.add(MusicTrackEntityPoint.from(BARRONITE_ROCKS_MINING_LOCATION, barroniteRocksInteraction));

		InteractionTarget miningTileInteraction = InteractionTarget.builder()
			.entityId(-1)
			.location(BARRONITE_ROCKS_MINING_LOCATION)
			.type(InteractionType.TILE)
			.hint(MINING_HINT_TEXT)
			.build();
		miningHighlights.add(MusicTrackEntityPoint.from(BARRONITE_ROCKS_MINING_LOCATION, miningTileInteraction));

		return miningHighlights;
	}

	private List<MusicTrackEntityPoint> buildSacredForgeHighlights(boolean promptCheck, int currentBarroniteShardQuantity)
	{
		String hintText;
		String action;

		if (promptCheck)
		{
			hintText = CHECK_FORGE_HINT_TEXT;
			action = CHECK_ACTION;
		}
		else
		{
			String shardLabel = currentBarroniteShardQuantity == 1 ? "shard" : "shards";
			hintText = "Fuel Sacred Forge with " + currentBarroniteShardQuantity + " " + shardLabel;
			action = FUEL_ACTION;
		}

		InteractionTarget sacredForgeInteraction = InteractionTarget.builder()
			.entityId(-1)
			.entity(SACRED_FORGE_OBJECT_NAME)
			.location(SACRED_FORGE_LOCATION)
			.type(InteractionType.GAME_OBJECT)
			.hint(hintText)
			.action(action)
			.build();
		return List.of(MusicTrackEntityPoint.from(SACRED_FORGE_LOCATION, sacredForgeInteraction));
	}

	private List<MusicTrackEntityPoint> buildVaultDoorHighlights()
	{
		InteractionTarget vaultDoorInteraction = InteractionTarget.builder()
			.entityId(-1)
			.entity(VAULT_DOOR_OBJECT_NAME)
			.location(VAULT_DOOR_LOCATION)
			.type(InteractionType.GAME_OBJECT)
			.hint(VAULT_DOOR_HINT_TEXT)
			.action(OPEN_ACTION)
			.build();
		return List.of(MusicTrackEntityPoint.from(VAULT_DOOR_LOCATION, vaultDoorInteraction));
	}
}