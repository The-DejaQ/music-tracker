package org.dejaq.plugins.musictracker.track.special;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;

@Getter
public enum Quetzal
{

	ALDARIN("Aldarin", new WorldPoint(1388, 2898, 0), -1),
	AUBURNVALE("Auburnvale", new WorldPoint(1410, 3362, 0), -1),
	CIVITAS_ILLA_FORTIS("Civitas illa Fortis", new WorldPoint(1696, 3141, 0), -1),
	HUNTER_GUILD("Hunter Guild", new WorldPoint(1584, 3054, 0), -1),
	QUETZACALLI_GORGE("Quetzacalli Gorge", new WorldPoint(1511, 3221, 0), -1),
	SUNSET_COAST("Sunset Coast", new WorldPoint(1547, 2996, 0), -1),
	TAL_TEKLAN("Tal Teklan", new WorldPoint(1225, 3088, 0), -1),
	THE_TEOMAT("The Teomat", new WorldPoint(1436, 3168, 0), -1),

	CAM_TORUM("Cam Torum", new WorldPoint(1447, 3107, 0), VarbitID.QUETZAL_CAMTORUM),
	COLOSSAL_WYRM_REMAINS("Colossal Wyrm Remains", new WorldPoint(1671, 2932, 0), VarbitID.QUETZAL_COLOSSALWYRM),
	FORTIS_COLOSSEUM("Fortis Colosseum", new WorldPoint(1776, 3110, 0), VarbitID.QUETZAL_COLOSSEUM),
	KASTORI("Kastori", new WorldPoint(1343, 3019, 0), VarbitID.QUETZAL_KASTORI),
	OUTER_FORTIS("Outer Fortis", new WorldPoint(1701, 3036, 0), VarbitID.QUETZAL_OUTERFORTIS),
	SALVAGER_OVERLOOK("Salvager Overlook", new WorldPoint(1612, 3301, 0), -1);

	private final String displayName;
	private final WorldPoint landingSiteLocation;
	private final int buildGateVarbitId;

	Quetzal(String displayName, WorldPoint landingSiteLocation, int buildGateVarbitId)
	{
		this.displayName = displayName;
		this.landingSiteLocation = landingSiteLocation;
		this.buildGateVarbitId = buildGateVarbitId;
	}

	public boolean requiresBuild()
	{
		return buildGateVarbitId != -1;
	}
}