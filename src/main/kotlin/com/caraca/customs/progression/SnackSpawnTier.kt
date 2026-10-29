package com.caraca.customs.progression

import com.caraca.customs.config.Feature
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player

/**
 * Tier a placed Poké Snack spawns for, used to apply the tier spawn cap to its spawns.
 *
 * - While players share tiers, it's the world's tier, wherever the players are.
 * - Otherwise, it's the highest tier among the players up to [RANGE] blocks (one chunk) away from the snack. When no
 *   player is that close, it's the tier of the nearest player in the snack's dimension, and tier 1 when there's none.
 *
 * Spectators are ignored. The tier is worked out once per game tick, since Cobblemon asks for it for every spawn detail
 * of a spawn attempt.
 *
 * @param level Dimension the snack is placed in.
 * @param pos Position of the snack.
 *
 * @author hadouz01
 * @since 1.15.2
 */
class SnackSpawnTier(private val level: ServerLevel, private val pos: BlockPos) {

    companion object {
        private const val RANGE = 16.0
    }

    private var cachedAt = Long.MIN_VALUE
    private var cachedTier = 1

    /**
     * Gets the tier the snack spawns for right now.
     *
     * @return The tier, from 1 to the tier count.
     */
    fun current(): Int {
        val time = level.gameTime
        if (time != cachedAt) {
            cachedTier = resolve()
            cachedAt = time
        }
        return cachedTier
    }

    private fun resolve(): Int {
        if (Feature.SHARED_TIERING.isEnabled) return PlayerTiers.tierFor(SharedPokedex.sharedCount())

        val center = pos.center
        val players = level.players().filterNot(Player::isSpectator)
        val nearby = players.filter { it.distanceToSqr(center) <= RANGE * RANGE }
        if (nearby.isNotEmpty()) return nearby.maxOf { PlayerTiers.cachedTier(it.uuid) }

        val nearest = players.minByOrNull { it.distanceToSqr(center) } ?: return 1
        return PlayerTiers.cachedTier(nearest.uuid)
    }
}
