package org.dejaq.plugins.musictracker.track;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.WorldPoint;

@Data
@AllArgsConstructor
public class MusicTrackEntityPoint
{

	private final int regionId;
	private final int worldX;
	private final int worldY;
	private final int plane;
	private final int entityId;
	private final String entity;
	private final InteractionType interactionType;

	@Getter
	@Setter
	private InteractionTarget interactionTarget;

	public static MusicTrackEntityPoint from(WorldPoint worldPoint, int entityId, InteractionType type)
	{
		return new MusicTrackEntityPoint(
			worldPoint.getRegionID(), worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane(),
			entityId, null, type, null
		);
	}

	public static MusicTrackEntityPoint from(WorldPoint worldPoint, String entity, InteractionType type)
	{
		return new MusicTrackEntityPoint(
			worldPoint.getRegionID(), worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane(),
			-1, entity, type, null
		);
	}

	public static MusicTrackEntityPoint from(WorldPoint worldPoint, InteractionTarget interaction)
	{
		if (interaction == null)
		{
			return new MusicTrackEntityPoint(
				worldPoint.getRegionID(), worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane(),
				-1, null, null, null
			);
		}

		WorldPoint effectiveLocation = interaction.getLocation() != null
			? interaction.getLocation()
			: worldPoint;

		if (interaction.getEntityId() > 0)
		{
			return new MusicTrackEntityPoint(
				effectiveLocation.getRegionID(),
				effectiveLocation.getX(),
				effectiveLocation.getY(),
				effectiveLocation.getPlane(),
				interaction.getEntityId(),
				null,
				interaction.getType(),
				interaction
			);
		}
		else
		{
			return new MusicTrackEntityPoint(
				effectiveLocation.getRegionID(),
				effectiveLocation.getX(),
				effectiveLocation.getY(),
				effectiveLocation.getPlane(),
				-1,
				interaction.getEntity(),
				interaction.getType(),
				interaction
			);
		}
	}
}