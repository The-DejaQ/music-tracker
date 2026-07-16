package org.dejaq.plugins.musictracker.track.special;

import java.awt.Color;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.game.ItemEquipmentStats;
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
public class RighteousnessHandler implements SpecialTrackHandler
{
	private static final List<String> ENTRANA_ROUTE_NAMES = List.of("Entrana");

	private static final String MONK_OF_ENTRANA_NAME = "Monk of Entrana";
	private static final String BANK_DEPOSIT_BOX_NAME = "Bank deposit box";
	private static final String MYSTERIOUS_RUINS_NAME = "Mysterious ruins";

	private static final String MONK_TAKE_BOAT_HINT = "Take-boat";
	private static final String MONK_TAKE_BOAT_ACTION = "Take-boat";
	private static final String DEPOSIT_BOX_HINT = "Deposit all combat gear";
	private static final String DEPOSIT_BOX_ACTION = "Deposit";

	private static final String RUINS_HINT_ENTER = "Enter";
	private static final String RUINS_HINT_WEAR_THEN_ENTER = "Wear tiara then enter";
	private static final String RUINS_HINT_USE_TALISMAN = "Use talisman on ruins";

	private static final String NO_COMBAT_GEAR_REQUIREMENT_DISPLAY_TEXT = "No combat gear";

	private static final WorldPoint MONK_OF_ENTRANA_LOCATION = new WorldPoint(3046, 3237, 0);
	private static final WorldPoint BANK_DEPOSIT_BOX_LOCATION = new WorldPoint(3045, 3234, 0);
	private static final WorldPoint LAW_ALTAR_RUINS_LOCATION = new WorldPoint(2857, 3380, 0);

	private static final WorldPoint ENTRANA_ISLAND_BORDER_CENTER = new WorldPoint(2840, 3359, 0);
	private static final int ENTRANA_ISLAND_BORDER_RADIUS = 39;
	private static final int MONK_OF_ENTRANA_TRAVEL_HIGHLIGHT_DISTANCE_THRESHOLD = 30;

	@Override
	public List<String> getHandledRouteNames()
	{
		return ENTRANA_ROUTE_NAMES;
	}

	@Override
	public boolean hasVolatileDynamicHighlights(MusicTrack currentMusicTrack, Route currentRoute, MusicTrackerPlugin musicTrackerPlugin)
	{
		return true;
	}

	@Override
	public List<DynamicRequirement<ItemRequirement>> getDynamicItems(MusicTrack currentMusicTrack, Route currentRoute, MusicTrackerPlugin musicTrackerPlugin)
	{
		Color combatGearRequirementColor = canAccessEntrana(musicTrackerPlugin)
			? ColorScheme.PROGRESS_COMPLETE_COLOR
			: ColorScheme.PROGRESS_ERROR_COLOR;

		return addDynamicItems(currentRoute, musicTrackerPlugin,
			DynamicRequirement.of(null, NO_COMBAT_GEAR_REQUIREMENT_DISPLAY_TEXT, combatGearRequirementColor));
	}

	@Override
	public List<MusicTrackEntityPoint> getDynamicEntityHighlights(MusicTrack currentMusicTrack, Route currentRoute, TrackStep currentTrackStep, int currentStageIndex, MusicTrackerPlugin musicTrackerPlugin)
	{
		if (currentTrackStep == null)
		{
			return null;
		}

		Client gameClient = musicTrackerPlugin.getClient();
		if (!gameClient.isClientThread())
		{
			log.warn("getDynamicEntityHighlights called off the client thread; ignoring");
			return null;
		}

		WorldPoint currentPlayerLocation = resolveCurrentPlayerWorldLocation(gameClient);

		if (currentPlayerLocation != null && isPlayerOnEntranaIsland(currentPlayerLocation))
		{
			return buildRuinsHighlight(musicTrackerPlugin);
		}

		if (currentPlayerLocation != null && isPlayerNearMonkOfEntrana(currentPlayerLocation))
		{
			return buildTravelHighlight(musicTrackerPlugin);
		}

		if (currentTrackStep.hasEntity(MONK_OF_ENTRANA_NAME) || currentTrackStep.hasEntity(BANK_DEPOSIT_BOX_NAME))
		{
			return buildTravelHighlight(musicTrackerPlugin);
		}

		if (currentTrackStep.hasEntity(MYSTERIOUS_RUINS_NAME))
		{
			return buildRuinsHighlight(musicTrackerPlugin);
		}

		return null;
	}

