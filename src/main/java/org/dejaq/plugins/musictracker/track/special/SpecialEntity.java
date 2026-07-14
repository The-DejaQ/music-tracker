package org.dejaq.plugins.musictracker.track.special;

public enum SpecialEntity
{
	QUETZAL,
	FAIRY_RING;

	public static SpecialEntity fromToken(String token)
	{
		if (token == null || token.isBlank())
		{
			return null;
		}
		for (SpecialEntity specialEntity : values())
		{
			if (specialEntity.name().equalsIgnoreCase(token.trim()))
			{
				return specialEntity;
			}
		}
		return null;
	}
}