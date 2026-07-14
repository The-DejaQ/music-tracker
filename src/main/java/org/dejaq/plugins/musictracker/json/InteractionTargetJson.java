package org.dejaq.plugins.musictracker.json;

import java.util.List;
import java.util.Map;

public class InteractionTargetJson
{
	public int entityId = -1;
	public String entity;
	public WorldPointJson location;
	public String type;
	public String hint;
	public List<String> actions;
	public Integer searchRadius;
	public Map<String, String> data;
}