package org.dejaq.plugins.musictracker.json;

import java.util.List;

public class RouteJson
{
	public String name;
	public String track;

	public boolean defaultRoute;
	public List<TrackStepJson> trackSteps;
	public String quest;
	public String notes;
	public List<ItemRequirementJson> items;
	public List<ItemRequirementJson> recommendedItems;
	public List<SkillRequirementJson> levels;
	public List<SkillRequirementJson> recommendedLevels;
}