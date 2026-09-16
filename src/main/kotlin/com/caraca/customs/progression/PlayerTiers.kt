package com.caraca.customs.progression

import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.pokedex.CaughtCount
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Snapshot of a player's progression tier.
 *
 * @property tier Current tier, from 1 to [tierCount].
 * @property tierCount Amount of tiers configured.
 * @property caughtSpecies Species registered as caught in the player's Pokédex.
 * @property levelCap Highest level the player's Pokémon can reach through experience.
 * @property nextTierCaptures Caught species needed for the next tier, or `null` at the last tier.
 * @property nextLevelCap Level cap unlocked by the next tier, or `null` at the last tier.
 *
 * @author hadouz01
 * @since 1.4.0
 */
data class TierInfo(
    val tier: Int,
    val tierCount: Int,
    val caughtSpecies: Int,
    val levelCap: Int,
    val nextTierCaptures: Int?,
    val nextLevelCap: Int?
) {
    /**
     * Whether this is the last tier.
     */
    val isMaxTier: Boolean get() = tier >= tierCount
}

/**
 * Player tiers unlocked by the amount of different species caught.
 *
 * Both progressions split their total into equal integer steps and give the remainder to the last tier:
 * - **Level caps:** tier `t` caps at `t * floor(X / Tq)`, `X` being Cobblemon's max level, and the last tier caps at `X`.
 *   With `X = 100` and `Tq = 6`: 16, 32, 48, 64, 80 and 100.
 * - **Capture thresholds:** tier 1 needs no captures, tier `t` needs `(t - 1) * floor(M / Tq)` and the last tier
 *   needs `M`, the configured max capture count. With `M = 512` and `Tq = 6`: 0, 85, 170, 255, 340 and 512.
 *
 * Tiers are cached per player, since natural spawns check them constantly. [LevelCapHandler] keeps the cache up to date
 * when players join, leave, catch new species or the config is reloaded.
 *
 * @author hadouz01
 * @since 1.4.0
 */
object PlayerTiers {

    /**
     * Amount of tiers currently configured.
     */
    val tierCount: Int get() = DynamicConfig.data.tierCount

    /**
     * Cobblemon's configured max Pokémon level.
     */
    val maxLevel: Int get() = Cobblemon.config.maxPokemonLevel.coerceAtLeast(1)

    /**
     * Level cap of a tier.
     *
     * @param tier Tier from 1 to [tierCount].
     * @return The highest level reachable at that tier.
     */
    fun levelCapFor(tier: Int): Int {
        val count = tierCount
        val clampedTier = tier.coerceIn(1, count)
        if (clampedTier == count) return maxLevel
        val step = maxLevel / count
        return (clampedTier * step).coerceIn(1, maxLevel)
    }

    /**
     * Caught species needed to reach a tier.
     *
     * @param tier Tier from 1 to [tierCount].
     * @return The capture threshold of that tier.
     */
    fun captureThresholdFor(tier: Int): Int {
        val count = tierCount
        val clampedTier = tier.coerceIn(1, count)
        val maxCaptures = DynamicConfig.data.maxTierCaptureCount
        if (clampedTier == 1) return 0
        if (clampedTier == count) return maxCaptures
        val step = maxCaptures / count
        return (clampedTier - 1) * step
    }

    private val cachedTiers = ConcurrentHashMap<UUID, Int>()

    /**
     * Gets a player's tier from the cache, computing it on the first call.
     *
     * @param playerId Player UUID.
     * @return The player's current tier.
     */
    fun cachedTier(playerId: UUID): Int = cachedTiers.getOrPut(playerId) { tierFor(caughtSpecies(playerId)) }

    /**
     * Recomputes and caches a player's tier.
     *
     * @param playerId Player UUID.
     * @return The previously cached tier, or `null` if there was none.
     */
    fun refreshTier(playerId: UUID): Int? = cachedTiers.put(playerId, tierFor(caughtSpecies(playerId)))

    /**
     * Drops a player's cached tier, used when they leave.
     *
     * @param playerId Player UUID.
     */
    fun forgetTier(playerId: UUID) {
        cachedTiers.remove(playerId)
    }

    /**
     * Tier reached with a given amount of caught species.
     *
     * @param caughtSpecies Species registered as caught.
     * @return The highest tier whose threshold is met.
     */
    fun tierFor(caughtSpecies: Int): Int {
        return (tierCount downTo 1).first { caughtSpecies >= captureThresholdFor(it) }
    }

    /**
     * Counts the species registered as caught in a player's Pokédex. Works for offline players too.
     *
     * @param playerId Player UUID.
     * @return Amount of caught species.
     */
    fun caughtSpecies(playerId: UUID): Int {
        return Cobblemon.playerDataManager.getPokedexData(playerId).getGlobalCalculatedValue(CaughtCount)
    }

    /**
     * Builds the full tier snapshot of a player.
     *
     * @param playerId Player UUID.
     * @return The player's [TierInfo].
     */
    fun infoFor(playerId: UUID): TierInfo {
        val caught = caughtSpecies(playerId)
        val tier = tierFor(caught)
        val hasNext = tier < tierCount
        return TierInfo(
            tier = tier,
            tierCount = tierCount,
            caughtSpecies = caught,
            levelCap = levelCapFor(tier),
            nextTierCaptures = if (hasNext) captureThresholdFor(tier + 1) else null,
            nextLevelCap = if (hasNext) levelCapFor(tier + 1) else null
        )
    }
}
