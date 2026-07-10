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
	private static final int REQUIRED_SINGLE_SKILL_LEVEL = 99;
	private static final int REQUIRED_COMBINED_SKILL_LEVEL = 130;

	@Override
	public List<DynamicRequirement<LevelRequirement>> getDynamicLevels(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		int currentAttackLevel = musicTrackerPlugin.getClient().getRealSkillLevel(Skill.ATTACK);
		int currentStrengthLevel = musicTrackerPlugin.getClient().getRealSkillLevel(Skill.STRENGTH);

		boolean hasRequiredAttackLevel = currentAttackLevel >= REQUIRED_SINGLE_SKILL_LEVEL;
		boolean hasRequiredStrengthLevel = currentStrengthLevel >= REQUIRED_SINGLE_SKILL_LEVEL;
		boolean hasRequiredCombinedLevel = (currentAttackLevel + currentStrengthLevel) >= REQUIRED_COMBINED_SKILL_LEVEL;

		if (hasRequiredAttackLevel)
		{
			return List.of(DynamicRequirement.of(null, REQUIRED_SINGLE_SKILL_LEVEL + " Attack", ColorScheme.PROGRESS_COMPLETE_COLOR));
		}
		if (hasRequiredStrengthLevel)
		{
			return List.of(DynamicRequirement.of(null, REQUIRED_SINGLE_SKILL_LEVEL + " Strength", ColorScheme.PROGRESS_COMPLETE_COLOR));
		}
		if (hasRequiredCombinedLevel)
		{
			return List.of(DynamicRequirement.of(null, "Combined level " + REQUIRED_COMBINED_SKILL_LEVEL + " Attack and Strength", ColorScheme.PROGRESS_COMPLETE_COLOR));
		}

		return List.of(
			DynamicRequirement.of(null, "Combined level " + REQUIRED_COMBINED_SKILL_LEVEL + " Attack and Strength or", ColorScheme.PROGRESS_ERROR_COLOR),
			DynamicRequirement.of(null, REQUIRED_SINGLE_SKILL_LEVEL + " Attack", ColorScheme.PROGRESS_ERROR_COLOR),
			DynamicRequirement.of(null, REQUIRED_SINGLE_SKILL_LEVEL + " Strength", ColorScheme.PROGRESS_ERROR_COLOR)
		);
	}
}