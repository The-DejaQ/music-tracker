## Changelog

### [1.0] - 2026-07-14

#### Plugin
- Removed name-based ItemRequirement as reflection for ItemID is not allowed.
- Added handled route names to all special tracks so we don't override every route on a Music Track.
- More UI changes: Collapsible filter section, font changes
- Fixed tracked title color not updating upon clicking to track.
- Added a few more Progression systems, highly experimental.

#### Tracks
- Added proper route for track **Attack 5**
- The plugin will now highlight the closest Quetzal to the player instead of setting default to Civitas illa Fortis.
- Fixed **Varlamore** tracks, they now show correct highlighting for Quetzals.

### [1.0] - 2026-07-11

#### Plugin
- Fixed refreshing throwing an error and fail to reload track/region data.
- Added better handling for dynamic requirements

#### Tracker Content Panel
- Added filter options on the Side-Panel UI (removed from config; migrated into new filters)
- Added Report issue / View changelog buttons to top panel

#### Tracks
- Fixed **Race Against the Clock** main route to show pickaxe requirement
- Fixed **Dagannoth Dawn** required items - removed quest required