package org.dejaq.plugins.musictracker.requirement;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.runelite.client.game.ItemManager;
import org.dejaq.plugins.musictracker.requirement.collections.ItemCollections;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequirement
{
	@Builder.Default
	private int itemId = -1;
	private ItemCollections itemCollection;
	@Builder.Default
	private int quantity = 1;
	private String label;
	@Builder.Default
	private boolean equipped = false;

	public ItemRequirement(int itemId, int quantity)
	{
		this.itemId = itemId;
		this.quantity = quantity;
	}

	public ItemRequirement(ItemCollections itemCollection, int quantity)
	{
		this.itemCollection = itemCollection;
		this.quantity = quantity;
	}

	public boolean isItemCollection()
	{
		return itemCollection != null;
	}

	public List<Integer> getGroupItemIds()
	{
		return isItemCollection() ? itemCollection.getItems() : List.of();
	}

	public boolean hasItem()
	{
		return itemId > 0 || itemCollection != null;
	}

	public String getEffectiveName(ItemManager itemManager)
	{
		if (label != null && !label.isBlank())
		{
			return label;
		}

		if (itemCollection != null)
		{
			return formatItemCollectionsName(itemCollection.name());
		}

		if (itemManager != null && itemId > 0)
		{
			try
			{
				return itemManager.getItemComposition(itemId).getName();
			}
			catch (Exception itemCompositionLookupException)
			{
				return "Item #" + itemId;
			}
		}

		return "Item #" + itemId;
	}

	public ItemRequirement setLabel(String label)
	{
		this.label = label;
		return this;
	}

	public String getDisplayText(ItemManager itemManager)
	{
		if (label != null && !label.isBlank())
		{
			return label;
		}

		String effectiveName = getEffectiveName(itemManager);

		if (isItemCollection())
		{
			return effectiveName;
		}

		return effectiveName + " x " + quantity;
	}

	private String formatItemCollectionsName(String enumName)
	{
		if (enumName == null || enumName.isBlank())
		{
			return "Unknown Item";
		}

		StringBuilder formattedNameBuilder = new StringBuilder();
		String[] enumNameWords = enumName.toLowerCase().split("_");

		for (int wordIndex = 0; wordIndex < enumNameWords.length; wordIndex++)
		{
			if (enumNameWords[wordIndex].isEmpty())
			{
				continue;
			}
			formattedNameBuilder.append(Character.toUpperCase(enumNameWords[wordIndex].charAt(0)));
			if (enumNameWords[wordIndex].length() > 1)
			{
				formattedNameBuilder.append(enumNameWords[wordIndex].substring(1));
			}
			if (wordIndex < enumNameWords.length - 1)
			{
				formattedNameBuilder.append(" ");
			}
		}

		return formattedNameBuilder.toString();
	}
}