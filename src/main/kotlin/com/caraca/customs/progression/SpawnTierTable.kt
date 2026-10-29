package com.caraca.customs.progression

import com.caraca.CaracaCustoms
import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools
import com.cobblemon.mod.common.api.spawning.detail.PokemonHerdSpawnDetail
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory
import kotlin.math.ceil

/**
 * Final spawn rate multiplier of every [SpawnGroup] at every tier.
 *
 * Starts from [SpawnRateRules], then makes sure each tier offers enough species to reach the next one: if the species
 * that can naturally spawn in a tier (counted from Cobblemon's world spawn pool, so species without natural spawns don't
 * count) are fewer than what the next tier needs plus [com.caraca.customs.config.ConfigData.tierSpeciesSurplus],
 * the next groups by starting tier (the ones with more spawnable species first, on ties) are unlocked early, at their
 * early tier rate, until the gap is filled. A group unlocked this way keeps spawning in the following tiers.
 *
 * With the default settings no group needs to be unlocked early. The table is rebuilt when the server starts, after
 * datapack reloads, and lazily after the config or the spawn groups change.
 *
 * @author hadouz01
 * @since 1.5.3
 */
object SpawnTierTable {

    private val LOGGER = LoggerFactory.getLogger(CaracaCustoms.MOD_ID)

    /**
     * Built multipliers and the tiers where groups had to be unlocked early.
     *
     * @property rates Multiplier of every group at every tier, indexed by `tier - 1`.
     * @property earlyUnlocks First tier each group was unlocked in by the species guarantee.
     */
    private class Table(val rates: Map<SpawnGroup, FloatArray>, val earlyUnlocks: Map<SpawnGroup, Int>)

    @Volatile
    private var table: Table? = null

    /**
     * Rebuilds the table whenever the server starts or its datapacks are reloaded, since the spawn pool changes too.
     */
    fun register() {
        ServerLifecycleEvents.SERVER_STARTED.register { rebuild() }
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { _, _, _ -> rebuild() }
    }

    /**
     * Rebuilds the table right away, logging any group unlocked early.
     */
    fun rebuild() {
        table = build()
    }

    /**
     * Marks the table as outdated, so it's rebuilt on the next lookup.
     */
    fun invalidate() {
        table = null
    }

    /**
     * Gets a group's spawn rate multiplier at a tier.
     *
     * @param group Target spawn group.
     * @param tier Player tier.
     * @return The multiplier applied to the species' original spawn rate. `0` means it can't spawn.
     */
    fun multiplierAt(group: SpawnGroup, tier: Int): Float {
        val rates = current().rates[group] ?: return 0.0f
        return rates[(tier.coerceIn(1, rates.size)) - 1]
    }

    /**
     * Gets the tier a group belongs to: its starting tier, or the tier the species guarantee unlocked it in, if earlier.
     * Reduced rate tiers before the starting tier don't count, so a group starting at tier 5 belongs to tier 5 even
     * though it already spawns at a lower rate in tier 4.
     *
     * @param group Target spawn group.
     * @return The group's tier, from 1 to the tier count.
     */
    fun homeTierOf(group: SpawnGroup): Int {
        val startTier = DynamicConfig.getSpawnGroupConfigFor(group).startTier.coerceIn(1, PlayerTiers.tierCount)
        val unlockedAt = current().earlyUnlocks[group] ?: return startTier
        return minOf(startTier, unlockedAt)
    }

    private fun current(): Table = table ?: build().also { table = it }

    private fun build(): Table {
        val tierCount = PlayerTiers.tierCount
        val configs = SpawnGroup.entries.associateWith { DynamicConfig.getSpawnGroupConfigFor(it) }
        val rates = SpawnGroup.entries.associateWith { group ->
            FloatArray(tierCount) { index -> SpawnRateRules.multiplierAt(configs.getValue(group), index + 1) }
        }

        val spawnable = spawnableSpecies() ?: return Table(rates, emptyMap())
        val speciesPerGroup = SpawnGroup.entries.associateWith { group ->
            spawnable.count { SpawnGroupRegistry.groupOf(it) == group }
        }
        val ungrouped = spawnable.count { SpawnGroupRegistry.groupOf(it) == null }
        val surplus = DynamicConfig.data.tierSpeciesSurplus
        val unlockedEarly = linkedMapOf<SpawnGroup, Int>()

        for (tier in 1 until tierCount) {
            val index = tier - 1
            val needed = ceil(PlayerTiers.captureThresholdFor(tier + 1) * (1 + surplus)).toInt()
            val active = SpawnGroup.entries.filter { rates.getValue(it)[index] > 0.0f || it in unlockedEarly }
            var available = ungrouped + active.sumOf { speciesPerGroup.getValue(it) }

            SpawnGroup.entries
                .filter { it !in active && speciesPerGroup.getValue(it) > 0 }
                .sortedWith(compareBy<SpawnGroup> { configs.getValue(it).startTier }.thenByDescending { speciesPerGroup.getValue(it) })
                .forEach { group ->
                    if (available >= needed) return@forEach
                    unlockedEarly[group] = tier
                    available += speciesPerGroup.getValue(group)
                    LOGGER.info("Tier {} needs more spawnable species to reach tier {}, unlocking {} early", tier, tier + 1, group.id)
                }

            unlockedEarly.keys.forEach { group ->
                val groupRates = rates.getValue(group)
                if (groupRates[index] <= 0.0f) {
                    val config = configs.getValue(group)
                    groupRates[index] = config.maxSpawnRateMultiplier * config.earlyTierSpawnRate
                }
            }

            if (available < needed) {
                LOGGER.warn("Tier {} only has {} spawnable species, but reaching tier {} needs {}", tier, available, tier + 1, needed)
            }
        }

        return Table(rates, unlockedEarly)
    }

    /**
     * Collects every species with natural spawns in Cobblemon's world spawn pool, herd members included.
     *
     * @return The spawnable species, or `null` when the spawn pool isn't loaded (e.g. on the client).
     */
    fun spawnableSpecies(): Set<ResourceLocation>? {
        val details = runCatching { CobblemonSpawnPools.WORLD_SPAWN_POOL.details.toList() }.getOrNull() ?: return null
        if (details.isEmpty()) return null
        return details.asSequence()
            .flatMap { detail ->
                when (detail) {
                    is PokemonSpawnDetail -> sequenceOf(detail.pokemon.species)
                    is PokemonHerdSpawnDetail -> detail.herdablePokemon.asSequence().map { it.pokemon.species }
                    else -> emptySequence()
                }
            }
            .filterNotNull()
            .mapNotNull { SpawnGroupRegistry.parseSpecies(it) }
            .toSet()
    }
}
