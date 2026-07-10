package org.dejaq.plugins.musictracker.track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.track.special.AbyssTrackHandler;
import org.dejaq.plugins.musictracker.track.special.AztecHandler;
import org.dejaq.plugins.musictracker.track.special.DefaultTrackHandler;
import org.dejaq.plugins.musictracker.track.special.DesolateIsleHandler;
import org.dejaq.plugins.musictracker.track.special.MawsJawsClawsHandler;
import org.dejaq.plugins.musictracker.track.special.MorUlRekHandler;
import org.dejaq.plugins.musictracker.track.special.RaceAgainstClockHandler;
import org.dejaq.plugins.musictracker.track.special.SpecialTrackHandler;
import org.dejaq.plugins.musictracker.track.special.StratosphereHandler;
import org.dejaq.plugins.musictracker.track.special.TitleFightHandler;
import org.dejaq.plugins.musictracker.track.special.WarriorsGuildHandler;

public class SpecialTrackRegistry
{
	private static final SpecialTrackHandler DEFAULT_TRACK_HANDLER = new DefaultTrackHandler();
	private static final Map<String, List<SpecialTrackHandler>> SPECIAL_TRACK_HANDLERS = new HashMap<>();

	static
	{
		register("Aztec", new AztecHandler());

		register("Race Against the Clock", new RaceAgainstClockHandler());

		register("Maws Jaws & Claws", new MawsJawsClawsHandler());

		register("The Desolate Isle", new DesolateIsleHandler());

		register("Title Fight", new TitleFightHandler());

		register("Mor Ul Rek", new MorUlRekHandler());
		register("Inferno", new MorUlRekHandler());

		register("Warriors' Guild", new WarriorsGuildHandler());

		//register("Alchemical Attack!", new AlchemicalAttackHandler());

		// shared for abyss routes
		SpecialTrackHandler abyssTrackHandler = new AbyssTrackHandler();
		register(AbyssTrackHandler.AIR_ALTAR_TRACK, abyssTrackHandler);
		register(AbyssTrackHandler.MIND_ALTAR_TRACK, abyssTrackHandler);
		register(AbyssTrackHandler.WATER_ALTAR_TRACK, abyssTrackHandler);
		register(AbyssTrackHandler.EARTH_ALTAR_TRACK, abyssTrackHandler);
		register(AbyssTrackHandler.FIRE_ALTAR_TRACK, abyssTrackHandler);
		register(AbyssTrackHandler.BODY_ALTAR_TRACK, abyssTrackHandler);
		register(AbyssTrackHandler.COSMIC_ALTAR_TRACK, abyssTrackHandler);
		register(AbyssTrackHandler.CHAOS_ALTAR_TRACK, abyssTrackHandler);
		register(AbyssTrackHandler.NATURE_ALTAR_TRACK, abyssTrackHandler);
		register(AbyssTrackHandler.LAW_ALTAR_TRACK, abyssTrackHandler);
		register(AbyssTrackHandler.DEATH_ALTAR_TRACK, abyssTrackHandler);
		register(AbyssTrackHandler.SOUL_ALTAR_TRACK, abyssTrackHandler);

		register(AbyssTrackHandler.COSMIC_ALTAR_TRACK, new StratosphereHandler());
	}

	public static void register(String trackTitle, SpecialTrackHandler specialTrackHandler)
	{
		SPECIAL_TRACK_HANDLERS.computeIfAbsent(trackTitle.toLowerCase(), unusedTrackTitleKey -> new ArrayList<>()).add(specialTrackHandler);
	}

	public static SpecialTrackHandler getHandler(MusicTrack musicTrack, Route route)
	{
		if (musicTrack == null || musicTrack.getTitle() == null)
		{
			return DEFAULT_TRACK_HANDLER;
		}

		List<SpecialTrackHandler> candidateHandlers = SPECIAL_TRACK_HANDLERS.get(musicTrack.getTitle().toLowerCase());
		if (candidateHandlers == null || candidateHandlers.isEmpty())
		{
			return DEFAULT_TRACK_HANDLER;
		}

		SpecialTrackHandler fallbackHandler = null;
		for (SpecialTrackHandler candidateHandler : candidateHandlers)
		{
			String handledRouteName = candidateHandler.getHandledRouteName();
			if (handledRouteName == null)
			{
				fallbackHandler = candidateHandler;
				continue;
			}
			if (route != null && route.isRoute(handledRouteName))
			{
				return candidateHandler;
			}
		}

		return fallbackHandler != null ? fallbackHandler : DEFAULT_TRACK_HANDLER;
	}

	public static void resetAll()
	{
		DEFAULT_TRACK_HANDLER.reset();

		Set<SpecialTrackHandler> uniqueHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
		for (List<SpecialTrackHandler> handlers : SPECIAL_TRACK_HANDLERS.values())
		{
			uniqueHandlers.addAll(handlers);
		}

		for (SpecialTrackHandler handler : uniqueHandlers)
		{
			handler.reset();
		}
	}
}