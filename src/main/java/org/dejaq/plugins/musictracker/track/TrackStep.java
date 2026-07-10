package org.dejaq.plugins.musictracker.track;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import net.runelite.api.coords.WorldPoint;

@Data
@Builder
@NoArgsConstructor
public class TrackStep
{

	private String name;

	private WorldPoint destination;

	@Singular("interaction")
	private List<InteractionTarget> interactions;

	public TrackStep(WorldPoint destination)
	{
		this("Track Step", destination, List.of());
	}

	public TrackStep(String name, WorldPoint destination)
	{
		this(name, destination, List.of());
	}

	public TrackStep(WorldPoint destination, List<InteractionTarget> interactions)
	{
		this("Track Step", destination, interactions);
	}

	public TrackStep(String name, WorldPoint destination, List<InteractionTarget> interactions)
	{
		this.name = name;
		this.destination = destination;
		this.interactions = interactions;
	}

	public boolean hasInteractions()
	{
		return interactions != null && !interactions.isEmpty();
	}

	public boolean hasAdvancementInteraction()
	{
		if (!hasInteractions())
		{
			return false;
		}
		return interactions.stream().anyMatch(InteractionTarget::canAdvanceProgress);
	}

	public InteractionTarget getPrimaryInteraction()
	{
		if (!hasInteractions())
		{
			return null;
		}

		return interactions.stream()
			.filter(InteractionTarget::canAdvanceProgress)
			.findFirst()
			.orElse(null);
	}

	public List<InteractionTarget> getAllHighlights()
	{
		return interactions == null ? List.of() : interactions;
	}

	public List<InteractionTarget> getAdvancementInteractions()
	{
		if (!hasInteractions())
		{
			return List.of();
		}
		return interactions.stream()
			.filter(InteractionTarget::canAdvanceProgress)
			.collect(java.util.stream.Collectors.toList());
	}
}
