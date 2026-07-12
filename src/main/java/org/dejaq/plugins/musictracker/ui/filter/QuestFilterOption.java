package org.dejaq.plugins.musictracker.ui.filter;

public enum QuestFilterOption
{
	ALL("All"),
	UNLOCKED_BY("Unlocked By"),
	REQUIRED("Required"),
	NONE("None");

	private final String displayLabel;

	QuestFilterOption(String displayLabel)
	{
		this.displayLabel = displayLabel;
	}

	@Override
	public String toString()
	{
		return displayLabel;
	}
}