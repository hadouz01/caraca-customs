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

- **Crafting:** apricorns around a copper ingot for regular and ancient balls (ancient ones also use tumblestone), quartz to upgrade them to Ultra, and bread or food, a milk bucket and quartz for Caraca Balls.
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
- **Spawn cap:** species are split into 16 groups by evolutionary stage and potential (low, medium and high potential for each stage, starters, regular and mythical legendaries). Stronger groups only spawn naturally around players with a high enough tier, fishing and herds included (herd members above your tier are swapped for allowed ones). By default they unlock from tier 1 (low potential stage 1) to tier 6 (mythical legendaries).
- **Spawn rules per group:** starting tier, max spawn rate multiplier (0.01 to 100), up to 3 early tiers at a reduced rate (25% by default, then half of that per tier; medium potential stage 1 shows up in tier 1 at 50%) and an optional gradual reduction in later tiers. All 1025 species are sorted into the groups by evolution stage and the strength of their final form (Pokémon that only evolve from a baby, like Lucario, go by their base stats as stage 2 or 3). Species in no group (e.g. from other mods) spawn normally, and the lists in `data/caraca_customs/spawn_groups/` can be changed by datapacks.
- **Enough species per tier:** each tier always offers enough spawnable species to reach the next one, plus a configurable surplus (25% by default). If it doesn't, the next groups are unlocked early at a reduced rate.
- **Spawn levels:** wild Pokémon around you spawn between a minimum level (half your level cap minus 3, 5 or 10, depending on the cap) and your level cap. Most land in an average range from 40% to 80% of the way (at least 65% of the spawns), and levels further from it get rarer, so the minimum level and the cap are very rare. With 6 tiers, tier 2 spawns levels 13 to 32, mostly 21 to 28. The range and share are configurable.

### Following Pokémon

Sent out party Pokémon follow their owner at a size-based distance with a speed boost: slow species walk as fast as an average Pokémon, and anyone falling behind runs faster the further back it is, keeping up even with a sprinting player instead of teleporting. Small and medium Pokémon (up to 2.5 m) stay 2 to 5 blocks away, big ones (2.5 m to 5 m) 3 to 6 blocks and extra big ones (over 5 m) 5 to 9 blocks. Wild Pokémon keep their normal speed.

### Configuration

Everything lives in `config/caraca_customs/server_config.json`, and can also be edited through **Mod Menu**.

- **Mechanic toggles:** the top of the General page turns each mechanic on or off without restarting: custom Poké Balls (recipes, chest loot, native recipe replacements and creative tab), berry mechanics, level cap, spawn cap, tier spawn levels and Pokémon follow speed. Turning the custom Poké Balls on or off reloads the datapacks so recipes update right away.

```json
{
  "customBallsEnabled": true,
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

In this example, Oran trees always give 3 to 6 berries but keep the default growth time, and the Ultra Saffron Ball keeps the Ultra default rate but gets a 10× boost against Fire Pokémon.

- **Servers:** the server's config is sent to every player who joins, so tooltips and the catch rate HUD match the server.
- **`/caraca reload`** (operators only): applies config changes without restarting and sends them to everyone online.
- **Growth debugger:** shows in chat how long each berry stage will take and which mulch is active.

Translated into **English** and **Brazilian Portuguese**.

## License

Licensed under **CC0 1.0 Universal**. See [LICENSE](LICENSE).
