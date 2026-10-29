package com.caraca.customs.config

import com.caraca.customs.balls.CustomBallRegistry
import com.caraca.customs.balls.modules.BallTier
import com.caraca.customs.constants.ExpDivisionMethod
import com.caraca.customs.constants.ExpShareMethod
import com.caraca.customs.progression.SpawnGroup
import com.caraca.customs.progression.SpawnTierTable
import com.cobblemon.mod.common.api.berry.Berries
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.ResourceLocation
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import kotlin.math.roundToInt

/**
 * Data container representing individual berry configuration properties.
 *
 * @property minYield Minimum base yield count upon harvest.
 * @property maxYield Maximum base yield count upon harvest.
 * @property minGrowthTime Minimum growth time in minutes from stage 0 to stage 3.
 * @property maxGrowthTime Maximum growth time in minutes from stage 0 to stage 3.
 *
 * @author hadouz01
 * @since 1.0.0
 */
data class BerryConfigEntry(
    var minYield: Int = 0,
    var maxYield: Int = 0,
    var minGrowthTime: Int = 0,
    var maxGrowthTime: Int = 0
)

/**
 * Data container representing custom Poké Ball capture rate properties.
 *
 * @property defaultCaptureRate Base catch rate multiplier applied under standard conditions.
 * @property boostedCaptureRate Multiplier applied when elemental or status conditions are met.
 *
 * @author hadouz01
 * @since 1.0.0
 */
data class PokeBallConfigEntry(
    var defaultCaptureRate: Float = 0.0f,
    var boostedCaptureRate: Float = 0.0f
)

/**
 * Data container representing the spawn rules of a species group in the tier spawn cap.
 *
 * Rates are multipliers of the species' original spawn rate (`1.0` = unchanged).
 *
 * @property startTier Tier in which the group starts spawning at [maxSpawnRateMultiplier].
 * @property maxSpawnRateMultiplier Spawn rate multiplier reached at [startTier], from 0.01 to 100.
 * @property reducedRateInterval How many tiers before [startTier] the group already spawns, from 0 to 3. The tier right
 * before [startTier] gets [earlyTierSpawnRate] of [maxSpawnRateMultiplier], and each earlier tier gets half of the next one.
 * @property earlyTierSpawnRate Fraction of [maxSpawnRateMultiplier] given to the tier right before [startTier], from 0.01
 * to 1 (25% by default). Also used when the group is unlocked early to fill a tier without enough species.
 * @property reduceSpawnRateTier Tier in which the group reaches [maxReducedSpawnRate], always above [startTier].
 * `0` disables the reduction.
 * @property maxReducedSpawnRate Spawn rate multiplier reached at [reduceSpawnRateTier], from 0.01 to 99.99 and always at
 * least 0.01 below [maxSpawnRateMultiplier]. `0` disables the reduction. Between [startTier] and
 * [reduceSpawnRateTier] the rate goes down in equal steps.
 *
 * @author hadouz01
 * @since 1.5.0
 */
data class SpawnGroupConfigEntry(
    var startTier: Int = 1,
    var maxSpawnRateMultiplier: Float = 1.0f,
    var reducedRateInterval: Int = 0,
    var earlyTierSpawnRate: Float = DEFAULT_EARLY_TIER_SPAWN_RATE,
    var reduceSpawnRateTier: Int = 0,
    var maxReducedSpawnRate: Float = 0.0f
) {
    companion object {
        /**
         * Default [earlyTierSpawnRate]: 25% of the max multiplier.
         */
        const val DEFAULT_EARLY_TIER_SPAWN_RATE = 0.25f
    }
}

/**
 * Experience distribution rules used by Exp. Share All.
 *
 * @property expDivisionMethod Whether the active Pokémon's experience is split between the party or copied to it.
 * @property expShareMethod How [ExpDivisionMethod.SHARED] splits the experience: the same slice for everyone, or half
 * for the active Pokémon and half for the rest of the party.
 * @property expShareMultiplier Fraction of the active Pokémon's experience every other member gets with
 * [ExpDivisionMethod.MULTIPLIED], from 0 to 1.
 * @property expShareItemBonus Extra experience multiplier every party member holding a Lucky Egg adds to held Exp. Shares,
 * split between every Exp. Share holder, from 0 to 100. Ex: 0.5 with 4 Lucky Eggs and 2 Exp. Shares = 2× for each holder.
 *
 * @author hadouz01
 * @since 1.11.0
 */
