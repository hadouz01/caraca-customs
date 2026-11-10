# Caraca Customs

A Fabric add-on for **Cobblemon** that adds 58 type-based Poké Balls, a fully configurable berry system and Poké Ball loot across the world. It works in singleplayer and on dedicated servers, and everything can be tuned in-game or through a single JSON file.

## Requirements

| | Version |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | 0.19.3+ |
| Fabric API | any |
| Fabric Language Kotlin | any |
| Cobblemon | 1.8.4+ |
| Catch Rate Display | 2.12.3+ (optional) |
| Mod Menu + Cloth Config | optional, for the in-game config screen |

## Building

```sh
./gradlew build
```

The jar is written to `build/libs/caraca_customs-<version>.jar`.

To run the game in development:

```sh
./gradlew runClient
./gradlew runServer
```

Cobblemon 1.8.4 requires **Java 21**. The build uses a Gradle toolchain for that, so a JDK 21 must be installed (Gradle can download one automatically). Cobblemon and Catch Rate Display are compiled against the jars in `libs/`.

## Features

### Pokéballs

Every elemental ball comes in four versions: **Regular**, **Ultra**, **Ancient** and **Ancient Ultra**. Each one is stronger against its own Pokémon types.

| Ball | Strong against |
|---|---|
| Aquamarine | Flying, Ice |
| Azure | Water, Dragon |
| Cherry | Psychic |
| Cinnamon | Rock, Ground |
| Citrine | Electric |
| Lavender | Poison, Ghost |
| Lime | Bug |
| Poké / Premier (Ivory on Ancient) | Normal, Fighting |
| Roseate | Fairy |
| Saffron | Fire |
| Slate | Dark |
| Smoked | Steel |
| Verdant | Grass |
| **Caraca** | **Every type + perfect IVs** |

A ball uses its **boosted** rate when the Pokémon has one of its types or a status tied to them (Burn for Fire, Paralysis for Electric, Poison for Poison, Freeze for Ice), and that rate is doubled if the Pokémon is asleep. Otherwise it uses its **default** rate.

| Tier | Default rate | Boosted rate |
|---|---|---|
| Regular (and Ancient) | 1× | 1.5× |
| Ultra (and Ancient Ultra) | 3.35× | 6.7× |
| Caraca | 6.7× | 6.7× |

- **Crafting:** apricorns around a copper ingot for regular and ancient balls (ancient ones also use tumblestone), quartz to upgrade them to Ultra, and bread or food, a milk bucket and quartz for Caraca Balls. The regular material (copper ingot, raw copper, iron ingot, raw iron, iron nugget or cobblestone) and the Ultra one (quartz, gold ingot, raw gold, gold nugget, diamond, emerald, ender pearl or cobblestone) can be swapped in the config without changing the recipes' shape, and "Fill Empty Spaces With" can add 4 iron or gold ingots to the empty corners of every Ultra recipe (nothing by default).
- **Color mixing:** a single color ball (Poké, Citrine, Azure, Verdant, Roseate, Slate, Premier or Ivory) with two apricorns of another color turns into the mixed color ball, like a Citrine Ball and two red apricorns into a Saffron Ball. A mixed ball with two apricorns of one of its colors turns back into that color's ball, like a Saffron Ball and two yellow apricorns into a Citrine Ball. One ball in, one ball out, in every family (Regular, Ultra, Ancient and Ancient Ultra), Caraca Balls aside.
- **Cobblemon recipes:** Cobblemon's Poké, Premier, Azure, Citrine, Roseate, Slate and Verdant ball recipes (and their Ancient versions) craft the Caraca Customs versions instead. Any other recipe for those balls, even from another mod or datapack, also gives the Caraca Customs versions.
- **Loot:** Regular balls in villages, dungeons, temples, pyramids, mansions and outposts; Ultra balls also in mineshafts and strongholds; Caraca Balls in Nether fortresses, End cities and ancient cities. The Spawn Bonus Chest gives Caraca Customs balls too.
- **Tooltips:** hovering over a ball shows its catch rates and favored types, each in its own color.
- **Catch Rate Display:** when installed, every custom ball shows up in its HUD and ball comparison list with the right multipliers.
- **Pokémon properties:** `pokeball=lavender_ball` or `pokeball=poke_ball` (in `/givepokemon`, `/pokeedit` and so on) pick the Caraca Customs balls, with tab completion. Cobblemon's own versions still work with their full id, like `pokeball=cobblemon:poke_ball`.

### Berries

Every Cobblemon berry can be configured on its own, and any value left at `0` uses the global default.

