# Music Tracker TODO

**Last Updated:** 2026-07-20

- Equipped item `ItemRequirement` check

### ISSUES

#### Tracks
- Pest Control & Null and Void special handlers
- Abyss recommends obstacle WITHOUT the TOOL in our container. It might SKIP obstacle we DO have tool for (i.e. we have tinderbox; it might suggest Distract Eyes, even though we're right next to Boil)

#### Routes

### Other
- **Data Cleanup**
    - Move all quest-locked tracks into `Quests.json`
    - Remove `quest` field from inside routes (should only exist at top-level for `unlockType: QUEST`)

- **Code Fixes**
    - Implement proper fallback / validation for tracks with `unlockType: QUEST` but missing `quest` key
    - Ensure `getUnlockRestrictionMessage()` always works correctly even with malformed data