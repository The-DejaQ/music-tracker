package org.dejaq.plugins.musictracker.ui.filter;

public enum MembersFilterOption
{
	ALL("All"),
	MEMBERS("Members"),
	FREE("Free");

	private final String displayLabel;

	MembersFilterOption(String displayLabel)
	{
		this.displayLabel = displayLabel;
	}

	@Override
	public String toString()
	{
		return displayLabel;
	}
}