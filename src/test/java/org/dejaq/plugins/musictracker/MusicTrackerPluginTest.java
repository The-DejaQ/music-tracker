package org.dejaq.plugins.musictracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class MusicTrackerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MusicTrackerPlugin.class);
		RuneLite.main(args);
	}
}