- **Yield:** a minimum and maximum amount of berries per harvest (up to 6), also used by trees that spawn naturally.
- **Growth time:** a minimum and maximum time in minutes for each growth stage, rolled randomly per stage. After a long stage the next one is kept shorter.
- **Fortune hoes:** each Fortune level has a configurable chance (57% by default) to give an extra berry.

| Mulch | Effect |
|---|---|
| Growth Mulch | Every growth stage is 3× faster |
| Rich Mulch | +3 berries per harvest (up to 6) |
| Any mulch | Lasts 4 harvests, then it's used up |

### Player tiers

Every different species registered as caught in your Pokédex counts towards your **tier**, and each tier raises the **level cap** of your Pokémon.

| Tier | Species caught | Level cap |
|---|---|---|
| 1 | 0 | 16 |
| 2 | 85 | 32 |
| 3 | 170 | 48 |
| 4 | 255 | 64 |
| 5 | 340 | 80 |
| 6 | 512 | 100 |

- **Configurable:** 1 to 20 tiers and 50 to 1025 species for the last one. Levels and species are split evenly between tiers, the leftover goes to the last tier, and the max level follows Cobblemon's config.
- **At the cap:** Pokémon stop gaining experience from battles and candies (candies aren't consumed). Commands like `/levelup` and `/pokeedit` still go past it.
- **Feedback:** messages when a Pokémon hits the cap (with how many species are missing for the next tier and its level cap) and when you reach a new tier, plus `/caraca tier [player]` to check the progress.
- **Spawn cap:** species are split into 16 groups by evolutionary stage and potential (low, medium and high potential for each stage, starters, regular and mythical legendaries). Stronger groups only spawn naturally around players with a high enough tier, fishing and herds included (herd members above your tier are swapped for allowed ones). Placed Poké Snacks follow the highest tier among the players within a chunk of them (the nearest player when nobody is that close), or the world's tier while sharing tiers. By default they unlock from tier 1 (low potential stage 1) to tier 6 (mythical legendaries).
- **Spawn rules per group:** starting tier, max spawn rate multiplier (0.01 to 100), up to 3 early tiers at a reduced rate (25% by default, then half of that per tier; medium potential stage 1 shows up in tier 1 at 50%) and an optional gradual reduction in later tiers. All 1025 species are sorted into the groups by evolution stage and the strength of their final form (Pokémon that only evolve from a baby, like Lucario, go by their base stats as stage 2 or 3). Species in no group (e.g. from other mods) spawn normally, and the lists in `data/caraca_customs/spawn_groups/` can be changed by datapacks.
- **Enough species per tier:** each tier always offers enough spawnable species to reach the next one, plus a configurable surplus (40% by default). If it doesn't, the next groups are unlocked early at a reduced rate.
- **Spawn levels:** wild Pokémon around you spawn between a minimum level (half your level cap minus 3, 5 or 10, depending on the cap) and your level cap. Most land in an average range from 40% to 80% of the way (at least 65% of the spawns), and levels further from it get rarer, so the minimum level and the cap are very rare. With 6 tiers, tier 2 spawns levels 13 to 32, mostly 21 to 28. Pokémon attracted by a Poké Snack use the snack's tier. The range and share are configurable.
- **Shared tiers:** with player-shared tiering on (off by default), every different species caught by any player counts towards everyone's tier, so two players with 40 different species each reach the tier that needs 80. Captures are kept in a world Pokédex that only stores which players caught each species, recorded even while the option is off, and older captures are added when each player joins (or all at once with `/caraca sharedex rebuild`). Players in the **Tier-share Blocklist** progress with their own captures only, and the species only they caught don't count for the others.

### Exp. Share All

When one Pokémon in your party earns battle experience, the rest of the party earns some too, as if everyone held an Exp. Share. Only players' Pokémon are affected.

| Division | 100 exp in a party of 4 |
|---|---|
| **Multiplied** (default, ×0.5) | 100 for the Pokémon that fought, 50 for each of the others |
| **Shared, Equally** | 25 for everyone |
| **Shared, Balanced** | 50 for the Pokémon that fought, about 17 for each of the others |

A Lucky Egg only boosts the Pokémon holding it, and a Pokémon holding an Exp. Share gets +0.5× (configurable from 0 to 100) for every party member holding a Lucky Egg, split between every Exp. Share holder: 3.5× with five Lucky Eggs and one Exp. Share, 2× each with four Lucky Eggs and two Exp. Shares. Benched Pokémon get experience only, no EVs, unless they really hold an Exp. Share. Every slice still stops at the level cap and follows Cobblemon's own `awardExperienceOnBattleLoss` and `awardExperienceToFaintedPokemon` options.

### Caracatron 3000

A handheld device (in Cobblemon's utility tab) that opens a Pokédex-style screen with your tier progress:

- **Header:** your tier (the world's while sharing tiers), how many Pokémon are available up to it and how many of them were caught.
- **Pokémon list:** every Pokémon sorted by tier (the default), every Pokémon sorted by dex number, or only the Pokémon of one tier (a Pokémon is listed in the first tier it can spawn in, even at a reduced rate, so every tier shows all the Pokémon that count towards reaching the next one; legendaries and mythicals without natural spawns are listed in their spawn group's tier), with their model, dex number and a Poké Ball icon (also shown next to the selected Pokémon's name): red when you caught it, yellow topped when only other players did, and none when nobody caught it. Each slot is tinted with its tier's color (yellow green, green, aqua, blue, magenta and red for the default six tiers; past six tiers the slots get a blended outline instead), and uncaught Pokémon get a dark filter over their slot. Search by name (ignoring upper case and accents) or dex number. While sharing tiers, the globe switches between everyone's captures and yours.
- **Spawn info:** types, dex number, name, tier (written in the tier's color) and every natural spawn of the selected Pokémon for the picked rarity: place, biomes, time, weather, height, structures and weight, plus when it unlocks, its spawn rate multiplier and the spawn levels of your tier. Hovering a biome group (like "Forest") lists every biome in it, one per line with where it comes from (Vanilla or the biome mod that adds it).
- **How to attract it:** the three best seasonings for the selected Pokémon first, then the Campfire Pot grids of the Poké Snack and the Fishing Bait (with those seasonings in the three slots on the right and the result after an arrow), and every seasoning that boosts it, each item after its icon: berries for its type or egg group (10× spawn rate each, multiplying each other), EV berries that leave only Pokémon giving that EV, and golden foods that favor rarer spawns. Hovering any ingredient or seasoning shows how to get it, one entry per line: the biomes where a berry grows naturally and the berries that mutate into it when planted side by side, or the recipe of other items. Clicking an item name or a grid slot opens it on the Materials page.
- **Scanner:** holding the use button with the Caracatron opens Cobblemon's Pokédex scanner, scanning Pokémon faster than a Pokédex (2× by default, from 1× to 5× with the "Caracatron Scan Speed" option); a quick click opens the Caracatron.
- **Dynamic light:** with LambDynamicLights installed, the Caracatron lights up its surroundings at level 15 while held or dropped.
- **Pages:** the arrows at the top of the screen switch between the Caracatron, Cobblemon's Pokédex (opened on the selected Pokémon, inside the Caracatron's frame and dressed in your screen color, its textures recolored in game unless a resource pack provides `colors/<color>/pokedex/<texture>.png`), Drops, Materials and the Guide (still under construction). Coming back to the Caracatron keeps your selection, search and filter. The Caracatron opens on the page it was last closed on, with the same searches, selected Pokémon and item, and tabs. The color select and filter bar tooltips remind you the mouse wheel works there too. Scrolling the mouse wheel over the arrows switches pages too, and over the filter bar it switches filters. Clicking the filter name opens a drawer listing every filter of the page, like the screen color one. The select at the top center shows the current page.
- **Drops:** the same list, with every model showing whether caught or not, but the search looks for items (by name or id, like `minecraft:emerald`) and lists the Pokémon that drop them, along with the Pokémon whose name or number matches (the matching part of the name shows underlined in green). While searching, the "By biggest drop" filter puts first the Pokémon that drop the most of the item (amount times chance). The right panel shows the icon of every item the selected Pokémon drops (hover it for the name), with amounts and chances, the searched items' lines in bold green (hover an item to underline it, click it to search its id), and with the Pasture Loot mod, a "(✔ Pasture)" or "(✘ Pasture)" tag after each chance (hover it for what it means).
- **Materials:** search any block or item, from any mod, by name or id (two letters or more) and pick it to see where it generates: dimension, biomes (up to six, the rest in the tooltip of "And X more..."; biome groups are marked with a "*" and list their biomes when hovered), altitude with the most common range, tries per chunk, rarity and vein size. Plants growing on the ground (crops, bushes and Cobblemon's berries) show the model of each growth stage at the top. Items dropped by a block (like Raw Iron) show the block they drop from (Iron Ore). It also lists berry groves and how many Pokémon drop it, which opens the Drops page searching that item when clicked. Each line's label (dimension, altitude, structure...) is bold and colored to stand out. Three more tabs under the panel list the structures built with it (villages, ruins, shipwrecks, Cobblemon's structures and any datapack or mod structure made of templates) with where they generate and the chests holding it, every recipe making it (main one first, each with its grid, an arrow and the result with how many it makes; clicking an ingredient opens it) and what you can dig up with a brush (Cobblemon fossils included), grouped by structure.
- **Screen color:** picked in the select at the top center of every page (or in Mod Menu), or by scrolling the mouse wheel over it, a personal option kept by each player even on servers. Cerulean, Blanc, Canary, Lime, Noir and Pink come with the mod, and the header and filter texts stay white on every color, while the info panel uses the Pokédex's gray (a lighter gray on Noir), with titles in a shade matching each color. The screen of the Caracatron you hold, and the ones you see other players holding, shows your own color. Every color folder in `assets/caraca_customs/textures/gui/caracatron/colors/` shows up in the list (with a `models/item/caracatrons/screens/<color>.json` model for the held device), so resource packs can add new screens.

### Following Pokémon

Sent out party Pokémon follow their owner at a size-based distance with a speed boost: slow species walk as fast as an average Pokémon, and anyone falling behind runs faster the further back it is, keeping up even with a sprinting player instead of teleporting. Small and medium Pokémon (up to 2.5 m) stay 2 to 5 blocks away, big ones (2.5 m to 5 m) 3 to 6 blocks and extra big ones (over 5 m) 5 to 9 blocks. Wild Pokémon keep their normal speed.

### Configuration

Everything lives in `config/caraca_customs/server_config.json`, and can also be edited through **Mod Menu**.

- **Mechanic toggles:** the top of the General page turns each mechanic on or off without restarting: custom Poké Balls (recipes, chest loot, native recipe replacements and creative tab), berry mechanics, level cap, spawn cap, tier spawn levels, Pokémon follow speed, Exp. Share All and player-shared tiering. Turning the custom Poké Balls on or off, or changing their materials, reloads the datapacks so recipes update right away.

```json
{
  "customBallsEnabled": true,
  "regularBallMaterial": "minecraft:copper_ingot",
  "ultraBallMaterial": "minecraft:quartz",
  "ultraBallCornerFill": "none",
  "berryMechanicsEnabled": true,
  "dynamicFollowSpeedEnabled": true,
  "levelCapEnabled": true,
  "tierCount": 6,
  "maxTierCaptureCount": 512,
  "spawnCapEnabled": true,
  "tierSpawnLevelsEnabled": true,
  "averageSpawnLevelStart": 0.4,
  "averageSpawnLevelEnd": 0.8,
  "averageSpawnLevelChance": 0.65,
  "expShareAllEnabled": true,
  "defaultExpShareConfig": { "expDivisionMethod": "MULTIPLIED", "expShareMethod": "EQUALLY", "expShareMultiplier": 0.5, "expShareItemBonus": 0.5 },
  "playerSharedTieringEnabled": false,
  "tierShareBlocklist": ["Steve"],
  "caracatronScreenColor": "cerulean",
  "caracatronScanSpeed": 2,
  "globalHoeFortuneChance": 0.57,
  "defaultBerryConfig": { "minYield": 1, "maxYield": 6, "minGrowthTime": 1, "maxGrowthTime": 5 },
  "berryOverrides": {
    "cobblemon:oran_berry": { "minYield": 3, "maxYield": 6, "minGrowthTime": 0, "maxGrowthTime": 0 }
  },
  "defaultUltraPokeBallConfig": { "defaultCaptureRate": 3.35, "boostedCaptureRate": 6.7 },
  "pokeBallOverrides": {
    "ultra_saffron_ball": { "defaultCaptureRate": 0.0, "boostedCaptureRate": 10.0 }
  }
}
```

In this example, Oran trees always give 3 to 6 berries but keep the default growth time, and the Ultra Saffron Ball keeps the Ultra default rate but gets a 10× boost against Fire Pokémon. Steve is in the tier-share blocklist, which only matters once shared tiering is turned on.

- **Servers:** the server's config is sent to every player who joins, so tooltips and the catch rate HUD match the server.
- **`/caraca reload`** (operators only): applies config changes without restarting and sends them to everyone online.
- **Growth debugger:** shows in chat how long each berry stage will take and which mulch is active.

Translated into **English** and **Brazilian Portuguese**, also used for **European Portuguese**.

## License

Licensed under **CC0 1.0 Universal**. See [LICENSE](LICENSE).

The Caracatron 3000 item models and textures (`assets/caraca_customs/models/item/caracatron_3000_model*.json` and `assets/caraca_customs/textures/item/caracatrons/`) are adapted from [Cobblemon](https://gitlab.com/cable-mc/cobblemon)'s Pokédex, and the small caught icon of the Caracatron's slots (`caught_seen_icon_slot_small.png`) follows the shape of Cobblemon's small caught icon; these files stay under the **Mozilla Public License 2.0**, like the rest of Cobblemon.
