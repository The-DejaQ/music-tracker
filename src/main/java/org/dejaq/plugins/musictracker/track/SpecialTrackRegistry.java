package org.dejaq.plugins.musictracker.track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.track.special.AbyssTrackHandler;
import org.dejaq.plugins.musictracker.track.special.AztecHandler;
import org.dejaq.plugins.musictracker.track.special.DagannothDawnHandler;
import org.dejaq.plugins.musictracker.track.special.DefaultTrackHandler;
import org.dejaq.plugins.musictracker.track.special.DesolateIsleHandler;
import org.dejaq.plugins.musictracker.track.special.FairyRingHandler;
import org.dejaq.plugins.musictracker.track.special.MawsJawsClawsHandler;
import org.dejaq.plugins.musictracker.track.special.MorUlRekHandler;
import org.dejaq.plugins.musictracker.track.special.MysteriousRuinsHandler;
import org.dejaq.plugins.musictracker.track.special.QuetzalTrackHandler;
import org.dejaq.plugins.musictracker.track.special.RaceAgainstClockHandler;
import org.dejaq.plugins.musictracker.track.special.SpecialEntity;
import org.dejaq.plugins.musictracker.track.special.SpecialTrackHandler;
import org.dejaq.plugins.musictracker.track.special.StratosphereHandler;
import org.dejaq.plugins.musictracker.track.special.TitleFightHandler;
import org.dejaq.plugins.musictracker.track.special.WarriorsGuildHandler;

public class SpecialTrackRegistry
{
	private static final SpecialTrackHandler DEFAULT_TRACK_HANDLER = new DefaultTrackHandler();
	private static final Map<String, List<SpecialTrackHandler>> SPECIAL_TRACK_HANDLERS = new HashMap<>();
	private static final Map<SpecialEntity, SpecialTrackHandler> ENTITY_TRACK_HANDLERS = new EnumMap<>(SpecialEntity.class);

	static
	{
		register("Aztec", new AztecHandler());

		register("Race Against the Clock", new RaceAgainstClockHandler());

		register("Maws Jaws & Claws", new MawsJawsClawsHandler());

		register("Dagannoth Dawn", new DagannothDawnHandler());
		register("The Desolate Isle", new DesolateIsleHandler());

		register("Title Fight", new TitleFightHandler());

		register("Mor Ul Rek", new MorUlRekHandler());
		register("Inferno", new MorUlRekHandler());

		register("Warriors' Guild", new WarriorsGuildHandler());

		SpecialTrackHandler quetzalTrackHandler = new QuetzalTrackHandler();
		register(QuetzalTrackHandler.TALKASTI_PEOPLE_TRACK, quetzalTrackHandler);
		register(QuetzalTrackHandler.UNDER_THE_MOUNTAIN_TRACK, quetzalTrackHandler);
		registerEntityHandler(SpecialEntity.QUETZAL, quetzalTrackHandler);

		registerEntityHandler(SpecialEntity.FAIRY_RING, new FairyRingHandler());

		//register("Alchemical Attack!", new AlchemicalAttackHandler());

		// shared for abyss routes
		SpecialTrackHandler abyssTrackHandler = new AbyssTrackHandler();
		register(MysteriousRuinsHandler.AIR_ALTAR_TRACK, abyssTrackHandler);
		register(MysteriousRuinsHandler.MIND_ALTAR_TRACK, abyssTrackHandler);
		register(MysteriousRuinsHandler.WATER_ALTAR_TRACK, abyssTrackHandler);
		register(MysteriousRuinsHandler.EARTH_ALTAR_TRACK, abyssTrackHandler);
		register(MysteriousRuinsHandler.FIRE_ALTAR_TRACK, abyssTrackHandler);
		register(MysteriousRuinsHandler.BODY_ALTAR_TRACK, abyssTrackHandler);
		register(MysteriousRuinsHandler.COSMIC_ALTAR_TRACK, abyssTrackHandler);
		register(MysteriousRuinsHandler.CHAOS_ALTAR_TRACK, abyssTrackHandler);
		register(MysteriousRuinsHandler.NATURE_ALTAR_TRACK, abyssTrackHandler);
		register(MysteriousRuinsHandler.LAW_ALTAR_TRACK, abyssTrackHandler);
		register(MysteriousRuinsHandler.DEATH_ALTAR_TRACK, abyssTrackHandler);
		register(MysteriousRuinsHandler.SOUL_ALTAR_TRACK, abyssTrackHandler);

		register(MysteriousRuinsHandler.COSMIC_ALTAR_TRACK, new StratosphereHandler());
	}

	public static void register(String trackTitle, SpecialTrackHandler specialTrackHandler)
	{
		SPECIAL_TRACK_HANDLERS.computeIfAbsent(trackTitle.toLowerCase(), unusedTrackTitleKey -> new ArrayList<>()).add(specialTrackHandler);
	}

	public static void registerEntityHandler(SpecialEntity specialEntity, SpecialTrackHandler specialTrackHandler)
	{
		ENTITY_TRACK_HANDLERS.put(specialEntity, specialTrackHandler);
	}

	public static SpecialTrackHandler getHandler(MusicTrack musicTrack, Route route)
	{
		if (musicTrack == null || musicTrack.getTitle() == null)
		{
			return DEFAULT_TRACK_HANDLER;
		}

		SpecialTrackHandler titleFallbackHandler = null;

		List<SpecialTrackHandler> candidateHandlers = SPECIAL_TRACK_HANDLERS.get(musicTrack.getTitle().toLowerCase());
		if (candidateHandlers != null)
		{
			for (SpecialTrackHandler handler : candidateHandlers)
			{
				List<String> routeNames = handler.getHandledRouteNames();

				if (routeNames == null || routeNames.isEmpty())
				{
					titleFallbackHandler = handler;
					continue;
				}

				if (route != null && routeNames.contains(route.getName()))
				{
					return handler;
				}
			}
		}

		SpecialTrackHandler entityHandler = findEntityHandlerForRoute(route);
		if (entityHandler != null)
		{
			return entityHandler;
		}

		return titleFallbackHandler != null ? titleFallbackHandler : DEFAULT_TRACK_HANDLER;
	}

	private static SpecialTrackHandler findEntityHandlerForRoute(Route route)
	{
		if (route == null || ENTITY_TRACK_HANDLERS.isEmpty() || !route.hasSteps())
		{
			return null;
		}

		for (TrackStep trackStep : route.getTrackSteps())
		{
			if (trackStep == null)
			{
				continue;
			}
			for (InteractionTarget interactionTarget : trackStep.getAllHighlights())
			{
				SpecialEntity specialEntity = SpecialEntity.fromToken(interactionTarget.getEntity());
				if (specialEntity != null)
				{
					SpecialTrackHandler entityHandler = ENTITY_TRACK_HANDLERS.get(specialEntity);
					if (entityHandler != null)
					{
						return entityHandler;
					}
				}
			}
		}

		return null;
	}

	public static void resetAll()
	{
		DEFAULT_TRACK_HANDLER.reset();

		Set<SpecialTrackHandler> uniqueHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
		for (List<SpecialTrackHandler> handlers : SPECIAL_TRACK_HANDLERS.values())
		{
			uniqueHandlers.addAll(handlers);
		}
		uniqueHandlers.addAll(ENTITY_TRACK_HANDLERS.values());

		for (SpecialTrackHandler handler : uniqueHandlers)
		{
			handler.reset();
		}
	}
}