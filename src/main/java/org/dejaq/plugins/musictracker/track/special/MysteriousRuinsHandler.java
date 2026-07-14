package org.dejaq.plugins.musictracker.track.special;

import java.util.List;

public class MysteriousRuinsHandler implements SpecialTrackHandler
{
	private static final List<String> ROUTE_NAMES = List.of(
		"Mysterious Ruins"
	);

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

	@Override
	public List<String> getHandledRouteNames()
	{
		return ROUTE_NAMES;
	}
}