	private WorldPoint resolveCurrentPlayerWorldLocation(Client gameClient)
	{
		Player localPlayer = gameClient.getLocalPlayer();
		return localPlayer != null ? localPlayer.getWorldLocation() : null;
	}

	private boolean isPlayerOnEntranaIsland(WorldPoint currentPlayerLocation)
	{
		return currentPlayerLocation.distanceTo(ENTRANA_ISLAND_BORDER_CENTER) <= ENTRANA_ISLAND_BORDER_RADIUS;
	}

	private boolean isPlayerNearMonkOfEntrana(WorldPoint currentPlayerLocation)
	{
		return currentPlayerLocation.distanceTo(MONK_OF_ENTRANA_LOCATION) < MONK_OF_ENTRANA_TRAVEL_HIGHLIGHT_DISTANCE_THRESHOLD;
	}

	private List<MusicTrackEntityPoint> buildTravelHighlight(MusicTrackerPlugin musicTrackerPlugin)
	{
		if (canAccessEntrana(musicTrackerPlugin))
		{
			InteractionTarget monkInteraction = InteractionTarget.builder()
				.entity(MONK_OF_ENTRANA_NAME)
				.location(MONK_OF_ENTRANA_LOCATION)
				.type(InteractionType.NPC)
				.hint(MONK_TAKE_BOAT_HINT)
				.action(MONK_TAKE_BOAT_ACTION)
				.build();

			return List.of(MusicTrackEntityPoint.from(MONK_OF_ENTRANA_LOCATION, monkInteraction));
		}

		InteractionTarget depositBoxInteraction = InteractionTarget.builder()
			.entity(BANK_DEPOSIT_BOX_NAME)
			.location(BANK_DEPOSIT_BOX_LOCATION)
			.type(InteractionType.GAME_OBJECT)
			.hint(DEPOSIT_BOX_HINT)
			.action(DEPOSIT_BOX_ACTION)
			.build();

		return List.of(MusicTrackEntityPoint.from(BANK_DEPOSIT_BOX_LOCATION, depositBoxInteraction));
	}

	private List<MusicTrackEntityPoint> buildRuinsHighlight(MusicTrackerPlugin musicTrackerPlugin)
	{
		String ruinsHint = resolveRuinsHint(musicTrackerPlugin);
		if (ruinsHint == null)
		{
			return null;
		}

		InteractionTarget ruinsInteraction = InteractionTarget.builder()
			.entityId(-1)
			.entity(MYSTERIOUS_RUINS_NAME)
			.location(LAW_ALTAR_RUINS_LOCATION)
			.type(InteractionType.GAME_OBJECT)
			.hint(ruinsHint)
			.build();

		return List.of(MusicTrackEntityPoint.from(LAW_ALTAR_RUINS_LOCATION, ruinsInteraction));
	}

	private String resolveRuinsHint(MusicTrackerPlugin musicTrackerPlugin)
	{
		ItemRequirement lawTiaraRequirement = new ItemRequirement(ItemCollections.LAW_ALTAR_WEARABLE, 1);
		if (musicTrackerPlugin.playerHasItemEquipped(lawTiaraRequirement))
		{
			return RUINS_HINT_ENTER;
		}

		ItemRequirement lawTalismanRequirement = new ItemRequirement(ItemCollections.LAW_ALTAR, 1);
		if (musicTrackerPlugin.playerHasItem(lawTalismanRequirement))
		{
			return RUINS_HINT_USE_TALISMAN;
		}

		if (musicTrackerPlugin.playerHasItem(lawTiaraRequirement))
		{
			return RUINS_HINT_WEAR_THEN_ENTER;
		}
		return null;
	}

