package com.caraca.customs.progression

import com.caraca.customs.config.DynamicConfig
import com.caraca.customs.config.Feature
import com.cobblemon.mod.common.api.spawning.detail.PokemonHerdSpawnDetail
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnAction
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition
import com.cobblemon.mod.common.api.spawning.spawner.FishingSpawnerFactory
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawnerFactory
import com.cobblemon.mod.common.api.spawning.spawner.PokeSnackSpawnerFactory
import java.util.UUID
import kotlin.random.Random

/**
 * Tier spawn cap for natural spawns around a player, fishing and placed Poké Snacks included.
 *
 * Grouped species get their weight multiplied by [SpawnTierTable.multiplierAt] and can't spawn at `0`. Herds go member
 * by member: blocked only when every member is, weighted by their members' average, and a blocked (or much rarer)
 * member gets swapped for another one of the herd. It also sets spawn levels through [TierSpawnLevels] while
 * [Feature.SPAWN_LEVELS] is on.
 *
 * @property tier Gives the tier the spawns are capped by, asked again on every check.
 *
 * @author hadouz01
 * @since 1.5.0
 */
class TierSpawningInfluence(private val tier: () -> Int) : SpawningInfluence {

    /**
     * Caps the spawns around a player by their own tier.
     *
     * @param playerId UUID of the player the spawns happen around.
     */
    constructor(playerId: UUID) : this({ PlayerTiers.cachedTier(playerId) })

    override fun affectSpawnable(detail: SpawnDetail, spawnablePosition: SpawnablePosition): Boolean {
        val multiplier = multiplierFor(detail) ?: return true
        return multiplier > 0.0f
    }

    override fun affectWeight(detail: SpawnDetail, spawnablePosition: SpawnablePosition, weight: Float): Float {
        val multiplier = multiplierFor(detail) ?: return weight
        return weight * multiplier
    }

    override fun affectAction(action: SpawnAction<*>) {
        val spawn = action as? PokemonSpawnAction ?: return
        if (Feature.SPAWN_CAP.isEnabled) swapHerdMember(spawn)
        if (Feature.SPAWN_LEVELS.isEnabled) {
            val level = TierSpawnLevels.rollLevel(tier())
            spawn.levelRange = level..level
        }
    }

    private fun swapHerdMember(spawn: PokemonSpawnAction) {
        val herd = spawn.detail as? PokemonHerdSpawnDetail ?: return
        val members = herd.herdablePokemon.map { it to speciesMultiplier(it.pokemon.species) }
        val bestMultiplier = members.maxOfOrNull { it.second ?: 1.0f } ?: return
        if (bestMultiplier <= 0.0f) return

        val ownMultiplier = speciesMultiplier(spawn.props.species) ?: 1.0f
        if (ownMultiplier > 0.0f && Random.nextFloat() < ownMultiplier / bestMultiplier) return

        val replacement = members
            .filter { (member, multiplier) -> member.pokemon.species != spawn.props.species && (multiplier ?: 1.0f) > 0.0f }
            .weightedPick { (member, multiplier) -> member.weight * (multiplier ?: 1.0f) }
            ?.first ?: return

        spawn.props = replacement.pokemon
        replacement.levelRange?.let { range ->
            spawn.levelRange = spawn.levelRange.first.coerceIn(range.first, range.last)..spawn.levelRange.last.coerceIn(range.first, range.last)
        }
    }

    private fun multiplierFor(detail: SpawnDetail): Float? {
        if (!DynamicConfig.data.spawnCapEnabled) return null
        return when (detail) {
            is PokemonSpawnDetail -> speciesMultiplier(detail.pokemon.species)
            is PokemonHerdSpawnDetail -> herdMultiplier(detail)
            else -> null
        }
    }

    /**
     * Average multiplier of the herd's members weighted by their herd weight, or `null` when no member is in a group.
     */
    private fun herdMultiplier(herd: PokemonHerdSpawnDetail): Float? {
        val members = herd.herdablePokemon.map { it to speciesMultiplier(it.pokemon.species) }
        if (members.all { it.second == null }) return null
        if (members.all { (it.second ?: 1.0f) <= 0.0f }) return 0.0f
        val totalWeight = members.sumOf { it.first.weight.toDouble() }
        if (totalWeight <= 0.0) return members.maxOf { it.second ?: 1.0f }
        return (members.sumOf { (member, multiplier) -> member.weight.toDouble() * (multiplier ?: 1.0f) } / totalWeight).toFloat()
    }

    private fun speciesMultiplier(species: String?): Float? {
        val speciesId = species?.let(SpawnGroupRegistry::parseSpecies) ?: return null
        val group = SpawnGroupRegistry.groupOf(speciesId) ?: return null
        return SpawnTierTable.multiplierAt(group, tier())
    }

    private fun <T> List<T>.weightedPick(weight: (T) -> Float): T? {
        val total = sumOf { weight(it).toDouble() }
        if (total <= 0.0) return null
        var roll = Random.nextDouble(total)
        for (item in this) {
            roll -= weight(item)
            if (roll < 0) return item
        }
        return last()
    }

    companion object {

        /**
         * Adds this influence to every new player spawner, fishing cast and placed Poké Snack. Snacks are capped by
         * [SnackSpawnTier].
         */
        fun register() {
            PlayerSpawnerFactory.influenceBuilders.add { player -> TierSpawningInfluence(player.uuid) }
            FishingSpawnerFactory.positionInfluenceBuilders.add { context -> listOf(TierSpawningInfluence(context.player.uuid)) }
            PokeSnackSpawnerFactory.influenceBuilders.add { context ->
                TierSpawningInfluence(SnackSpawnTier(context.world, context.pos)::current)
            }
        }
    }
}
