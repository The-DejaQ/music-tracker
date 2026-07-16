## Development History

###### 2026-07-15

#### Tracks
- Added **MysteriousRuinsHandler** (registered via **SpecialTrackRegistry**)
    - Added support for distinguishing between talisman vs tiara in inventory for relevant tracks
    - Improved object highlighting for Mysterious Ruins entrances
- Added talisman/tiara detection logic to **StratosphereHandler**
- Added **RighteousnessHandler** for music track on Entrana at Law Altar.

#### Core
- Added **Region** enum to centralize region name handling
    - Fixed **RegionLoader** incorrectly displaying region names without spaces
- Added `contributeTrackSteps` method to SpecialTrackHandler
    - **TrackNavigator** now applies contributed Track Steps from Special handlers
- Added checks for entity only in **TrackStep**
- Added `ENTRANA_BLOCKED` and `ENTRANA_ALLOWED_BONUS` fields to ItemCollections.
- Added equipped field to check if an item should be equipped for requirements/recommendations.

###### 2026-07-14

#### Plugin
- Removed name-based `ItemRequirement` (reflection on `ItemID` not allowed)
- Added handled route names to all `SpecialTrackHandler`s to prevent overriding every route
- UI improvements: Collapsible filter section + font adjustments
- Fixed track title color not updating when selecting a track
- Added experimental progression systems

#### Tracks
- Added proper route for **Attack 5**
- Quetzal highlighting now uses closest Quetzal instead of defaulting to Civitas illa Fortis
- Fixed **Varlamore** track highlighting for Quetzals

###### 2026-07-11

#### Plugin
- Fixed refresh throwing errors and failing to reload track/region data
- Improved dynamic requirement handling

#### Tracker Content Panel
- Moved filter options into Side Panel UI (removed from config)
- Added "Report issue" and "View changelog" buttons to top panel

#### Tracks
- Fixed **Race Against the Clock** main route to show pickaxe requirement
- Fixed **Dagannoth Dawn** required items (removed quest requirement)