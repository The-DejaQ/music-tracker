package org.dejaq.plugins.musictracker;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("music-tracker")
public interface MusicTrackerConfig extends Config
{
	@ConfigSection(
		name = "General",
		description = "General settings",
		position = 0
	)
	String generalSection = "general";

	@ConfigItem(
		keyName = "showProgress",
		name = "Show Progress in Regions",
		description = "Toggles Progress bars in Region headers",
		position = 1,
		section = generalSection
	)
	default boolean showProgress()
	{
		return true;
	}

	@ConfigItem(
		keyName = "saveSkippedTracks",
		name = "Save skipped Music Tracks",
		description = "Remembers which music tracks you've skipped between sessions",
		position = 2,
		section = generalSection
	)
	default boolean saveSkippedTracks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skippedTracks",
		name = "",
		description = "",
		hidden = true
	)
	default String skippedTracks()
	{
		return "";
	}

	@ConfigItem(
		keyName = "useShortestPath",
		name = "Use Shortest Path plugin",
		description = "Send path requests to the Shortest Path plugin when available",
		position = 3,
		section = generalSection
	)
	default boolean useShortestPath()
	{
		return true;
	}

	@ConfigSection(
		name = "Progression",
		description = "How auto-progression picks which track to move to next",
		position = 1,
		closedByDefault = true
	)
	String progressionSection = "progression";

	@ConfigItem(
		keyName = "autoProgress",
		name = "Auto Progress",
		description = "Automatically move to the next track after unlocking one",
		position = 0,
		section = progressionSection
	)
	default boolean autoProgress()
	{
		return true;
	}

	@ConfigItem(
		keyName = "stayInRegion",
		name = "Stay in Region",
		description = "Prefer the next eligible track in your current region over a closer one in another region; only moves to another region once the current one has none left",
		position = 1,
		section = progressionSection
	)
	default boolean stayInRegion()
	{
		return true;
	}

	@ConfigItem(
		keyName = "statusFilter",
		name = "",
		description = "",
		hidden = true
	)
	default String statusFilter()
	{
		return "ALL";
	}

	@ConfigItem(
		keyName = "membersFilter",
		name = "",
		description = "",
		hidden = true
	)
	default String membersFilter()
	{
		return "ALL";
	}

	@ConfigItem(
		keyName = "questsFilter",
		name = "",
		description = "",
		hidden = true
	)
	default String questsFilter()
	{
		return "ALL";
	}

	@ConfigItem(
		keyName = "hideMissingLevel",
		name = "",
		description = "",
		hidden = true
	)
	default boolean hideMissingLevel()
	{
		return false;
	}

	@ConfigItem(
		keyName = "legacyFiltersMigrated",
		name = "",
		description = "",
		hidden = true
	)
	default boolean legacyFiltersMigrated()
	{
		return false;
	}

	@ConfigSection(
		name = "Messages",
		description = "Message settings",
		position = 3,
		closedByDefault = true
	)
	String messagesSection = "messages";

	@ConfigItem(
		keyName = "notifyOnUnlock",
		name = "Notify on Unlock",
		description = "Send a notification when a music track is unlocked",
		position = 0,
		section = messagesSection,
		hidden = true
	)
	default boolean notifyOnUnlock()
	{
		return true;
	}

	@ConfigItem(
		keyName = "messageNameColor",
		name = "Music Tracker message name color",
		description = "The color of the name when Music Tracker sends a message",
		position = 1,
		section = messagesSection
	)
	default Color messageNameColor()
	{
		return Color.BLUE;
	}

	@ConfigItem(
		keyName = "messageTextColor",
		name = "Music Tracker message text color",
		description = "The color of the text when Music Tracker sends a message",
		position = 2,
		section = messagesSection
	)
	default Color messageTextColor()
	{
		return Color.BLACK;
	}

	@ConfigSection(
		name = "Overlay",
		description = "Overlay options",
		position = 4,
		closedByDefault = true
	)
	String overlaySection = "overlayOptions";

	@ConfigItem(
		keyName = "showRegionOverlay",
		name = "Show Region",
		description = "Shows the region name of the currently tracked target",
		position = 0,
		section = overlaySection
	)
	default boolean showRegionOverlay()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showLocationOverlay",
		name = "Show Location",
		description = "Shows the location of the currently tracked target",
		position = 1,
		section = overlaySection
	)
	default boolean showLocationOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showQuestOverlay",
		name = "Show Quest Required",
		description = "Shows the quest required to reach the currently tracked target",
		position = 2,
		section = overlaySection
	)
	default boolean showQuestOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showItemsOverlay",
		name = "Show Items Required",
		description = "Shows the items required to reach the currently tracked target",
		position = 3,
		section = overlaySection
	)
	default boolean showItemsOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "recommendedItemsOverlay",
		name = "Show Recommended Items",
		description = "Shows the recommended items to reach the currently tracked target",
		position = 4,
		section = overlaySection
	)
	default boolean recommendedItemsOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showLevelsOverlay",
		name = "Show Levels Required",
		description = "Shows the levels required to reach the currently tracked target",
		position = 5,
		section = overlaySection
	)
	default boolean showLevelsOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "recommendedLevelsOverlay",
		name = "Show Recommended Levels",
		description = "Shows the levels recommended to reach the currently tracked target",
		position = 6,
		section = overlaySection
	)
	default boolean recommendedLevelsOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showNotesOverlay",
		name = "Show Notes",
		description = "Shows notes for the currently tracked target",
		position = 7,
		section = overlaySection
	)
	default boolean showNotesOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showUnlockHintOverlay",
		name = "Show Unlock Hint",
		description = "Shows the unlock hint for the currently tracked target",
		position = 8,
		section = overlaySection
	)
	default boolean showUnlockHintOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDistanceOverlay",
		name = "Show Distance",
		description = "Shows the distance to the currently tracked target",
		position = 9,
		section = overlaySection
	)
	default boolean showDistanceOverlay()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showNextTrackOverlay",
		name = "Show Next Track",
		description = "Shows the next music track to be tracked",
		position = 10,
		section = overlaySection
	)
	default boolean showNextTrackOverlay()
	{
		return true;
	}

	@ConfigSection(
		name = "Entity Highlighting",
		description = "Appearance of the on-screen highlight for the current step's NPC, object, or tile",
		position = 5,
		closedByDefault = true
	)
	String entityHighlightSection = "entityHighlight";

	@Alpha
	@ConfigItem(
		keyName = "entityHighlightFillColor",
		name = "Highlight Fill Color",
		description = "The fill color used to highlight the NPC, object, or tile for the current step",
		position = 0,
		section = entityHighlightSection
	)
	default Color entityHighlightFillColor()
	{
		return new Color(255, 200, 50, 90);
	}

	@ConfigItem(
		keyName = "entityHighlightOutlineColor",
		name = "Highlight Outline Color",
		description = "The outline and hint-text color used to highlight the NPC, object, or tile for the current step",
		position = 1,
		section = entityHighlightSection
	)
	default Color entityHighlightOutlineColor()
	{
		return new Color(255, 200, 50);
	}

	@ConfigSection(
		name = "World Map",
		description = "World map harp icon overlay settings",
		position = 6,
		closedByDefault = true
	)
	String worldMapSection = "worldMap";

	@ConfigItem(
		keyName = "showWorldMapOverlay",
		name = "Show World Map Icons",
		description = "Shows a harp icon on the world map at each music track's unlock location",
		position = 0,
		section = worldMapSection
	)
	default boolean showWorldMapOverlay()
	{
		return false;
	}

	@ConfigItem(
		keyName = "worldMapClickToTrack",
		name = "Click to start tracking",
		description = "Right-clicking a harp icon on the world map starts tracking that music track",
		position = 1,
		section = worldMapSection
	)
	default boolean worldMapClickToTrack()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showWorldMapPoints",
		name = "Display world map point",
		description = "Show a Music Cape icon on the world map for the current target",
		position = 2,
		section = worldMapSection
	)
	default boolean showWorldMapPoints()
	{
		return true;
	}

	@ConfigSection(
		name = "Minimap",
		description = "Minimap options",
		position = 7,
		closedByDefault = true
	)
	String minimapSection = "minimap";


	@ConfigItem(
		keyName = "showMinimapArrow",
		name = "Show Minimap Arrow",
		description = "Shows a minimap hint arrow pointing at the current step's destination",
		position = 0,
		section = minimapSection
	)
	default boolean showMinimapArrow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "staticMinimapArrow",
		name = "Static Minimap Arrow",
		description = "If the Minimap arrow appears as static or breathes",
		position = 1,
		section = minimapSection
	)
	default boolean minimapArrowStatic()
	{
		return true;
	}

	@ConfigItem(
		keyName = "minimapArrowColor",
		name = "Minimap Arrow Color",
		description = "The color of the minimap arrow/marker pointing at the current step's destination",
		position = 2,
		section = minimapSection
	)
	default Color minimapArrowColor()
	{
		return new Color(255, 200, 50);
	}

	@ConfigSection(
		name = "Debug",
		description = "Debug options",
		position = 8,
		closedByDefault = true
	)
	String debugSection = "debug";

	@ConfigItem(
		keyName = "debugData",
		name = "Debug Data",
		description = "Enables various debug data for this plugin",
		position = 0,
		section = debugSection
	)
	default boolean debugData()
	{
		return false;
	}

	@ConfigItem(
		keyName = "lockedTracks",
		name = "Use Locked Tracks",
		description = "When enabled, pretends all tracks are locked - to be unlocked again",
		position = 1,
		section = debugSection
	)
	default boolean lockedTracks()
	{
		return false;
	}

	@ConfigItem(
		keyName = "enableTrackBuilder",
		name = "Enable Track Builder",
		description = "Adds a Builder tab to the side panel for creating and editing custom music tracks and routes",
		position = 3,
		section = debugSection
	)
	default boolean enableTrackBuilder()
	{
		return false;
	}
}