package org.dejaq.plugins.musictracker.json;

import java.util.List;
import java.util.Map;

public class MusicTrackJson
{
	public String title;
	public String region;
	public String location;
	public Boolean members;
	public String quest;
	public String notes;
	public String unlockHint;
	public String unlockType;
	public String unlockMessage;
	public String wikiUrl;

	public List<ItemRequirementJson> items;
	public List<ItemRequirementJson> recommendedItems;

	public Map<String, Integer> levels;
	public Map<String, Integer> recommendedLevels;

	public List<TrackStepJson> trackSteps;

	public List<RouteJson> routes;
}