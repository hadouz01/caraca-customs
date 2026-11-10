package com.caraca.customs.config

import com.caraca.customs.balls.BallMaterials
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
 * Settings of a berry.
 *
 * @property minYield Lowest base yield on harvest.
 * @property maxYield Highest base yield on harvest.
 * @property minGrowthTime Shortest growth time, in minutes, from stage 0 to 3.
 * @property maxGrowthTime Longest growth time, in minutes, from stage 0 to 3.
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
 * Catch rates of a custom Poké Ball.
 *
 * @property defaultCaptureRate Catch rate multiplier in normal conditions.
 * @property boostedCaptureRate Catch rate multiplier when the ball's type or status condition is met.
 *
 * @author hadouz01
 * @since 1.0.0
 */
data class PokeBallConfigEntry(
    var defaultCaptureRate: Float = 0.0f,
    var boostedCaptureRate: Float = 0.0f
)

/**
 * Spawn rules of a spawn group in the tier spawn cap. Rates multiply the species' original spawn rate (`1.0` keeps it).
 *
 * @property startTier Tier where the group starts spawning at [maxSpawnRateMultiplier].
 * @property maxSpawnRateMultiplier Spawn rate multiplier reached at [startTier], from 0.01 to 100.
 * @property reducedRateInterval How many tiers before [startTier] the group already spawns, from 0 to 3. The tier right
 * before gets [earlyTierSpawnRate] of [maxSpawnRateMultiplier], and each earlier one gets half of the next.
 * @property earlyTierSpawnRate Fraction of [maxSpawnRateMultiplier] for the tier right before [startTier], from 0.01 to 1
 * (25% by default). Also used when the group gets unlocked early to fill a tier.
 * @property reduceSpawnRateTier Tier where the group reaches [maxReducedSpawnRate], always above [startTier]. `0` turns
 * the reduction off.
 * @property maxReducedSpawnRate Spawn rate multiplier reached at [reduceSpawnRateTier], from 0.01 to 99.99 and at least
 * 0.01 below [maxSpawnRateMultiplier]. `0` turns the reduction off. The rate goes down in equal steps until then.
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
 * The whole mod config. Maps keep their insertion order, so the file and Mod Menu keep the same layout.
 *
 * @property debugGrowthTick Shows a debug message every time a berry growth tick is calculated.
 * @property globalHoeFortuneChance Chance per Fortune level of an extra berry when harvesting with a hoe.
 * @property defaultBerryConfig Berry settings used when a berry has none, or a value isn't positive.
 * @property berryOverrides Settings of specific berries, by id.
 * @property defaultRegularPokeBallConfig Default catch rates of regular custom Poké Balls.
 * @property defaultUltraPokeBallConfig Default catch rates of ultra custom Poké Balls.
 * @property defaultCaracaBallConfig Default catch rates of Caraca Poké Balls.
 * @property defaultExpShareConfig How Exp. Share All splits the experience between the party.
 * @property pokeBallOverrides Catch rates of specific custom Poké Balls, by path.
 * @property customBallsEnabled Turns the custom Poké Balls on or off: recipes, loot, the native recipes that craft them,
 * the creative tab and the `pokeball=` aliases. Recipes follow on the next datapack reload.
 * @property regularBallMaterial Item regular Poké Ball recipes ask for, one of [BallMaterials.REGULAR_OPTIONS].
 * @property ultraBallMaterial Item Ultra Poké Ball recipes ask for, one of [BallMaterials.ULTRA_OPTIONS].
 * @property ultraBallCornerFill Item filling the empty corners of the Ultra Poké Ball recipes, one of
 * [BallMaterials.CORNER_OPTIONS], or [BallMaterials.NO_CORNER_FILL] to leave them empty.
 * @property berryMechanicsEnabled Turns the berry growth times, yields, Hoe Fortune bonus and mulch uses on or off.
 * @property dynamicFollowSpeedEnabled Turns the size-based follow distance and catch-up speed of party Pokémon on or off.
 * @property expShareAllEnabled Turns Exp. Share All on or off.
 * @property levelCapEnabled Turns the tier level caps on or off.
 * @property tierCount How many tiers there are, from 1 to 20.
 * @property maxTierCaptureCount Caught species needed for the last tier, from 50 to 1025.
 * @property spawnCapEnabled Turns the tier spawn cap on or off.
 * @property tierSpeciesSurplus Extra spawnable species each tier must have beyond what the next tier needs, as a
 * fraction, from 0 to 2 (0.4 by default). With 0.4, a tier leading to one that needs 100 species must have 140.
 * @property tierSpawnLevelsEnabled Turns spawn levels following the tier's level cap on or off.
 * @property averageSpawnLevelStart Where the most common spawn levels start, as a fraction of the way from the tier's
 * minimum spawn level to its level cap, from 0 to 1.
 * @property averageSpawnLevelEnd Where the most common spawn levels end, on the same scale, from 0 to 1.
 * @property averageSpawnLevelChance Minimum share of spawns rolling a level from the most common range, from 0 to 1.
 * @property spawnGroups Spawn rules of every [SpawnGroup], by id.
 * @property playerSharedTieringEnabled Turns player-shared tiering on or off.
 * @property tierShareBlocklist Players left out of the shared tiers, who progress with their own captures only.
 * @property caracatronScreenColor Caracatron screen color, a folder name in `textures/gui/caracatron/colors/`. It's each
 * player's own choice, so the server's value never replaces the client's.
 * @property caracatronScanSpeed How many times faster the Caracatron scans than a Pokédex, from 1 to 5.
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
    var regularBallMaterial: String = BallMaterials.REGULAR_OPTIONS.first(),
    var ultraBallMaterial: String = BallMaterials.ULTRA_OPTIONS.first(),
    var ultraBallCornerFill: String = BallMaterials.NO_CORNER_FILL,
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
 * Loads, saves and sanitizes the mod config, `config/caraca_customs/server_config.json`.
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
     * Every berry of Cobblemon 1.8.4. Berries come from datapacks and aren't loaded yet when the mod starts, so their ids
     * are listed here.
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
     * Config in use. Java code reads it through [DynamicConfig.getData].
     */
    @JvmStatic
    var data: ConfigData = ConfigData()
        private set

    /**
     * Loads the config file, or creates it with the defaults.
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
     * Sanitizes the config and saves it to disk.
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
     * Adds every ball, berry and spawn group missing from the config.
     */
    private fun populateMissingDefaults() {
        populateBallDefaults()
        populateBerryDefaults()
        populateSpawnGroupDefaults()
    }

    /**
     * Adds a blank override for every custom ball missing from the config.
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
     * Adds a blank override for every berry missing from the config.
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
     * Adds every spawn group missing from the config, with its defaults.
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
     * @param entry Spawn group entry.
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
     * Gets the spawn rules of a group, or its defaults when it's missing from the config.
     *
     * @param group Spawn group.
     */
    @JvmStatic
    fun getSpawnGroupConfigFor(group: SpawnGroup): SpawnGroupConfigEntry {
        return data.spawnGroups[group.id] ?: group.defaultConfig
    }

    /**
     * Keeps every value of the config within its bounds.
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
        data.regularBallMaterial = BallMaterials.sanitize(data.regularBallMaterial as String?, BallMaterials.REGULAR_OPTIONS)
        data.ultraBallMaterial = BallMaterials.sanitize(data.ultraBallMaterial as String?, BallMaterials.ULTRA_OPTIONS)
        data.ultraBallCornerFill = BallMaterials.sanitize(data.ultraBallCornerFill as String?, BallMaterials.CORNER_OPTIONS)
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
     * @param entry Exp. Share All configuration entry.
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
     * Keeps a tier's default catch rates between 0.01 and 10000, with the boosted rate never below the default one.
     *
     * @param entry Default catch rates of a tier.
     */
    private fun sanitizeGlobalPokeBallConfig(entry: PokeBallConfigEntry) {
        entry.defaultCaptureRate = entry.defaultCaptureRate.coerceIn(GLOBAL_BALL_RATE_FLOOR, ABSOLUTE_BALL_RATE_CEILING)
        entry.boostedCaptureRate = entry.boostedCaptureRate.coerceIn(GLOBAL_BALL_RATE_FLOOR, ABSOLUTE_BALL_RATE_CEILING)

        if (entry.boostedCaptureRate < entry.defaultCaptureRate) {
            entry.boostedCaptureRate = entry.defaultCaptureRate
        }
    }

    /**
     * Keeps a ball override between 0 and 10000, with the boosted rate never below the default one.
     *
     * @param entry Ball override.
     */
    private fun sanitizeUnitaryPokeBallOverride(entry: PokeBallConfigEntry) {
        entry.defaultCaptureRate = entry.defaultCaptureRate.coerceIn(UNITARY_BALL_RATE_FLOOR, ABSOLUTE_BALL_RATE_CEILING)
        entry.boostedCaptureRate = entry.boostedCaptureRate.coerceIn(UNITARY_BALL_RATE_FLOOR, ABSOLUTE_BALL_RATE_CEILING)

        if (entry.boostedCaptureRate < entry.defaultCaptureRate) {
            entry.boostedCaptureRate = entry.defaultCaptureRate
        }
    }

    /**
     * Keeps the default berry settings above zero and the yield under [MAX_YIELD_CAP].
     *
     * @param defaultEntry Default berry settings.
     */
    private fun sanitizeGlobalDefaultBerryConfig(defaultEntry: BerryConfigEntry) {
        defaultEntry.minYield = defaultEntry.minYield.coerceIn(GLOBAL_MIN_YIELD_BOUND, MAX_YIELD_CAP)
        defaultEntry.maxYield = defaultEntry.maxYield.coerceIn(defaultEntry.minYield, MAX_YIELD_CAP)

        defaultEntry.minGrowthTime = defaultEntry.minGrowthTime.coerceAtLeast(GLOBAL_MIN_GROWTH_BOUND)
        defaultEntry.maxGrowthTime = defaultEntry.maxGrowthTime.coerceAtLeast(defaultEntry.minGrowthTime)
    }

    /**
     * Keeps a berry override within bounds. `0` means "use the default", and the max yield stays between the min yield
     * and [MAX_YIELD_CAP].
     *
     * @param override Berry override.
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
     * Gets the settings of a berry: its override, with the non-positive fields taken from
     * [ConfigData.defaultBerryConfig], and the yield kept under [MAX_YIELD_CAP].
     *
     * @param berryId Full id of the berry.
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
     * Gets the catch rates of a custom ball: its override, with the `0` fields taken from its tier's defaults.
     *
     * @param ballId Path of the ball.
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
     * Writes the config as JSON to send it to clients.
     */
    fun serializeToString(): String {
        return NETWORK_GSON.toJson(data)
    }

    /**
     * Replaces the config with the one sent by the server, after sanitizing it. The client keeps its own
     * [ConfigData.caracatronScreenColor], since it's a personal choice.
     *
     * @param json Config sent by the server.
     */
    fun loadFromJsonString(json: String) {
        val serverData = runCatching { GSON.fromJson(json, ConfigData::class.java) }.getOrNull() ?: return
        serverData.caracatronScreenColor = data.caracatronScreenColor
        data = serverData
        SpawnTierTable.invalidate()
        sanitizeAndValidateEntries()
    }

    /**
     * Default catch rates for a ball's [BallTier], the regular ones for unknown balls.
     *
     * @param cleanId Lowercase path of the ball.
     */
    private fun resolveCategoryDefault(cleanId: String): PokeBallConfigEntry {
        return when (CustomBallRegistry.getBallByName(cleanId)?.props?.tier) {
            BallTier.CARACA -> data.defaultCaracaBallConfig
            BallTier.ULTRA -> data.defaultUltraPokeBallConfig
            else -> data.defaultRegularPokeBallConfig
        }
    }
}