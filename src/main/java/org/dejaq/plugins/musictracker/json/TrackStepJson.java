package org.dejaq.plugins.musictracker.json;

import java.util.List;

public class TrackStepJson
{
	public String name;
	public WorldPointJson destination;
	public InteractionTargetJson interaction;
	public List<InteractionTargetJson> interactions;
}