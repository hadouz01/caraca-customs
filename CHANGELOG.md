# Caraca Customs — Patch Notes

Fabric mod for Minecraft 1.21.1 that extends **Cobblemon** with custom Poké Balls, a configurable berry system and loot changes, with optional **Cobblemon Catch Rate Display** (catchrate-display) integration.

---

## 1.1.0

**Requires Cobblemon 1.8.4 or newer** (previously 1.7.3). Optional: catchrate-display 2.12.3 or newer.

### Compatibility
- Updated to **Cobblemon 1.8.4** and **catchrate-display 2.12.3**.
- The 4 berries added in Cobblemon 1.8.4 (**Eggant, Enigma, Hopo, Touga**) are now configurable like every other berry.
- catchrate-display integration now uses its official ball API: custom Poké Balls show up in the ball comparison panel with their translated names and live multipliers. The mod also works when catchrate-display isn't installed.

### Fixes
- **Server config sync now works.** Joining players receive the server's configuration, `/caraca reload` re-sends it to everyone online, and clients go back to their own config after disconnecting. In 1.0.0 the sync was never active.
- **Hoe Fortune bonus now works.** Harvesting a berry tree while holding a Fortune hoe rolls the bonus berries at harvest time. Before, the bonus was rolled when berries formed, where Cobblemon 1.8.4 never provides the player, so it could not trigger.
- **Mulch:** the mulch state is read correctly on Cobblemon 1.8.4, and when a mulch runs out it now also disappears visually for players.
- **Fresh trees:** the first mulch applied to a freshly planted tree now lasts 4 yield cycles, like every other mulch (it was 3).
- **Long growth times:** berry growth times of roughly 18 hours or more no longer risk crashing the game when a tree is planted.
- Berry growth settings are re-applied after `/reload`.
- **Recipes:**
  - The **Ancient Caraca Ball** recipe failed to load and can now be crafted.
  - The **Caraca Ball** recipe had two bread slots that could never be filled; it can now be crafted.
- **Bonus chest:** the Spawn Bonus Chest loot table failed to load because it referenced a Poké Ball that doesn't exist. It loads again, and now includes the **Aquamarine Ball**.
- **Cobblemon loot restored:** removed old copies of Cobblemon's loot tables that overrode 1.8.4's versions. Cobblemon's own loot is back: TMs in jungle temple, nether fortress and shipwreck supply chests, and the Focus Band in ruins. Only the Spawn Bonus Chest is still changed by this mod.
- **Variation file:** the **Ultra Verdant Ball** was missing its Poké Ball variation file.
- **First launch:** the config file is now created with an override entry for every custom Poké Ball.
- **Large configs:** big configurations no longer risk exceeding the network packet size limit.
- **Stray folders:** the mod no longer creates a `src/` folder inside game or server directories.

### Changes
- **Mod Menu:**
  - Added the **Hoe Fortune chance** option (0–1).
  - "Reset" buttons now restore the real defaults; on per-berry and per-ball overrides they reset to `0`, meaning "use the global value".
- **Config:** the Hoe Fortune chance is now limited to 0–1.
- **`/caraca reload`:** now reports failure correctly to command blocks and functions.

---

## 1.0.0

Initial release. Built for Cobblemon 1.7.3.

### Custom Poké Balls
58 new Poké Balls in four families, all registered under the `caraca_customs` namespace with their own textures, 3D models and throw particles:

| Family | Balls | Default tier |
|---|---|---|
| Regular | 14 elemental balls + Caraca Ball | Regular (Caraca Ball: Caraca) |
| Ultra | 14 elemental balls | Ultra |
| Ancient | 14 elemental balls + Ancient Caraca Ball | Regular (Ancient Caraca Ball: Caraca) |
| Ancient Ultra | 14 elemental balls | Ultra |

Each elemental ball favors specific Pokémon types:

