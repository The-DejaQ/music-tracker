package org.dejaq.plugins.musictracker.track.special;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.InteractionType;
import org.dejaq.plugins.musictracker.track.MusicTrackEntityPoint;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.TrackStep;

@Slf4j
public class QuetzalTrackHandler implements SpecialTrackHandler
{
	public static final String TALKASTI_PEOPLE_TRACK = "The Talkasti People";
	public static final String UNDER_THE_MOUNTAIN_TRACK = "Under the Mountain";

	private static final List<String> QUETZAL_ROUTE_NAMES = List.of("Quetzal");

	private static final String RENU_ENTITY_NAME = "Renu";
	private static final String TRAVEL_ACTION = "Travel";
	private static final int LANDING_SITE_ARRIVAL_RADIUS = 4;
	private static final String OVER_THE_MOUNTAINS_TRACK_TITLE = "Over the Mountains";
	private static final int UNLOCK_DESTINATION_REVERT_RADIUS = 85;
	private static final String DATA_KEY_QUETZAL = "quetzal";

	private int cachedTick = -1;
	private int cachedStageIndex = -1;
	private List<MusicTrackEntityPoint> cachedHighlights;

	@Override
	public List<String> getHandledRouteNames()
	{
		return QUETZAL_ROUTE_NAMES;
	}

	@Override
	public List<SpecialEntity> getHandledEntities()
	{
		return List.of(SpecialEntity.QUETZAL);
	}

	@Override
	public Integer getForcedStageIndex(MusicTrack musicTrack, Route route, int currentStageIndex, WorldPoint currentPlayerLocation, MusicTrackerPlugin musicTrackerPlugin)
	{
		if (route == null || currentPlayerLocation == null)
		{
			return null;
		}

		Client client = musicTrackerPlugin.getClient();
		if (!client.isClientThread())
		{
			return null;
		}

		TrackStep currentTrackStep = route.getStep(currentStageIndex);

		if (isQuetzalTravelStep(currentTrackStep))
		{
			return resolveForwardAdvanceFromTravelStep(musicTrack, route, currentStageIndex, currentTrackStep, currentPlayerLocation, client);
		}

		return resolveBackwardRevertToTravelStep(musicTrack, route, currentStageIndex, currentPlayerLocation);
	}

	private Integer resolveForwardAdvanceFromTravelStep(MusicTrack musicTrack, Route route, int currentStageIndex, TrackStep currentTrackStep, WorldPoint currentPlayerLocation, Client client)
	{
		int nextStageIndex = currentStageIndex + 1;
		if (nextStageIndex >= route.getStepCount())
		{
			return null;
		}

		Quetzal arrivalTarget = resolveArrivalTarget(musicTrack, currentTrackStep, client);
		if (arrivalTarget == null)
		{
			return null;
		}

		WorldPoint landingSiteLocation = arrivalTarget.getLandingSiteLocation();
		boolean arrivedAtLandingSite = currentPlayerLocation.distanceTo(landingSiteLocation) <= LANDING_SITE_ARRIVAL_RADIUS;

		return arrivedAtLandingSite ? nextStageIndex : null;
	}

	private Integer resolveBackwardRevertToTravelStep(MusicTrack musicTrack, Route route, int currentStageIndex, WorldPoint currentPlayerLocation)
	{
		int previousStageIndex = currentStageIndex - 1;
		if (previousStageIndex < 0)
		{
			return null;
		}

		TrackStep previousTrackStep = route.getStep(previousStageIndex);
		if (!isQuetzalTravelStep(previousTrackStep))
		{
			return null;
		}

		TrackStep currentTrackStep = route.getStep(currentStageIndex);
		if (currentTrackStep == null || currentTrackStep.getDestination() == null)
		{
			return null;
		}

		WorldPoint unlockDestination = currentTrackStep.getDestination();
		boolean farFromUnlockDestination = currentPlayerLocation.distanceTo(unlockDestination) >
			(musicTrack.getTitle().equals(OVER_THE_MOUNTAINS_TRACK_TITLE) ? 120 : UNLOCK_DESTINATION_REVERT_RADIUS); // best we can do...

		return farFromUnlockDestination ? previousStageIndex : null;
	}

