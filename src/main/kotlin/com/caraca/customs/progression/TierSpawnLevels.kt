package com.caraca.customs.progression

import com.caraca.customs.config.DynamicConfig
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Spawn level window of a tier, in levels.
 *
 * @property levels Every level a natural spawn can have, from the tier's minimum spawn level to its level cap.
 * @property average Levels that spawn the most.
 *
 * @author hadouz01
 * @since 1.9.0
 */
data class SpawnLevelWindow(val levels: IntRange, val average: IntRange)

/**
 * Levels of natural spawns around a player, following the player's tier.
 *
 * Spawns go from half the level cap minus 3, 5 or 10 (cap below 50, below 80, from 80 on) up to the cap. Levels in the
 * average band ([ConfigData.averageSpawnLevelStart] to [ConfigData.averageSpawnLevelEnd]) are the most common and get at
 * least [ConfigData.averageSpawnLevelChance] of the spawns. Outside it they fade down to [EDGE_WEIGHT], faster below
 * than above, so spawns lean a bit towards higher levels. With 6 tiers, tier 1 spawns 5 to 16, mostly 9 to 14.
 *
 * @author hadouz01
 * @since 1.9.0
 */
object TierSpawnLevels {

    /**
     * Weight of the minimum level and the level cap compared to the closest level of the average band.
     */
    private const val EDGE_WEIGHT = 0.1

    /**
     * Lowest level a tier with a given level cap can spawn.
     *
     * @param levelCap The tier's level cap.
     * @return Half the cap minus 3 (cap under 50), 5 (cap under 80) or 10 (cap 80 or higher), at least 1.
     */
    fun minimumLevelFor(levelCap: Int): Int {
        val reduction = when {
            levelCap < 50 -> 3
            levelCap < 80 -> 5
            else -> 10
        }
        return (levelCap / 2 - reduction).coerceIn(1, max(1, levelCap))
    }

    /**
     * Builds the spawn level window of a tier.
     *
     * @param tier Tier from 1 to [PlayerTiers.tierCount].
     * @return The tier's level window.
     */
    fun windowFor(tier: Int): SpawnLevelWindow {
        val cap = PlayerTiers.levelCapFor(tier.coerceIn(1, PlayerTiers.tierCount))
        val lowest = minimumLevelFor(cap)
        val span = cap - lowest
        val data = DynamicConfig.data
        val averageStart = (lowest + (span * data.averageSpawnLevelStart).roundToInt()).coerceIn(lowest, cap)
        val averageEnd = (lowest + (span * data.averageSpawnLevelEnd).roundToInt()).coerceIn(averageStart, cap)
        return SpawnLevelWindow(lowest..cap, averageStart..averageEnd)
    }

    /**
     * Chance of each level of a tier's window being rolled.
     *
     * @param tier Tier of the player the Pokémon spawns around.
     * @return Every level of the window with its chance, adding up to 1.
     */
    fun chancesFor(tier: Int): List<Pair<Int, Double>> {
        val window = windowFor(tier)
        val average = window.average
        val below = window.levels.first until average.first
        val above = average.last + 1..window.levels.last

        val belowCount = below.count()
        val aboveCount = above.count()
        val belowWeights = below.map { level ->
            val progress = (belowCount - (average.first - level)).toDouble() / belowCount
            level to EDGE_WEIGHT + (1 - EDGE_WEIGHT) * progress * progress
        }
        val aboveWeights = above.map { level ->
            val progress = (aboveCount - (level - average.last)).toDouble() / aboveCount
            level to EDGE_WEIGHT + (1 - EDGE_WEIGHT) * progress
        }
        val tails = belowWeights + aboveWeights
        val tailWeight = tails.sumOf { it.second }

        val averageWeight = average.count().toDouble()
        val minimumShare = DynamicConfig.data.averageSpawnLevelChance.toDouble()
        val naturalShare = averageWeight / (averageWeight + tailWeight)
        val tailScale = when {
            tailWeight <= 0.0 || naturalShare >= minimumShare -> 1.0
            minimumShare >= 1.0 -> 0.0
            else -> (1 - minimumShare) / minimumShare * averageWeight / tailWeight
        }
        val total = averageWeight + tailWeight * tailScale
        val averageChances = average.map { it to 1.0 / total }
        val tailChances = tails.map { (level, weight) -> level to weight * tailScale / total }
        return (averageChances + tailChances).sortedBy { it.first }
    }

    /**
     * Rolls the level of a natural spawn for a tier.
     *
     * @param tier Tier of the player the Pokémon spawns around.
     * @param random Random source.
     * @return The spawn level.
     */
    fun rollLevel(tier: Int, random: Random = Random.Default): Int {
        val chances = chancesFor(tier)
        var roll = random.nextDouble(chances.sumOf { it.second })
        for ((level, chance) in chances) {
            roll -= chance
            if (roll < 0) return level
        }
        return chances.last().first
    }
}
