package org.dejaq.plugins.musictracker.requirement;

import java.awt.Color;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DynamicRequirement<T>
{

	private T requirement;
	private String displayText;
	private Color color;

	public static <T> DynamicRequirement<T> of(T requirement, String displayText, Color color)
	{
		return DynamicRequirement.<T>builder()
			.requirement(requirement)
			.displayText(displayText)
			.color(color)
			.build();
	}
}