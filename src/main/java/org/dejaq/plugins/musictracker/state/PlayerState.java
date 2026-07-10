package org.dejaq.plugins.musictracker.state;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import org.dejaq.plugins.musictracker.quest.Quest;
import org.dejaq.plugins.musictracker.quest.QuestState;

@Singleton
public class PlayerState
{
	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;

	@Getter
	private final Map<Integer, Boolean> varbitData = new ConcurrentHashMap<>();

	@Getter
	@Setter
	private GameState gameState = GameState.LOGIN_SCREEN;

	private final Map<Integer, Integer> inventoryItemQuantities = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> equipmentItemQuantities = new ConcurrentHashMap<>();

	private final Map<Skill, Integer> realSkillLevels = new ConcurrentHashMap<>();
	private final Map<Quest, QuestState> questStateCache = new ConcurrentHashMap<>();
	private final Set<Quest> questStatesInFlight = ConcurrentHashMap.newKeySet();

	public void refreshTrackingVarbits()
	{
		varbitData.put(VarbitID.MUSIC_UNLOCK_TEXT_TOGGLE, client.getVarbitValue(VarbitID.MUSIC_UNLOCK_TEXT_TOGGLE) == 0);
		varbitData.put(VarbitID.MUSIC_AREA_MODE, client.getVarbitValue(VarbitID.MUSIC_AREA_MODE) == 0);
	}

	public boolean canTrackMusic()
	{
		return varbitData.getOrDefault(VarbitID.MUSIC_UNLOCK_TEXT_TOGGLE, false)
			&& varbitData.getOrDefault(VarbitID.MUSIC_AREA_MODE, false);
	}

	public void refreshItemQuantityCaches()
	{
		clientThread.invokeLater(() -> {
			inventoryItemQuantities.clear();
			equipmentItemQuantities.clear();

			ItemContainer inventoryItemContainer = client.getItemContainer(InventoryID.INV);
			if (inventoryItemContainer != null)
			{
				for (Item inventoryItem : inventoryItemContainer.getItems())
				{
					if (inventoryItem != null && inventoryItem.getId() > 0)
					{
						inventoryItemQuantities.merge(inventoryItem.getId(), inventoryItem.getQuantity(), Integer::sum);
					}
				}
			}

			ItemContainer equippedItemContainer = client.getItemContainer(InventoryID.WORN);
			if (equippedItemContainer != null)
			{
				for (Item equippedItem : equippedItemContainer.getItems())
				{
					if (equippedItem != null && equippedItem.getId() > 0)
					{
						equipmentItemQuantities.merge(equippedItem.getId(), equippedItem.getQuantity(), Integer::sum);
					}
				}
			}
		});
	}

	public void clearItemQuantityCaches()
	{
		inventoryItemQuantities.clear();
		equipmentItemQuantities.clear();
	}

	public int getItemQuantity(int itemId)
	{
		return inventoryItemQuantities.getOrDefault(itemId, 0) + equipmentItemQuantities.getOrDefault(itemId, 0);
	}

	public boolean hasItemQuantity(int itemId, int requiredQuantity)
	{
		int totalQuantity = inventoryItemQuantities.getOrDefault(itemId, 0) + equipmentItemQuantities.getOrDefault(itemId, 0);
		return totalQuantity >= requiredQuantity;
	}

	public boolean hasAnyItemFromGroup(List<Integer> candidateItemIds, int requiredQuantity)
	{
		int totalQuantity = 0;
		for (int candidateItemId : candidateItemIds)
		{
			totalQuantity += inventoryItemQuantities.getOrDefault(candidateItemId, 0);
			totalQuantity += equipmentItemQuantities.getOrDefault(candidateItemId, 0);
		}
		return totalQuantity >= requiredQuantity;
	}

	public boolean hasItemByPartialName(String itemNameFragment, int requiredQuantity, ItemManager itemManager)
	{
		String lowerCaseNameFragment = itemNameFragment.toLowerCase();
		int totalQuantity = 0;

		for (Map.Entry<Integer, Integer> inventoryEntry : inventoryItemQuantities.entrySet())
		{
			ItemComposition itemComposition = itemManager.getItemComposition(inventoryEntry.getKey());
			if (itemComposition != null && itemComposition.getName() != null
				&& itemComposition.getName().toLowerCase().contains(lowerCaseNameFragment))
			{
				totalQuantity += inventoryEntry.getValue();
			}
		}
		for (Map.Entry<Integer, Integer> equipmentEntry : equipmentItemQuantities.entrySet())
		{
			ItemComposition itemComposition = itemManager.getItemComposition(equipmentEntry.getKey());
			if (itemComposition != null && itemComposition.getName() != null
				&& itemComposition.getName().toLowerCase().contains(lowerCaseNameFragment))
			{
				totalQuantity += equipmentEntry.getValue();
			}
		}
		return totalQuantity >= requiredQuantity;
	}

	public void updateRealSkillLevel(Skill skill, int realLevel)
	{
		realSkillLevels.put(skill, realLevel);
	}

	public int getCachedRealSkillLevel(Skill skill)
	{
		return realSkillLevels.getOrDefault(skill, 1);
	}

	public QuestState getCachedQuestState(Quest quest, Runnable onCacheUpdated)
	{
		QuestState cachedQuestState = questStateCache.get(quest);
		if (cachedQuestState == null)
		{
			refreshQuestStateAsync(quest, onCacheUpdated);
			return QuestState.NOT_STARTED;
		}
		return cachedQuestState;
	}

	public QuestState getCachedQuestState(Quest quest)
	{
		return getCachedQuestState(quest, null);
	}

	private void refreshQuestStateAsync(Quest quest, Runnable onCacheUpdated)
	{
		if (!questStatesInFlight.add(quest))
		{
			return;
		}

		clientThread.invokeLater(() -> {
			questStateCache.put(quest, quest.getState(client));
			questStatesInFlight.remove(quest);
			if (onCacheUpdated != null)
			{
				SwingUtilities.invokeLater(onCacheUpdated);
			}
		});
	}

	public void reset()
	{
		gameState = GameState.LOGIN_SCREEN;
		clearItemQuantityCaches();
		questStateCache.clear();
		questStatesInFlight.clear();
		realSkillLevels.clear();
	}
}