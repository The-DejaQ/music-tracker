package org.dejaq.plugins.musictracker.requirement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.runelite.api.Skill;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelRequirement
{
	private Skill skill;
	private int level;

	public static LevelRequirement of(Skill skill, int level)
	{
		return LevelRequirement.builder()
			.skill(skill)
			.level(level)
			.build();
	}

	public String getDisplayText()
	{
		return skill.getName() + " " + level;
	}
}