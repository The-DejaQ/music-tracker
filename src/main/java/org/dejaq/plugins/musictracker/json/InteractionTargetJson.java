package org.dejaq.plugins.musictracker.json;

import java.util.List;

public class InteractionTargetJson
{
	public int entityId = -1;
	public String entity;
	public WorldPointJson location;
	public String type;
	public String hint;
	public List<String> actions;
	public Integer searchRadius;
}