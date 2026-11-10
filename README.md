# Caraca Customs

A Fabric add-on for **Cobblemon** 1.8.4 on Minecraft 1.21.1. It works in singleplayer and on dedicated servers, and every mechanic can be turned on or off and tuned in-game (Mod Menu) or through a single JSON file.

Full explanations of every mechanic live in the [Wiki](https://github.com/hadouz01/caraca-customs/wiki).

## What it adds

- **[Poké Balls](https://github.com/hadouz01/caraca-customs/wiki/Poke-Balls):** 58 type-based balls in Regular, Ultra, Ancient and Ancient Ultra versions, plus the Caraca Ball, with color mixing recipes, configurable materials and loot across the world.
- **[Berries](https://github.com/hadouz01/caraca-customs/wiki/Berries):** yield and growth time per berry, Fortune hoes and mulch that runs out after 4 harvests.
- **[Player tiers](https://github.com/hadouz01/caraca-customs/wiki/Player-Tiers):** the more species you catch, the higher your tier, which raises your Pokémon's level cap, unlocks stronger spawns and sets the level of wild Pokémon. Tiers can be shared between players.
- **[Exp. Share All](https://github.com/hadouz01/caraca-customs/wiki/Exp-Share-All):** the whole party earns battle experience, multiplied or shared.
- **[Caracatron 3000](https://github.com/hadouz01/caraca-customs/wiki/Caracatron-3000):** a handheld device showing your tier, which Pokémon you can find and where, how to attract them, what they drop and where every block and item comes from. It also scans Pokémon faster than a Pokédex.
- **[Following Pokémon](https://github.com/hadouz01/caraca-customs/wiki/Following-Pokemon):** sent out Pokémon follow at a distance based on their size and keep up with you.
- **[Configuration](https://github.com/hadouz01/caraca-customs/wiki/Configuration):** everything synced from the server and reloadable with `/caraca reload`.

Translated into **English** and **Brazilian Portuguese**, also used for **European Portuguese**.

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

You need a **JDK 21** (Cobblemon 1.8.4 requires exactly Java 21). Then run:

```sh
./gradlew build
```

The jar is written to `build/libs/caraca_customs.jar` (and a copy with the version in its name). Cobblemon and Catch Rate Display are downloaded from the Modrinth Maven, so the first build needs internet.

To run the game in development, use `./gradlew runClient` or `./gradlew runServer`.

<details>
<summary><b>If the build fails</b></summary>

- **"Unsupported Java" or "No matching toolchains found":** Gradle is running on another Java version. Point `JAVA_HOME` to a JDK 21 before building:
  ```sh
  # Linux / macOS / Git Bash
  export JAVA_HOME="/path/to/jdk-21"
  # Windows PowerShell
  $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
  ```
- **On Windows `cmd` or PowerShell:** use `gradlew.bat build` (or `.\gradlew build`) instead of `./gradlew build`.
- **"Failed to compute checksum" or "Could not resolve maven.modrinth:...":** the dependencies couldn't be downloaded. Check your connection and run `./gradlew build --refresh-dependencies`.
- **"Failed to setup Minecraft" after an interrupted build:** Loom's cache may be broken. Stop Gradle with `./gradlew --stop`, delete `~/.gradle/caches/fabric-loom` and build again.
- **Strange errors after switching branches or versions:** `./gradlew clean build`.
- **`runClient` hangs while loading Cobblemon:** Showdown didn't start, usually because the Gradle run used another Java version. Build and run with JDK 21.

</details>

## License

Licensed under **CC0 1.0 Universal**. See [LICENSE](LICENSE).

The Caracatron 3000 item models and textures (`assets/caraca_customs/models/item/caracatron_3000_model*.json` and `assets/caraca_customs/textures/item/caracatrons/`) are adapted from [Cobblemon](https://gitlab.com/cable-mc/cobblemon)'s Pokédex, and the small caught icon of the Caracatron's slots (`caught_seen_icon_slot_small.png`) follows the shape of Cobblemon's small caught icon; these files stay under the **Mozilla Public License 2.0**, like the rest of Cobblemon.
