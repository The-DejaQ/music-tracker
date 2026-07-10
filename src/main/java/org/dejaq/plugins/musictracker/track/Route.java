package org.dejaq.plugins.musictracker.track;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.quest.Quest;
import org.dejaq.plugins.musictracker.requirement.ItemRequirement;
import org.dejaq.plugins.musictracker.requirement.LevelRequirement;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route
{

	private String name;
	private int index;

	@Singular("step")
	private List<TrackStep> trackSteps;

	private Quest quest;

	@Singular("item")
	private List<ItemRequirement> items;

	@Singular("recommendedItem")
	private List<ItemRequirement> recommendedItems;

	@Singular("level")
	private List<LevelRequirement> levels;

	@Singular("recommendedLevel")
	private List<LevelRequirement> recommendedLevels;

	private String notes;

	private boolean defaultRoute;

	private boolean custom;

	public Route(String name, List<TrackStep> trackSteps)
	{
		this(name, trackSteps, false);
	}

	public Route(String name, List<TrackStep> trackSteps, boolean defaultRoute)
	{
		this.name = name;
		this.trackSteps = trackSteps;
		this.defaultRoute = defaultRoute;
	}

	public boolean isRoute(String name)
	{
		return this.name.equalsIgnoreCase(name);
	}

	public boolean isRoute(int index)
	{
		return this.index == index;
	}

	public int getStepCount()
	{
		return trackSteps == null ? 0 : trackSteps.size();
	}

	public boolean hasSteps()
	{
		return trackSteps != null && !trackSteps.isEmpty();
	}

	public TrackStep getStep(int index)
	{
		if (trackSteps == null || index < 0 || index >= trackSteps.size())
		{
			return null;
		}
		return trackSteps.get(index);
	}

	public WorldPoint getFinalDestination()
	{
		if (!hasSteps())
		{
			return null;
		}
		TrackStep trackStep = trackSteps.get(trackSteps.size() - 1);
		return trackStep != null ? trackStep.getDestination() : null;
	}
}