	private boolean canAccessEntrana(MusicTrackerPlugin musicTrackerPlugin)
	{
		Client gameClient = musicTrackerPlugin.getClient();
		ItemManager itemManager = musicTrackerPlugin.getItemManager();

		ItemContainer equippedItemContainer = gameClient.getItemContainer(InventoryID.WORN);
		if (equippedItemContainer != null)
		{
			for (Item equippedItem : equippedItemContainer.getItems())
			{
				if (equippedItem != null && equippedItem.getId() > 0 && isEntranaBlockingItem(equippedItem.getId(), itemManager))
				{
					return false;
				}
			}
		}

		ItemContainer inventoryItemContainer = gameClient.getItemContainer(InventoryID.INV);
		if (inventoryItemContainer != null)
		{
			for (Item inventoryItem : inventoryItemContainer.getItems())
			{
				if (inventoryItem != null && inventoryItem.getId() > 0 && isEntranaBlockingItem(inventoryItem.getId(), itemManager))
				{
					return false;
				}
			}
		}

		return true;
	}

	private boolean isEntranaBlockingItem(int itemId, ItemManager itemManager)
	{
		if (ItemCollections.ENTRANA_BLOCKED.getItems().contains(itemId))
		{
			return false;
		}

		if (ItemCollections.ENTRANA_ALLOWED_BONUS.getItems().contains(itemId))
		{
			return true;
		}

		if (isJewellery(itemId, itemManager))
		{
			return false;
		}

		if (isAmmunition(itemId, itemManager))
		{
			return isDart(itemId, itemManager);
		}

		return hasCombatBonus(itemId, itemManager);
	}

	private boolean isJewellery(int itemId, ItemManager itemManager)
	{
		ItemStats itemStats = itemManager.getItemStats(itemId);
		if (itemStats == null || !itemStats.isEquipable())
		{
			return false;
		}

		ItemEquipmentStats itemEquipmentStats = itemStats.getEquipment();
		if (itemEquipmentStats == null)
		{
			return false;
		}

		int equipmentSlot = itemEquipmentStats.getSlot();
		if (equipmentSlot == EquipmentInventorySlot.RING.getSlotIdx()
			|| equipmentSlot == EquipmentInventorySlot.AMULET.getSlotIdx())
		{
			return true;
		}

		if (equipmentSlot == EquipmentInventorySlot.GLOVES.getSlotIdx())
		{
			String itemName = itemManager.getItemComposition(itemId).getName();
			return itemName != null && itemName.toLowerCase(Locale.ROOT).contains("bracelet");
		}

		return false;
	}

	private boolean isAmmunition(int itemId, ItemManager itemManager)
	{
		ItemStats itemStats = itemManager.getItemStats(itemId);
		if (itemStats == null || !itemStats.isEquipable())
		{
			return false;
		}

		ItemEquipmentStats itemEquipmentStats = itemStats.getEquipment();
		return itemEquipmentStats != null && itemEquipmentStats.getSlot() == EquipmentInventorySlot.AMMO.getSlotIdx();
	}

	private boolean isDart(int itemId, ItemManager itemManager)
	{
		String itemName = itemManager.getItemComposition(itemId).getName();
		return itemName != null && itemName.toLowerCase(Locale.ROOT).contains("dart");
	}

	private boolean hasCombatBonus(int itemId, ItemManager itemManager)
	{
		ItemStats itemStats = itemManager.getItemStats(itemId);
		if (itemStats == null || !itemStats.isEquipable())
		{
			return false;
		}

		ItemEquipmentStats itemEquipmentStats = itemStats.getEquipment();
		if (itemEquipmentStats == null)
		{
			return false;
		}

		int[] combatBonuses = {
			itemEquipmentStats.getAstab(),
			itemEquipmentStats.getAslash(),
			itemEquipmentStats.getAcrush(),
			itemEquipmentStats.getAmagic(),
			itemEquipmentStats.getArange(),
			itemEquipmentStats.getDstab(),
			itemEquipmentStats.getDslash(),
			itemEquipmentStats.getDcrush(),
			itemEquipmentStats.getDmagic(),
			itemEquipmentStats.getDrange(),
			itemEquipmentStats.getStr(),
			itemEquipmentStats.getRstr()
		};

		for (int combatBonus : combatBonuses)
		{
			if (combatBonus != 0)
			{
				return true;
			}
		}

		if (itemEquipmentStats.getMdmg() != 0)
		{
			return true;
		}

		return false;
	}
}