data class ExpShareConfigEntry(
    var expDivisionMethod: ExpDivisionMethod = ExpDivisionMethod.MULTIPLIED,
    var expShareMethod: ExpShareMethod = ExpShareMethod.EQUALLY,
    var expShareMultiplier: Float = 0.5f,
    var expShareItemBonus: Float = 0.5f
)

/**
 * Main configuration model for mod customization including berries and custom Poké Balls.
 *
 * Employs insertion-ordered map instances to preserve natural layout structure across serialization and GUI rendering.
 *
 * @property debugGrowthTick Toggles debug notifications when growth ticks are calculated for berries.
 * @property globalHoeFortuneChance Base probability per Fortune level applied when harvesting with a tool.
 * @property defaultBerryConfig Fallback configuration applied to berries when attributes are non-positive or missing.
 * @property berryOverrides Map of specific berry identifiers to custom configuration entries.
 * @property defaultRegularPokeBallConfig Default capture rate config for regular custom Poké Balls.
 * @property defaultUltraPokeBallConfig Default capture rate config for ultra custom Poké Balls.
 * @property defaultCaracaBallConfig Default capture rate config for Caraca Poké Balls.
 * @property defaultExpShareConfig How Exp. Share All splits the experience between the party.
 * @property pokeBallOverrides Map of specific custom Poké Ball path identifiers to custom capture rate configurations.
 * @property customBallsEnabled Toggles the custom Poké Balls: their recipes, loot, the native recipes that craft them, the
 * creative tab replacement and the `pokeball=` aliases. Recipe changes apply on the next datapack reload.
 * @property berryMechanicsEnabled Toggles the berry growth times, yields, Hoe Fortune bonus and mulch uses.
 * @property dynamicFollowSpeedEnabled Toggles the size-based follow distance and catch-up speed of sent out party Pokémon.
 * @property expShareAllEnabled Toggles Exp. Share All, giving experience to the whole party when one member earns it.
 * @property levelCapEnabled Toggles the tier level caps on player-owned Pokémon.
 * @property tierCount Amount of progression tiers, from 1 to 20.
 * @property maxTierCaptureCount Caught species needed to reach the last tier, from 50 to 1025.
 * @property spawnCapEnabled Toggles the tier spawn cap on natural spawns.
 * @property tierSpeciesSurplus Extra spawnable species each tier must offer beyond what the next tier needs, as a fraction
 * of it, from 0 to 2, 0.4 by default. Ex: 0.4 = a tier leading to one that needs 100 species must have at least 140
 * spawnable species.
 * @property tierSpawnLevelsEnabled Toggles natural spawn levels following the level cap of the player's tier.
 * @property averageSpawnLevelStart Where the most common spawn levels start, as a fraction of the way from the tier's
 * minimum spawn level to its level cap, from 0 to 1.
 * @property averageSpawnLevelEnd Where the most common spawn levels end, in the same scale, from 0 to 1.
 * @property averageSpawnLevelChance Minimum share of natural spawns rolling a level from the most common range, from 0 to 1.
 * @property spawnGroups Map of [SpawnGroup] identifiers to their spawn rules.
 * @property playerSharedTieringEnabled Toggles player-shared tiering, counting the species caught by every player together
 * to unlock tiers.
 * @property tierShareBlocklist Names of players left out of the shared tiers, who progress with their own captures only.
 * @property caracatronScreenColor Screen color of the Caracatron 3000, the name of a color folder in
 * `assets/caraca_customs/textures/gui/caracatron/colors/`. A visual preference of each player, so the server's value never
 * replaces the client's own.
 * @property caracatronScanSpeed How many times faster the Caracatron 3000 scans Pokémon than a Pokédex, from 1 to 5.
 *
 * @author hadouz01
 * @since 1.0.0
 */
