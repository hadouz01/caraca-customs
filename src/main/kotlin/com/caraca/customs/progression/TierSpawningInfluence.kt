package com.caraca.customs.progression

import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition
import com.cobblemon.mod.common.api.spawning.spawner.FishingSpawnerFactory
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawnerFactory
import java.util.UUID

/**
 * Applies the tier spawn cap to the natural spawns around a player, fishing included.
 *
 * Pokémon whose species belongs to a [SpawnGroup] get their spawn weight multiplied by
 * [SpawnTierTable.multiplierAt] for the player's tier, and can't spawn at all when it's `0`. Species in no group,
 * and spawns that aren't Pokémon, are left untouched.
 *
 * @property playerId UUID of the player the spawns happen around.
 *
 * @author hadouz01
 * @since 1.5.0
 */
class TierSpawningInfluence(private val playerId: UUID) : SpawningInfluence {

    override fun affectSpawnable(detail: SpawnDetail, spawnablePosition: SpawnablePosition): Boolean {
        val multiplier = multiplierFor(detail) ?: return true
        return multiplier > 0.0f
    }

    override fun affectWeight(detail: SpawnDetail, spawnablePosition: SpawnablePosition, weight: Float): Float {
        val multiplier = multiplierFor(detail) ?: return weight
        return weight * multiplier
    }

    private fun multiplierFor(detail: SpawnDetail): Float? {
        if (!DynamicConfig.data.spawnCapEnabled) return null
        val species = (detail as? PokemonSpawnDetail)?.pokemon?.species ?: return null
        val speciesId = SpawnGroupRegistry.parseSpecies(species) ?: return null
        val group = SpawnGroupRegistry.groupOf(speciesId) ?: return null
        return SpawnTierTable.multiplierAt(group, PlayerTiers.cachedTier(playerId))
    }

    companion object {

        /**
         * Adds this influence to every new player spawner and fishing cast.
         */
        fun register() {
            PlayerSpawnerFactory.influenceBuilders.add { player -> TierSpawningInfluence(player.uuid) }
            FishingSpawnerFactory.positionInfluenceBuilders.add { context -> listOf(TierSpawningInfluence(context.player.uuid)) }
        }
    }
}
