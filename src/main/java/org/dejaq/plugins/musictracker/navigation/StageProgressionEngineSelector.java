package org.dejaq.plugins.musictracker.navigation;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.dejaq.plugins.musictracker.MusicTrackerConfig;

@Singleton
public class StageProgressionEngineSelector
{
	private final MusicTrackerConfig musicTrackerConfig;
	private final DefaultStageProgressionEngine defaultStageProgressionEngine;
	private final ProximityStageProgressionEngine proximityStageProgressionEngine;
	private final RelativeDistanceStageProgressionEngine relativeDistanceStageProgressionEngine;
	private final SegmentProjectionStageProgressionEngine segmentProjectionStageProgressionEngine;

	@Inject
	public StageProgressionEngineSelector(
		MusicTrackerConfig musicTrackerConfig,
		DefaultStageProgressionEngine defaultStageProgressionEngine,
		ProximityStageProgressionEngine proximityStageProgressionEngine,
		RelativeDistanceStageProgressionEngine relativeDistanceStageProgressionEngine,
		SegmentProjectionStageProgressionEngine segmentProjectionStageProgressionEngine)
	{
		this.musicTrackerConfig = musicTrackerConfig;
		this.defaultStageProgressionEngine = defaultStageProgressionEngine;
		this.proximityStageProgressionEngine = proximityStageProgressionEngine;
		this.relativeDistanceStageProgressionEngine = relativeDistanceStageProgressionEngine;
		this.segmentProjectionStageProgressionEngine = segmentProjectionStageProgressionEngine;
	}

	public StageProgressionEngine getActiveEngine()
	{
		ProgressionSystem progressionSystem = musicTrackerConfig.progressionSystem();
		if (progressionSystem == null)
		{
			return defaultStageProgressionEngine;
		}

		switch (progressionSystem)
		{
			case PROXIMITY:
				return proximityStageProgressionEngine;
			case RELATIVE_DISTANCE:
				return relativeDistanceStageProgressionEngine;
			case SEGMENT_PROJECTION:
				return segmentProjectionStageProgressionEngine;
			case DEFAULT:
			default:
				return defaultStageProgressionEngine;
		}
	}
}