data class ConfigData(
    var debugGrowthTick: Boolean = false,
    var globalHoeFortuneChance: Float = 0.57f,
    var defaultBerryConfig: BerryConfigEntry = BerryConfigEntry(
        minYield = 1,
        maxYield = 6,
        minGrowthTime = 1,
        maxGrowthTime = 5
    ),
    val berryOverrides: MutableMap<String, BerryConfigEntry> = linkedMapOf(),
    var defaultRegularPokeBallConfig: PokeBallConfigEntry = PokeBallConfigEntry(
        defaultCaptureRate = 1.0f,
        boostedCaptureRate = 1.5f
    ),
    var defaultUltraPokeBallConfig: PokeBallConfigEntry = PokeBallConfigEntry(
        defaultCaptureRate = 3.35f,
        boostedCaptureRate = 6.7f
    ),
    var defaultCaracaBallConfig: PokeBallConfigEntry = PokeBallConfigEntry(
        defaultCaptureRate = 6.7f,
        boostedCaptureRate = 6.7f
    ),
    val defaultExpShareConfig: ExpShareConfigEntry = ExpShareConfigEntry(
        expDivisionMethod = ExpDivisionMethod.MULTIPLIED,
        expShareMethod = ExpShareMethod.EQUALLY,
        expShareMultiplier = 0.5f,
        expShareItemBonus = 0.5f
    ),
    val pokeBallOverrides: MutableMap<String, PokeBallConfigEntry> = linkedMapOf(),
    var customBallsEnabled: Boolean = true,
    var berryMechanicsEnabled: Boolean = true,
    var dynamicFollowSpeedEnabled: Boolean = true,
    var expShareAllEnabled: Boolean = true,
    var levelCapEnabled: Boolean = true,
    var tierCount: Int = 6,
    var maxTierCaptureCount: Int = 512,
    var spawnCapEnabled: Boolean = true,
    var tierSpeciesSurplus: Float = 0.4f,
    var tierSpawnLevelsEnabled: Boolean = true,
    var averageSpawnLevelStart: Float = 0.4f,
    var averageSpawnLevelEnd: Float = 0.8f,
    var averageSpawnLevelChance: Float = 0.65f,
    val spawnGroups: MutableMap<String, SpawnGroupConfigEntry> = linkedMapOf(),
    var playerSharedTieringEnabled: Boolean = false,
    var tierShareBlocklist: MutableList<String> = mutableListOf(),
    var caracatronScreenColor: String = DynamicConfig.DEFAULT_CARACATRON_COLOR,
    var caracatronScanSpeed: Int = 2
)

