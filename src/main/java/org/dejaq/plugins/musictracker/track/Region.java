package org.dejaq.plugins.musictracker.track;

import java.util.Locale;

public enum Region
{
	MISTHALIN,
	ASGARNIA,
	KANDARIN,
	KARAMJA,
	MORYTANIA,
	KHARIDIAN_DESERT,
	FREMENNIK_PROVINCE,
	TIRANNWN,
	WILDERNESS,
	GREAT_KOUREND,
	VARLAMORE,
	KEBOS_LOWLANDS,
	OTHER,
	UNKNOWN,
	QUESTS,
	HOLIDAY,
	SAILING;

	public String getDisplayName()
	{
		return formatWords(" ");
	}

	public String getResourceFileBaseName()
	{
		return formatWords("");
	}

	private String formatWords(String wordSeparator)
	{
		String[] words = name().toLowerCase(Locale.ROOT).split("_");
		StringBuilder formattedNameBuilder = new StringBuilder();

		for (int wordIndex = 0; wordIndex < words.length; wordIndex++)
		{
			if (words[wordIndex].isEmpty())
			{
				continue;
			}
			formattedNameBuilder.append(Character.toUpperCase(words[wordIndex].charAt(0)));
			if (words[wordIndex].length() > 1)
			{
				formattedNameBuilder.append(words[wordIndex].substring(1));
			}
			if (wordIndex < words.length - 1)
			{
				formattedNameBuilder.append(wordSeparator);
			}
		}

		return formattedNameBuilder.toString();
	}
}