package org.dejaq.plugins.musictracker.track.special;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.InteractionType;
import org.dejaq.plugins.musictracker.track.MusicTrackEntityPoint;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.TrackStep;

@Slf4j
public class AztecHandler implements SpecialTrackHandler
{
	private static final WorldPoint CAPN_IZZY_LOCATION = new WorldPoint(2807, 3191, 0);
	private static final WorldPoint LADDER_LOCATION = new WorldPoint(2809, 3194, 0);

	private static final String CAPN_IZZY_NPC_NAME = "Cap'n Izzy No-Beard";
	private static final String LADDER_OBJECT_NAME = "Ladder";
	private static final String PAY_TOLL_HINT_TEXT = "Pay (200 coins)";
	private static final String CLIMB_DOWN_HINT_TEXT = "Climb down";
	private static final String CLIMB_DOWN_ACTION = "climb-down";

	private int cachedTick = -1;
	private List<MusicTrackEntityPoint> cachedHighlights;

	@Override
	public boolean hasVolatileDynamicHighlights(MusicTrack musicTrack, Route route, MusicTrackerPlugin musicTrackerPlugin)
	{
		return true;
	}

	@Override
	public List<MusicTrackEntityPoint> getDynamicEntityHighlights(MusicTrack musicTrack, Route route, TrackStep trackStep, int stageIndex, MusicTrackerPlugin musicTrackerPlugin)
	{
		if (route == null || trackStep == null)
		{
			return null;
		}

		Client client = musicTrackerPlugin.getClient();
		if (!client.isClientThread())
		{
			log.warn("getDynamicEntityHighlights called off the client thread; ignoring");
			return null;
		}

		int currentTick = client.getTickCount();
		if (currentTick == cachedTick && cachedHighlights != null)
		{
			return cachedHighlights;
		}

		boolean hasPaidTollToEnter = client.getVarbitValue(VarbitID.AGILITYARENA_CANENTER) > 0;

		cachedHighlights = hasPaidTollToEnter ? buildLadderHighlights() : buildCapnIzzyHighlights();
		cachedTick = currentTick;
		return cachedHighlights;
	}

	@Override
	public void reset()
	{
		cachedTick = -1;
		cachedHighlights = null;
	}

	private List<MusicTrackEntityPoint> buildCapnIzzyHighlights()
	{
		InteractionTarget capnIzzyInteraction = InteractionTarget.builder()
			.entity(CAPN_IZZY_NPC_NAME)
			.location(CAPN_IZZY_LOCATION)
			.type(InteractionType.NPC)
			.hint(PAY_TOLL_HINT_TEXT)
			.build();

		return List.of(MusicTrackEntityPoint.from(CAPN_IZZY_LOCATION, capnIzzyInteraction));
	}

	private List<MusicTrackEntityPoint> buildLadderHighlights()
	{
		InteractionTarget ladderInteraction = InteractionTarget.builder()
			.entity(LADDER_OBJECT_NAME)
			.location(LADDER_LOCATION)
			.type(InteractionType.GAME_OBJECT)
			.hint(CLIMB_DOWN_HINT_TEXT)
			.action(CLIMB_DOWN_ACTION)
			.build();

		return List.of(MusicTrackEntityPoint.from(LADDER_LOCATION, ladderInteraction));
	}
}