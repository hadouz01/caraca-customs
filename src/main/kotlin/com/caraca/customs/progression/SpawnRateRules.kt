package com.caraca.customs.progression

import com.caraca.customs.config.SpawnGroupConfigEntry

/**
 * Spawn rate multiplier of a [SpawnGroup] for each tier, following its [SpawnGroupConfigEntry]:
 *
 * - **Before the start tier:** blocked, unless within the reduced rate interval. The tier right before the start tier gets
 *   the early tier rate (25% by default) of the max multiplier, and each earlier one gets half of the next (12.5%, then 6.25%).
 * - **From the start tier:** the max multiplier.
 * - **With a reduction:** from the start tier to the reduction tier, the rate goes down in equal steps until it reaches
 *   the max reduced rate, and stays there in the following tiers.
 *
 * Example: start tier 4, max multiplier 3, interval 2, reduction at tier 6 with a reduced rate of 1:
 * tier 1 = 0, tier 2 = 0.375, tier 3 = 0.75, tier 4 = 3, tier 5 = 2 and tier 6 = 1.
 *
 * @author hadouz01
 * @since 1.5.0
 */
object SpawnRateRules {

    /**
     * Calculates a group's spawn rate multiplier at a tier.
     *
     * @param entry Spawn rules of the group.
     * @param tier Player tier.
     * @return The multiplier applied to the species' original spawn rate. `0` means it can't spawn.
     */
    fun multiplierAt(entry: SpawnGroupConfigEntry, tier: Int): Float {
        val start = entry.startTier
        val max = entry.maxSpawnRateMultiplier

        if (tier < start) {
            val tiersBefore = start - tier
            if (tiersBefore > entry.reducedRateInterval) return 0.0f
            var rate = max * entry.earlyTierSpawnRate
            repeat(tiersBefore - 1) { rate /= 2 }
            return rate
        }

        val reduceTier = entry.reduceSpawnRateTier
        val reduced = entry.maxReducedSpawnRate
        if (reduceTier <= start || reduced <= 0.0f) return max
        if (tier >= reduceTier) return reduced

        val stepReduction = (max - reduced) / (reduceTier - start)
        return max - stepReduction * (tier - start)
    }
}