	private boolean isQuetzalTravelStep(TrackStep trackStep)
	{
		if (trackStep == null)
		{
			return false;
		}
		for (InteractionTarget interactionTarget : trackStep.getAllHighlights())
		{
			if (SpecialEntity.fromToken(interactionTarget.getEntity()) == SpecialEntity.QUETZAL)
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasVolatileDynamicHighlights(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		return true;
	}

	@Override
	public List<MusicTrackEntityPoint> getDynamicEntityHighlights(MusicTrack musicTrack, Route route, TrackStep trackStep, int stageIndex, MusicTrackerPlugin musicTrackerPlugin)
	{
		if (route == null)
		{
			return null;
		}

		Client client = musicTrackerPlugin.getClient();
		if (!client.isClientThread())
		{
			log.warn("Quetzal getDynamicEntityHighlights called off the client thread; ignoring");
			return null;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return null;
		}

		if (!isQuetzalTravelStep(trackStep))
		{
			return null;
		}

		int currentTick = client.getTickCount();
		if (currentTick == cachedTick && stageIndex == cachedStageIndex && cachedHighlights != null)
		{
			return cachedHighlights;
		}

		boolean isVarbitGatedTrack = isVarbitGatedTrack(musicTrack);
		Quetzal departureQuetzal = resolveDepartureQuetzal(client, localPlayer.getWorldLocation());
		if (departureQuetzal == null)
		{
			cachedTick = currentTick;
			cachedStageIndex = stageIndex;
			cachedHighlights = null;
			return null;
		}

		musicTrackerPlugin.getTrackNavigator().getNavigationCoordinator().requestShortestPathTo(departureQuetzal.getLandingSiteLocation());

		String hintText;
		Quetzal arrivalTarget = resolveArrivalTarget(musicTrack, trackStep, client);
		String destinationName = arrivalTarget != null ? arrivalTarget.getDisplayName() : departureQuetzal.getDisplayName();
		String travelHint = TRAVEL_ACTION + " -> " + destinationName;
		hintText = isVarbitGatedTrack ? travelHint : resolveJsonHint(trackStep, travelHint);

		cachedTick = currentTick;
		cachedStageIndex = stageIndex;
		cachedHighlights = buildRenuHighlight(departureQuetzal.getLandingSiteLocation(), hintText);
		return cachedHighlights;
	}

	@Override
	public void reset()
	{
		cachedTick = -1;
		cachedStageIndex = -1;
		cachedHighlights = null;
	}

	private boolean isVarbitGatedTrack(MusicTrack musicTrack)
	{
		if (musicTrack == null || musicTrack.getTitle() == null)
		{
			return false;
		}
		return musicTrack.getTitle().equalsIgnoreCase(TALKASTI_PEOPLE_TRACK)
			|| musicTrack.getTitle().equalsIgnoreCase(UNDER_THE_MOUNTAIN_TRACK);
	}

	private String resolveJsonHint(TrackStep trackStep, String fallbackHint)
	{
		if (trackStep == null)
		{
			return fallbackHint;
		}
		for (InteractionTarget interactionTarget : trackStep.getAllHighlights())
		{
			if (SpecialEntity.fromToken(interactionTarget.getEntity()) == SpecialEntity.QUETZAL
				&& interactionTarget.getHint() != null && !interactionTarget.getHint().isBlank())
			{
				return interactionTarget.getHint();
			}
		}
		return fallbackHint;
	}

	private Quetzal resolveDepartureQuetzal(Client client, WorldPoint playerLocation)
	{
		return findNearestAvailableQuetzal(client, playerLocation);
	}

	private Quetzal resolveArrivalTarget(MusicTrack musicTrack, TrackStep trackStep, Client client)
	{
		Quetzal dataDrivenTarget = resolveDataDrivenTarget(trackStep);
		if (dataDrivenTarget != null)
		{
			return dataDrivenTarget;
		}

		if (musicTrack != null && musicTrack.getTitle() != null)
		{
			if (musicTrack.getTitle().equalsIgnoreCase(TALKASTI_PEOPLE_TRACK))
			{
				return isQuetzalAvailable(client, Quetzal.KASTORI) ? Quetzal.KASTORI : Quetzal.TAL_TEKLAN;
			}
			if (musicTrack.getTitle().equalsIgnoreCase(UNDER_THE_MOUNTAIN_TRACK))
			{
				return isQuetzalAvailable(client, Quetzal.CAM_TORUM) ? Quetzal.CAM_TORUM : Quetzal.THE_TEOMAT;
			}
		}

		return null;
	}

	private Quetzal resolveDataDrivenTarget(TrackStep trackStep)
	{
		if (trackStep == null)
		{
			return null;
		}
		for (InteractionTarget interactionTarget : trackStep.getAllHighlights())
		{
			if (SpecialEntity.fromToken(interactionTarget.getEntity()) != SpecialEntity.QUETZAL)
			{
				continue;
			}
			if (interactionTarget.getData() == null)
			{
				continue;
			}
			String quetzalName = interactionTarget.getData().get(DATA_KEY_QUETZAL);
			if (quetzalName == null || quetzalName.isBlank())
			{
				continue;
			}
			for (Quetzal quetzal : Quetzal.values())
			{
				if (quetzal.name().equalsIgnoreCase(quetzalName.trim()))
				{
					return quetzal;
				}
			}
		}
		return null;
	}

	private Quetzal findNearestAvailableQuetzal(Client client, WorldPoint playerLocation)
	{
		Quetzal nearestQuetzal = null;
		int nearestDistance = Integer.MAX_VALUE;

		for (Quetzal quetzal : Quetzal.values())
		{
			if (!isQuetzalAvailable(client, quetzal))
			{
				continue;
			}

			int distance = playerLocation.distanceTo(quetzal.getLandingSiteLocation());
			if (distance < nearestDistance)
			{
				nearestDistance = distance;
				nearestQuetzal = quetzal;
			}
		}

		return nearestQuetzal;
	}

	private boolean isQuetzalAvailable(Client client, Quetzal quetzal)
	{
		return !quetzal.requiresBuild() || client.getVarbitValue(quetzal.getBuildGateVarbitId()) > 0;
	}

	private List<MusicTrackEntityPoint> buildRenuHighlight(WorldPoint landingSiteLocation, String hintText)
	{
		InteractionTarget renuInteraction = InteractionTarget.builder()
			.entity(RENU_ENTITY_NAME)
			.location(landingSiteLocation)
			.type(InteractionType.NPC)
			.hint(hintText)
			.action(TRAVEL_ACTION)
			.build();

		return List.of(MusicTrackEntityPoint.from(landingSiteLocation, renuInteraction));
	}
}