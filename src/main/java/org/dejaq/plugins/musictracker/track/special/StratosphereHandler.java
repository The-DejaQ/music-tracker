package org.dejaq.plugins.musictracker.track.special;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.InteractionType;
import org.dejaq.plugins.musictracker.track.MusicTrackEntityPoint;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.TrackStep;

@Slf4j
public class StratosphereHandler implements SpecialTrackHandler
{
	private static final String ZANARIS_ROUTE_NAME = "Zanaris";

	private static final WorldPoint ZANARIS_CENTER = new WorldPoint(2432, 4422, 0);
	private static final int ZANARIS_RADIUS = 64;
	private static final int WALL_JUNCTION_X = 2430;
	private static final int WALL_JUNCTION_Y = 4400;

	private static final int JUTTING_WALL_66_REQUIRED_AGILITY_LEVEL = 66;
	private static final int JUTTING_WALL_46_REQUIRED_AGILITY_LEVEL = 46;

	private static final WorldPoint JUTTING_WALL_66_LOCATION = new WorldPoint(2409, 4401, 0);
	private static final WorldPoint JUTTING_WALL_46_LOCATION = new WorldPoint(2400, 4403, 0);
	private static final WorldPoint MYSTERIOUS_RUINS_LOCATION = new WorldPoint(2407, 4376, 0);

	private static final String JUTTING_WALL_ENTITY_NAME = "Jutting wall";
	private static final String MYSTERIOUS_RUINS_ENTITY_NAME = "Mysterious ruins";

	@Getter
	@RequiredArgsConstructor
	private enum StratospherePhase
	{
		JUTTING_WALL_66(JUTTING_WALL_66_LOCATION, JUTTING_WALL_ENTITY_NAME, "66 Agility"),
		JUTTING_WALL_46(JUTTING_WALL_46_LOCATION, JUTTING_WALL_ENTITY_NAME, "46 Agility"),
		MYSTERIOUS_RUINS(MYSTERIOUS_RUINS_LOCATION, MYSTERIOUS_RUINS_ENTITY_NAME, "Enter / Use talisman on");

		private final WorldPoint location;
		private final String entityName;
		private final String hintText;
	}

	private int cachedTick = -1;
	private int cachedStageIndex = -1;
	private List<MusicTrackEntityPoint> cachedHighlights;

	@Override
	public String getHandledRouteName()
	{
		return ZANARIS_ROUTE_NAME;
	}

	@Override
	public boolean hasVolatileDynamicHighlights(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		return true;
	}

	@Override
	public List<MusicTrackEntityPoint> getDynamicEntityHighlights(MusicTrack musicTrack, Route route, TrackStep trackStep, int stageIndex, MusicTrackerPlugin musicTrackerPlugin)
	{
		Client client = musicTrackerPlugin.getClient();

		if (!client.isClientThread())
		{
			musicTrackerPlugin.getClientThread().invokeLater(() ->
				getDynamicEntityHighlights(musicTrack, route, trackStep, stageIndex, musicTrackerPlugin));
			return cachedHighlights;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return null;
		}

		WorldPoint playerLocation = localPlayer.getWorldLocation();
		if (playerLocation.distanceTo(ZANARIS_CENTER) > ZANARIS_RADIUS)
		{
			return null;
		}

		int currentTick = client.getTickCount();
		if (currentTick == cachedTick && stageIndex == cachedStageIndex && cachedHighlights != null)
		{
			return cachedHighlights;
		}

		int currentAgilityLevel = getCurrentAgilityLevel(musicTrackerPlugin);
		StratospherePhase currentPhase = resolveCurrentPhase(playerLocation, currentAgilityLevel);

		List<MusicTrackEntityPoint> computedHighlights = buildHighlightForPhase(currentPhase);

		musicTrackerPlugin.getTrackNavigator().getNavigationCoordinator().requestShortestPathTo(currentPhase.getLocation());

		cachedTick = currentTick;
		cachedStageIndex = stageIndex;
		cachedHighlights = computedHighlights;
		return computedHighlights;
	}

	private StratospherePhase resolveCurrentPhase(WorldPoint playerLocation, int currentAgilityLevel)
	{
		if (hasOvershotIntoRuinsZone(playerLocation))
		{
			return StratospherePhase.MYSTERIOUS_RUINS;
		}

		if (currentAgilityLevel >= JUTTING_WALL_66_REQUIRED_AGILITY_LEVEL)
		{
			return StratospherePhase.JUTTING_WALL_66;
		}
		if (currentAgilityLevel >= JUTTING_WALL_46_REQUIRED_AGILITY_LEVEL)
		{
			return StratospherePhase.JUTTING_WALL_46;
		}
		return StratospherePhase.MYSTERIOUS_RUINS;
	}

	private boolean hasOvershotIntoRuinsZone(WorldPoint playerLocation)
	{
		return playerLocation.getX() < WALL_JUNCTION_X && playerLocation.getY() < WALL_JUNCTION_Y;
	}

	private int getCurrentAgilityLevel(MusicTrackerPlugin musicTrackerPlugin)
	{
		return musicTrackerPlugin.getClient().getRealSkillLevel(Skill.AGILITY);
	}

	private List<MusicTrackEntityPoint> buildHighlightForPhase(StratospherePhase phase)
	{
		InteractionTarget target = InteractionTarget.builder()
			.entityId(-1)
			.entity(phase.getEntityName())
			.location(phase.getLocation())
			.type(InteractionType.GAME_OBJECT)
			.hint(phase.getHintText())
			.build();

		return List.of(MusicTrackEntityPoint.from(phase.getLocation(), target));
	}
}
