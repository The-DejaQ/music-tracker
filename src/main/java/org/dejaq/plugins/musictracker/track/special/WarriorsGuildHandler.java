package org.dejaq.plugins.musictracker.track.special;

import java.util.List;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.requirement.DynamicRequirement;
import org.dejaq.plugins.musictracker.requirement.LevelRequirement;
import org.dejaq.plugins.musictracker.track.Route;

public class WarriorsGuildHandler implements SpecialTrackHandler
{
	private static final List<String> ROUTE_NAMES = List.of(
		"Route 1"
	);

	private static final int REQUIRED_SINGLE_SKILL_LEVEL = 99;
	private static final int REQUIRED_COMBINED_SKILL_LEVEL = 130;

	private static final String SINGLE_ATTACK_TEXT = REQUIRED_SINGLE_SKILL_LEVEL + " Attack";
	private static final String SINGLE_STRENGTH_TEXT = REQUIRED_SINGLE_SKILL_LEVEL + " Strength";
	private static final String COMBINED_TEXT = "Combined level " + REQUIRED_COMBINED_SKILL_LEVEL + " Attack and Strength";

	@Override
	public List<String> getHandledRouteNames()
	{
		return ROUTE_NAMES;
	}

	@Override
	public List<DynamicRequirement<LevelRequirement>> getDynamicLevels(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		int currentAttackLevel = musicTrackerPlugin.getClient().getRealSkillLevel(Skill.ATTACK);
		int currentStrengthLevel = musicTrackerPlugin.getClient().getRealSkillLevel(Skill.STRENGTH);
		int currentCombinedLevel = currentAttackLevel + currentStrengthLevel;

		if (currentAttackLevel >= REQUIRED_SINGLE_SKILL_LEVEL)
		{
			return List.of(satisfied(SINGLE_ATTACK_TEXT));
		}
		if (currentStrengthLevel >= REQUIRED_SINGLE_SKILL_LEVEL)
		{
			return List.of(satisfied(SINGLE_STRENGTH_TEXT));
		}
		if (currentCombinedLevel >= REQUIRED_COMBINED_SKILL_LEVEL)
		{
			return List.of(satisfied(COMBINED_TEXT));
		}

		return List.of(
			unsatisfied(COMBINED_TEXT + " or"),
			unsatisfied(SINGLE_ATTACK_TEXT),
			unsatisfied(SINGLE_STRENGTH_TEXT)
		);
	}

	private static DynamicRequirement<LevelRequirement> satisfied(String displayText)
	{
		return DynamicRequirement.of(null, displayText, ColorScheme.PROGRESS_COMPLETE_COLOR);
	}

	private static DynamicRequirement<LevelRequirement> unsatisfied(String displayText)
	{
		return DynamicRequirement.of(null, displayText, ColorScheme.PROGRESS_ERROR_COLOR);
	}
}