/**
 * High-performance JSON configuration manager handling loading, saving, and querying mod settings.
 *
 * Enforces dynamic bounds and sanitization rules for capture rates and berry parameters.
 * Creates and reads `.minecraft/config/caraca_customs/server_config.json`.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object DynamicConfig {

    /**
     * Highest berry yield the mod assigns to a tree, Rich Mulch boost included.
     */
    const val MAX_YIELD_CAP = 6

    private const val GLOBAL_MIN_YIELD_BOUND = 1
    private const val GLOBAL_MIN_GROWTH_BOUND = 1

    private const val GLOBAL_BALL_RATE_FLOOR = 0.01f
    private const val UNITARY_BALL_RATE_FLOOR = 0.0f
    private const val ABSOLUTE_BALL_RATE_CEILING = 10000.0f

    private const val MIN_FORTUNE_CHANCE = 0.0f
    private const val MAX_FORTUNE_CHANCE = 1.0f

    /**
     * Bounds of [ConfigData.tierCount].
     */
    const val MIN_TIER_COUNT = 1
    const val MAX_TIER_COUNT = 20

    /**
     * Bounds of [ConfigData.maxTierCaptureCount].
     */
    const val MIN_TIER_CAPTURE_COUNT = 50
    const val MAX_TIER_CAPTURE_COUNT = 1025

    /**
     * Highest value of [ExpShareConfigEntry.expShareItemBonus].
     */
    const val MAX_EXP_SHARE_ITEM_BONUS = 100.0f

    /**
     * Bounds of the [SpawnGroupConfigEntry] fields.
     */
    const val MIN_SPAWN_RATE_MULTIPLIER = 0.01f
    const val MAX_SPAWN_RATE_MULTIPLIER = 100.0f
    const val MAX_REDUCED_RATE_INTERVAL = 3
    const val MAX_REDUCED_SPAWN_RATE = 99.99f
    const val SPAWN_RATE_STEP = 0.01f
    const val MAX_EARLY_TIER_SPAWN_RATE = 1.0f

    /**
     * Bounds of [ConfigData.tierSpeciesSurplus].
     */
    const val MAX_TIER_SPECIES_SURPLUS = 2.0f

    /**
     * Default value of [ConfigData.caracatronScreenColor].
     */
    const val DEFAULT_CARACATRON_COLOR = "cerulean"

    /**
     * Caracatron screen colors whose folder was renamed, by old name. The blue screen became Cerulean in 1.25.3.
     */
    private val RENAMED_CARACATRON_COLORS = mapOf("blue" to "cerulean")

    /**
     * Bounds of [ConfigData.caracatronScanSpeed].
     */
    const val MIN_CARACATRON_SCAN_SPEED = 1
    const val MAX_CARACATRON_SCAN_SPEED = 5

    private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    private val NETWORK_GSON: Gson = Gson()
    private val CONFIG_FILE: File = FabricLoader.getInstance().configDir
        .resolve("caraca_customs")
        .resolve("server_config.json")
        .toFile()

    /**
     * Every berry shipped by Cobblemon 1.8.4. Berries are datapack-driven and not yet loaded during
     * mod initialization, so their identifiers are listed explicitly.
     */
    private val ALL_BERRY_KEYS = listOf(
        "aguav", "apicot", "aspear", "babiri", "belue", "bluk", "charti", "cheri", "chesto", "chilan",
        "chople", "coba", "colbur", "cornn", "custap", "durin", "eggant", "enigma", "figy", "ganlon",
        "grepa", "haban", "hondew", "hopo", "iapapa", "jaboca", "kasib", "kebia", "kee", "kelpsy",
        "lansat", "leppa", "liechi", "lum", "mago", "magost", "maranga", "micle", "nanab", "nomel",
        "occa", "oran", "pamtre", "passho", "payapa", "pecha", "persim", "petaya", "pinap", "pomeg",
        "qualot", "rabuta", "rawst", "razz", "rindo", "roseli", "rowap", "salac", "shuca", "sitrus",
        "spelon", "starf", "tamato", "tanga", "touga", "wacan", "watmel", "wepear", "wiki", "yache"
    ).map { "cobblemon:${it}_berry" }

    /**
     * Active configuration instance held in memory.
     * Accessible from Java via [DynamicConfig.getData].
     */
    @JvmStatic
    var data: ConfigData = ConfigData()
        private set

    /**
     * Loads the configuration file from disk or creates a default one if absent.
     */
    fun load() {
        if (!CONFIG_FILE.parentFile.exists()) {
            CONFIG_FILE.parentFile.mkdirs()
        }

        if (!CONFIG_FILE.exists()) {
            data = ConfigData()
            populateMissingDefaults()
            save()
            return
        }

        runCatching {
            FileReader(CONFIG_FILE).use { reader ->
                data = GSON.fromJson(reader, ConfigData::class.java) ?: ConfigData()
            }
        }.onFailure {
            data = ConfigData()
        }

        populateMissingDefaults()
        sanitizeAndValidateEntries()
        save()
    }

    /**
     * Serializes and saves the active in-memory configuration state back to disk.
     */
    fun save() {
        sanitizeAndValidateEntries()
        SpawnTierTable.invalidate()
        runCatching {
            FileWriter(CONFIG_FILE).use { writer ->
                GSON.toJson(data, writer)
            }
        }
    }

    /**
     * Changes the Caracatron screen color and writes it to the config file without touching any other value in it, so a
     * client connected to a server never saves the server's settings as its own.
     *
     * @param color Name of the color folder.
     */
    fun saveCaracatronScreenColor(color: String) {
        data.caracatronScreenColor = sanitizeCaracatronColor(color)
        val local = runCatching { FileReader(CONFIG_FILE).use { GSON.fromJson(it, ConfigData::class.java) } }.getOrNull() ?: return
        local.caracatronScreenColor = data.caracatronScreenColor
        runCatching {
            FileWriter(CONFIG_FILE).use { writer ->
                GSON.toJson(local, writer)
            }
        }
    }

    /**
     * Ensures all registered custom balls and berries exist in their respective override maps dynamically.
     */
    private fun populateMissingDefaults() {
        populateBallDefaults()
        populateBerryDefaults()
        populateSpawnGroupDefaults()
    }

    /**
     * Dynamically populates missing Poké Ball entries from [CustomBallRegistry].
     */
    private fun populateBallDefaults() {
        CustomBallRegistry.getRegisteredBalls().forEach { modularBall ->
            data.pokeBallOverrides.putIfAbsent(
                modularBall.props.nameId,
                PokeBallConfigEntry(0.0f, 0.0f)
            )
        }
    }

    /**
     * Dynamically populates missing Berry entries from Cobblemon's [Berries] registry.
     */
    private fun populateBerryDefaults() {
        ALL_BERRY_KEYS.forEach { key ->
            data.berryOverrides.putIfAbsent(
                key,
                BerryConfigEntry(0, 0, 0, 0)
            )
        }
    }

    /**
     * Populates missing spawn group entries with the defaults of each [SpawnGroup], in declaration order.
     */
    private fun populateSpawnGroupDefaults() {
        SpawnGroup.entries.forEach { group ->
            data.spawnGroups.putIfAbsent(group.id, group.defaultConfig)
        }
    }

    /**
     * Keeps a spawn group entry within its bounds:
     * - the start tier between 1 and the tier count;
     * - the max multiplier between 0.01 and 100, the interval between 0 and 3 and the early tier rate between 0.01 and 1;
     * - the reduction tier above the start tier and up to the tier count, or `0` (disabled) when that's impossible;
     * - the reduced rate between 0.01 and 99.99 and at least 0.01 below the max multiplier, or `0` (disabled).
     *
     * @param entry Target spawn group entry.
     * @param tierCount Amount of tiers configured.
     */
    private fun sanitizeSpawnGroup(entry: SpawnGroupConfigEntry, tierCount: Int) {
        entry.startTier = entry.startTier.coerceIn(1, tierCount)
        entry.maxSpawnRateMultiplier = entry.maxSpawnRateMultiplier.coerceIn(MIN_SPAWN_RATE_MULTIPLIER, MAX_SPAWN_RATE_MULTIPLIER)
        entry.reducedRateInterval = entry.reducedRateInterval.coerceIn(0, MAX_REDUCED_RATE_INTERVAL)
        entry.earlyTierSpawnRate = entry.earlyTierSpawnRate.coerceIn(MIN_SPAWN_RATE_MULTIPLIER, MAX_EARLY_TIER_SPAWN_RATE)

        if (entry.reduceSpawnRateTier != 0) {
            entry.reduceSpawnRateTier = entry.reduceSpawnRateTier.coerceAtMost(tierCount)
            if (entry.reduceSpawnRateTier <= entry.startTier) {
                entry.reduceSpawnRateTier = 0
            }
        }

        if (entry.maxReducedSpawnRate <= 0.0f) {
            entry.maxReducedSpawnRate = 0.0f
            return
        }

        val reducedCeiling = minOf(MAX_REDUCED_SPAWN_RATE, entry.maxSpawnRateMultiplier - SPAWN_RATE_STEP)
        entry.maxReducedSpawnRate = if (reducedCeiling < MIN_SPAWN_RATE_MULTIPLIER) {
            0.0f
        } else {
            entry.maxReducedSpawnRate.coerceIn(MIN_SPAWN_RATE_MULTIPLIER, reducedCeiling)
        }
    }

    /**
     * Resolves the spawn rules of a group, falling back to its defaults when missing from the config.
     *
     * @param group Target spawn group.
     * @return The group's config entry.
     */
    @JvmStatic
    fun getSpawnGroupConfigFor(group: SpawnGroup): SpawnGroupConfigEntry {
        return data.spawnGroups[group.id] ?: group.defaultConfig
    }

    /**
     * Normalizes global and individual overrides with bounds, floors, ceilings, and boosted values.
     */
    private fun sanitizeAndValidateEntries() {
        data.globalHoeFortuneChance = data.globalHoeFortuneChance.coerceIn(MIN_FORTUNE_CHANCE, MAX_FORTUNE_CHANCE)
        data.tierCount = data.tierCount.coerceIn(MIN_TIER_COUNT, MAX_TIER_COUNT)
        data.maxTierCaptureCount = data.maxTierCaptureCount.coerceIn(MIN_TIER_CAPTURE_COUNT, MAX_TIER_CAPTURE_COUNT)
        data.tierSpeciesSurplus = data.tierSpeciesSurplus.coerceIn(0.0f, MAX_TIER_SPECIES_SURPLUS)
        data.averageSpawnLevelStart = data.averageSpawnLevelStart.coerceIn(0.0f, 1.0f)
        data.averageSpawnLevelEnd = data.averageSpawnLevelEnd.coerceIn(data.averageSpawnLevelStart, 1.0f)
        data.averageSpawnLevelChance = data.averageSpawnLevelChance.coerceIn(0.0f, 1.0f)
        data.spawnGroups.values.forEach { entry -> sanitizeSpawnGroup(entry, data.tierCount) }
        sanitizeGlobalDefaultBerryConfig(data.defaultBerryConfig)

        data.berryOverrides.values.forEach { entry ->
            sanitizeUnitaryBerryOverride(entry)
        }

        sanitizeGlobalPokeBallConfig(data.defaultRegularPokeBallConfig)
        sanitizeGlobalPokeBallConfig(data.defaultUltraPokeBallConfig)
        sanitizeGlobalPokeBallConfig(data.defaultCaracaBallConfig)

        data.pokeBallOverrides.values.forEach { entry ->
            sanitizeUnitaryPokeBallOverride(entry)
        }

        sanitizeExpShareConfig(data.defaultExpShareConfig)
        data.caracatronScreenColor = sanitizeCaracatronColor(data.caracatronScreenColor as String?)
        data.caracatronScanSpeed = data.caracatronScanSpeed.coerceIn(MIN_CARACATRON_SCAN_SPEED, MAX_CARACATRON_SCAN_SPEED)
        data.tierShareBlocklist = (data.tierShareBlocklist as MutableList<String>?).orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .toMutableList()
    }

    /**
     * Restores enum values Gson left as `null` after a typo in the file, keeps the multiplier between 0 and 1 and the
     * Exp. Share bonus between 0 and [MAX_EXP_SHARE_ITEM_BONUS], both with two decimals.
     *
     * @param entry Target Exp. Share All configuration entry.
     */
    /**
     * Turns a stored screen color into a valid folder name, lowercase (older files kept `BLUE`), falling back to
     * [DEFAULT_CARACATRON_COLOR] when it's missing or can't be a folder name. Colors renamed since then are moved to
     * their new folder ([RENAMED_CARACATRON_COLORS]).
     */
    private fun sanitizeCaracatronColor(color: String?): String {
        val folder = color?.trim()?.lowercase().orEmpty()
        if (folder.isEmpty() || '/' in folder || !ResourceLocation.isValidPath(folder)) return DEFAULT_CARACATRON_COLOR
        return RENAMED_CARACATRON_COLORS[folder] ?: folder
    }

    private fun sanitizeExpShareConfig(entry: ExpShareConfigEntry) {
        if ((entry.expDivisionMethod as ExpDivisionMethod?) == null) entry.expDivisionMethod = ExpDivisionMethod.MULTIPLIED
        if ((entry.expShareMethod as ExpShareMethod?) == null) entry.expShareMethod = ExpShareMethod.EQUALLY
        entry.expShareMultiplier = (entry.expShareMultiplier.coerceIn(0.0f, 1.0f) * 100).roundToInt() / 100.0f
        entry.expShareItemBonus = (entry.expShareItemBonus.coerceIn(0.0f, MAX_EXP_SHARE_ITEM_BONUS) * 100).roundToInt() / 100.0f
    }

    /**
     * Sanitizes global Poké Ball default values applying absolute floor 0.01, ceiling 10000.0,
     * and ensuring boosted rate never falls below default rate.
     *
     * @param entry Target global Poké Ball configuration entry.
     */
    private fun sanitizeGlobalPokeBallConfig(entry: PokeBallConfigEntry) {
        entry.defaultCaptureRate = entry.defaultCaptureRate.coerceIn(GLOBAL_BALL_RATE_FLOOR, ABSOLUTE_BALL_RATE_CEILING)
        entry.boostedCaptureRate = entry.boostedCaptureRate.coerceIn(GLOBAL_BALL_RATE_FLOOR, ABSOLUTE_BALL_RATE_CEILING)

        if (entry.boostedCaptureRate < entry.defaultCaptureRate) {
            entry.boostedCaptureRate = entry.defaultCaptureRate
        }
    }

    /**
     * Sanitizes individual Poké Ball override entries applying floor 0.0, ceiling 10000.0,
     * and ensuring boosted rate never falls below default rate.
     *
     * @param entry Target unitary Poké Ball configuration entry.
     */
    private fun sanitizeUnitaryPokeBallOverride(entry: PokeBallConfigEntry) {
        entry.defaultCaptureRate = entry.defaultCaptureRate.coerceIn(UNITARY_BALL_RATE_FLOOR, ABSOLUTE_BALL_RATE_CEILING)
        entry.boostedCaptureRate = entry.boostedCaptureRate.coerceIn(UNITARY_BALL_RATE_FLOOR, ABSOLUTE_BALL_RATE_CEILING)

        if (entry.boostedCaptureRate < entry.defaultCaptureRate) {
            entry.boostedCaptureRate = entry.defaultCaptureRate
        }
    }

    /**
     * Enforces absolute non-zero floors and upper yield ceilings exclusively on global default berry configuration.
     *
     * @param defaultEntry Global default configuration.
     */
    private fun sanitizeGlobalDefaultBerryConfig(defaultEntry: BerryConfigEntry) {
        defaultEntry.minYield = defaultEntry.minYield.coerceIn(GLOBAL_MIN_YIELD_BOUND, MAX_YIELD_CAP)
        defaultEntry.maxYield = defaultEntry.maxYield.coerceIn(defaultEntry.minYield, MAX_YIELD_CAP)

        defaultEntry.minGrowthTime = defaultEntry.minGrowthTime.coerceAtLeast(GLOBAL_MIN_GROWTH_BOUND)
        defaultEntry.maxGrowthTime = defaultEntry.maxGrowthTime.coerceAtLeast(defaultEntry.minGrowthTime)
    }

    /**
     * Validates individual berry overrides, permitting 0 values while guaranteeing that maximum yield values
     * do not exceed MAX_YIELD_CAP (6) and are never lower than specified minimum yield values when defined.
     *
     * @param override Unitary berry configuration entry.
     */
    private fun sanitizeUnitaryBerryOverride(override: BerryConfigEntry) {
        if (override.minYield > MAX_YIELD_CAP) {
            override.minYield = MAX_YIELD_CAP
        }

        if (override.maxYield > MAX_YIELD_CAP) {
            override.maxYield = MAX_YIELD_CAP
        }

        if (override.minYield > 0 && override.maxYield > 0 && override.maxYield < override.minYield) {
            override.maxYield = override.minYield
        }

        if (override.minGrowthTime > 0 && override.maxGrowthTime > 0 && override.maxGrowthTime < override.minGrowthTime) {
            override.maxGrowthTime = override.minGrowthTime
        }
    }

    /**
     * Resolves the effective [BerryConfigEntry] for a given berry path identifier.
     *
     * Merges non-positive fields with [ConfigData.defaultBerryConfig] fallbacks and guarantees max bounds
     * strictly conform to limits at runtime.
     *
     * @param berryId Full string identifier of the berry.
     * @return Evaluated berry entry with default fallbacks and maximum limits applied.
     */
    @JvmStatic
    fun getConfigFor(berryId: String): BerryConfigEntry {
        val defaultEntry = data.defaultBerryConfig
        val override = data.berryOverrides[berryId] ?: return defaultEntry

        val resolvedMinYield = if (override.minYield <= 0) defaultEntry.minYield else override.minYield.coerceAtMost(MAX_YIELD_CAP)
        val unconstrainedMaxYield = if (override.maxYield <= 0) defaultEntry.maxYield else override.maxYield
        val resolvedMaxYield = unconstrainedMaxYield.coerceAtLeast(resolvedMinYield).coerceAtMost(MAX_YIELD_CAP)

        val resolvedMinGrowth = if (override.minGrowthTime <= 0) defaultEntry.minGrowthTime else override.minGrowthTime
        val unconstrainedMaxGrowth = if (override.maxGrowthTime <= 0) defaultEntry.maxGrowthTime else override.maxGrowthTime
        val resolvedMaxGrowth = unconstrainedMaxGrowth.coerceAtLeast(resolvedMinGrowth)

        return BerryConfigEntry(
            minYield = resolvedMinYield,
            maxYield = resolvedMaxYield,
            minGrowthTime = resolvedMinGrowth,
            maxGrowthTime = resolvedMaxGrowth
        )
    }

    /**
     * Resolves the effective [PokeBallConfigEntry] for a given custom Poké Ball identifier.
     *
     * Evaluates category fallbacks based on ball naming/package conventions and merges individual 0-value fallbacks.
     *
     * @param ballId Path identifier string of the Poké Ball.
     * @return Evaluated capture rate entry with fallbacks applied.
     */
    @JvmStatic
    fun getPokeBallConfigFor(ballId: String): PokeBallConfigEntry {
        val cleanId = ballId.lowercase()
        val categoryDefault = resolveCategoryDefault(cleanId)
        val override = data.pokeBallOverrides[cleanId] ?: return categoryDefault

        val finalDefaultRate = if (override.defaultCaptureRate <= 0.0f) categoryDefault.defaultCaptureRate else override.defaultCaptureRate
        val rawBoostedRate = if (override.boostedCaptureRate <= 0.0f) categoryDefault.boostedCaptureRate else override.boostedCaptureRate
        val finalBoostedRate = rawBoostedRate.coerceAtLeast(finalDefaultRate)

        return PokeBallConfigEntry(finalDefaultRate, finalBoostedRate)
    }

    /**
     * Serializes the current active configuration into a JSON string for network transmission.
     *
     * Leverages the existing GSON instance to convert the in-memory [ConfigData]
     * object into a raw UTF-8 compatible string.
     *
     * @return Raw JSON string representation of the active configuration.
     */
    fun serializeToString(): String {
        return NETWORK_GSON.toJson(data)
    }

    /**
     * Overwrites the active in-memory configuration with data provided by the server.
     *
     * Parses the incoming JSON payload and instantly applies bounds validation
     * to ensure structural integrity before replacing the client's local data. The client keeps its own
     * [ConfigData.caracatronScreenColor], since it's a visual preference.
     *
     * @param json Valid JSON string received from the dedicated server's payload.
     */
    fun loadFromJsonString(json: String) {
        val serverData = runCatching { GSON.fromJson(json, ConfigData::class.java) }.getOrNull() ?: return
        serverData.caracatronScreenColor = data.caracatronScreenColor
        data = serverData
        SpawnTierTable.invalidate()
        sanitizeAndValidateEntries()
    }

    /**
     * Determines the fallback default configuration from the Poké Ball's [BallTier].
     *
     * @param cleanId Lowercase path identifier of the Poké Ball.
     * @return Resolved default [PokeBallConfigEntry] for the ball's tier, or the regular one for unknown balls.
     */
    private fun resolveCategoryDefault(cleanId: String): PokeBallConfigEntry {
        return when (CustomBallRegistry.getBallByName(cleanId)?.props?.tier) {
            BallTier.CARACA -> data.defaultCaracaBallConfig
            BallTier.ULTRA -> data.defaultUltraPokeBallConfig
            else -> data.defaultRegularPokeBallConfig
        }
    }
}