package org.dejaq.plugins.musictracker.navigation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProgressionSystem
{
	DEFAULT("Default"),
	PROXIMITY("Experimental: Proximity"),
	RELATIVE_DISTANCE("Experimental: Relative Distance"),
	SEGMENT_PROJECTION("Experimental: Segment Projection");

	private final String displayName;

	@Override
	public String toString()
	{
		return displayName;
	}
}