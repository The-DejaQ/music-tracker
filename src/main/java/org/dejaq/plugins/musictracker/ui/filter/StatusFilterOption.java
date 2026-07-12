package org.dejaq.plugins.musictracker.ui.filter;

public enum StatusFilterOption
{
	ALL("All"),
	UNLOCKED("Unlocked"),
	LOCKED("Locked");

	private final String displayLabel;

	StatusFilterOption(String displayLabel)
	{
		this.displayLabel = displayLabel;
	}

	@Override
	public String toString()
	{
		return displayLabel;
	}
}