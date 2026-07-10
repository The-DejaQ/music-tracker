package org.dejaq.plugins.musictracker.track;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import net.runelite.api.coords.WorldPoint;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionTarget
{

	private int entityId;
	private String entity;
	private WorldPoint location;
	private InteractionType type;
	private String hint;
	@Singular("action")
	private List<String> actions;
	private int searchRadius;

	public InteractionTarget(int entityId, String hint, InteractionType type)
	{
		this(entityId, null, null, type, hint, List.of(), -1);
	}

	public InteractionTarget(int entityId, String hint, WorldPoint worldPoint, InteractionType type)
	{
		this(entityId, null, worldPoint, type, hint, List.of(), -1);
	}

	public InteractionTarget(int entityId, InteractionType type)
	{
		this(entityId, null, null, type, null, List.of(), -1);
	}

	public InteractionTarget(String entity, InteractionType type)
	{
		this(-1, entity, null, type, null, List.of(), -1);
	}

	public InteractionTarget(String entity, String hint, WorldPoint worldPoint, InteractionType type)
	{
		this(-1, entity, worldPoint, type, hint, List.of(), -1);
	}

	public InteractionTarget(WorldPoint location, InteractionType type)
	{
		this(-1, null, location, type, null, List.of(), -1);
	}

	public boolean isIdBased()
	{
		return entityId > 0;
	}

	public boolean isNameBased()
	{
		return entity != null && !entity.isBlank();
	}

	public boolean canAdvanceProgress()
	{
		return actions != null && !actions.isEmpty();
	}

	public WorldPoint getEffectiveLocation(WorldPoint fallback)
	{
		return location != null ? location : fallback;
	}

	public boolean hasCustomSearchRadius()
	{
		return searchRadius > 0;
	}
}