| Ball | Types |
|---|---|
| Aquamarine | Flying, Ice |
| Azure | Water, Dragon |
| Cherry | Psychic |
| Cinnamon | Rock, Ground |
| Citrine | Electric |
| Lavender | Poison, Ghost |
| Lime | Bug |
| Poké / Premier / Ivory | Normal, Fighting |
| Roseate | Fairy |
| Saffron | Fire |
| Slate | Dark |
| Smoked | Steel |
| Verdant | Grass |

**Catch logic**
- The ball uses its **boosted** multiplier when the target has one of its types, or has a status tied to one of them (**Burn** → Fire, **Paralysis** → Electric, **Poison/Toxic** → Poison, **Freeze** → Ice). Otherwise it uses its **default** multiplier.
- A **sleeping** target doubles the boosted multiplier.
- Default multipliers (default / boosted):
  - Regular: **1.0× / 1.5×**
  - Ultra: **3.35× / 6.7×**
  - Caraca: **6.7× / 6.7×**
- **Caraca Ball** and **Ancient Caraca Ball** match every type and give the caught Pokémon **perfect IVs (31 in every stat)**.
- Multipliers are read live from the configuration, so changes apply without a restart.

**Crafting**
- Regular and Ancient balls are crafted from apricorns (Ancient ones also use Tumblestone) around a **copper ingot**, 4 per craft.
- Ultra variants upgrade a regular or ancient ball with **quartz**, 2 per craft.
- Caraca Balls are crafted from bread, a milk bucket, **quartz** and a white apricorn. The Ancient Caraca Ball uses any food, a milk bucket, quartz and tumblestone.
- Cobblemon's own recipes for the Poké, Premier, Azure, Citrine, Roseate, Slate and Verdant balls (plus their Ancient versions) now craft the **Caraca Customs** versions instead.

**Item tags**
- `caraca_customs:poke_balls`
- `tier_1` through `tier_5` and `tier_x` Poké Ball tags (grouping custom and Cobblemon balls by strength)
- Matching crafting material tags (copper, iron, gold, diamond, quartz)

**Interface**
- Ball tooltips show the default and boosted multipliers and the favored types, colored per type.
- Cobblemon's creative tab hides the Cobblemon balls that this mod replaces and lists the custom balls first.

### Berries
Every Cobblemon berry can be configured individually, with a global default used for any unset value.
- **Yield:** minimum and maximum berries per harvest (up to 6). Also applies to naturally generated trees.
- **Growth time:** minimum and maximum minutes per growth stage. Stage durations are randomized within that range, and two consecutive stages can't both be long.
- **Growth Mulch:** each growth stage is 3× faster (at least 5 seconds).
- **Rich Mulch:** +3 berries per harvest (up to 6).
- **Any mulch** lasts **4 yield cycles**, then is used up.
- **Hoe Fortune:** each Fortune level has a configurable chance (default 57%) to add one extra berry. *(Could not trigger on Cobblemon 1.8.4; fixed in 1.1.0.)*

### Loot
- Custom Poké Balls are added to vanilla structure chests, with each tier also receiving the lower tiers' pools:
  - **Regular:** villages, dungeons, desert pyramids, jungle temples, woodland mansions, pillager outposts.
  - **Ultra:** mineshafts and strongholds.
  - **Caraca:** Nether fortresses, End cities and ancient cities.
- Cobblemon's Spawn Bonus Chest gives the Caraca Customs regular balls instead of Cobblemon's apricorn balls.

### catchrate-display integration (optional)
- Custom Poké Balls appear in the catch-rate HUD and ball comparison panel with their real multipliers and translated names.

### Configuration
- JSON file at `config/caraca_customs/server_config.json`, with values validated and kept within safe limits.
- In-game editor through **Mod Menu** + **Cloth Config**, with General, Berries, Overrides and Debuggers tabs.
- **Debuggers → growth tick debugger:** broadcasts each berry's next-stage duration and active mulch in chat.
- **`/caraca reload`** (operators): reloads the configuration and berry settings without restarting.
- Fully translated in **English** and **Brazilian Portuguese**.
