# Music Tracker

<!--![Installs](https://img.shields.io/endpoint?color=informational&label=Installs&url=COMING_SOON)-->
[![License](https://img.shields.io/badge/License-BSD%202--Clause-blue.svg)](https://opensource.org/licenses/BSD-2-Clause)

A music unlock tracker for Old School RuneScape.

Track the progress of your music tracks with route navigation, requirement overlays, entity highlighting, and track auto-progression to help you earn your Music Cape.

> **Note:** This plugin integrates with the [Shortest Path](https://runelite.net/plugin-hub/show/shortest-path) plugin for navigation. It is highly recommended to have the Shortest Path plugin installed for the best experience.

<img src="/docs/tracker-main.png" alt="Music Tracker" />

## Features

- **Multi-Route Support** — Provides multiple route options for numerous tracks (e.g. standard path vs. Abyss route), with new routes added regularly.
- **Smart Progression** — Automatically advances stages when you complete interactions or reach destinations. Handles plane changes, teleports, and backtracking intelligently. (Still a WIP)
- **Requirement Overlays** — See required and recommended items, levels, quests, and notes directly on screen.
- **Dynamic Requirements** — Automatically evaluates special requirements based on your current game state for certain tracks (e.g. whether you've sacrificed a Fire Cape for the Inferno, or completed specific diaries).
- **Entity Highlighting** — Highlights the correct NPC or object for the current step, with optional hint text.
- **Side Panel** — Clean collapsible region list with search, skip checkboxes, and color-coded track status.
- **Auto Progress** — Automatically moves to the next closest track after unlocking one.
- **World Map Integration** — Shows a Music Cape icon on the world map for your current target.
- **World Map Points** — Shows tracks you've unlocked or that are locked.
- **Shortest Path Support** — Integrates with the Shortest Path plugin for navigation.

## Screenshots

### Side Panel

<img src="/docs/tracker-side.png" alt="Music Tracker Side Panel" />

### In-Game Overlay

<img src="/docs/tracker-overlay.png" alt="Music Tracker Overlay" />

### Entity Highlighting

<img src="/docs/tracker-highlight.png" alt="Music Tracker Highlighting" />

### World Map

<img src="/docs/tracker-worldmap.png" alt="Music Tracker World Map" />

## Installation

1. Open RuneLite
2. Go to the **Plugin Hub**
3. Search for **"Music Tracker"**
4. Click **Install**

Alternatively, you can build from source (see below).

## Usage

1. Open the side panel (Music Tracker icon in the sidebar).
2. Expand a region and click on a track to start tracking it.
3. The plugin will show you the current step, highlight the target entity, and display any requirements.
4. Use the **Start/Stop Tracking** button or right-click options for quick control.
5. Right-click a track in the panel to switch between available routes.

## Configuration

The plugin has several configuration sections:

- **General** — Auto progress, save skipped tracks, use Shortest Path, show world map points.
- **Filters** — Hide unlocked tracks, hide tracks missing levels/quests, hide members tracks.
- **Overlay** — Toggle various information shown on the in-game overlay.
- **Entity Highlighting** — Customize highlight colors.
- **Debug** — Fake unlock mode and debug data.

### Building from Source

```bash
git clone https://github.com/The-DejaQ/music-tracker.git
cd music-tracker
./gradlew build