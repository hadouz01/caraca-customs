package com.caraca.customs.balls.effects

import com.cobblemon.mod.common.api.pokeball.catching.CaptureEffect
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.world.entity.LivingEntity

/**
 * Capture effect that maximizes all Individual Values (IVs) of a captured Pokémon to 31.
 *
 * Sets every permanent stat's IV ([Stats.PERMANENT]) to 31.
 *
 * @author hadouz01
 * @since 1.0.0
 */
class PerfectIVsCaptureEffect : CaptureEffect {

    override fun apply(thrower: LivingEntity, pokemon: Pokemon) {
        Stats.PERMANENT.forEach { stat ->
            pokemon.ivs[stat] = PERFECT_IV_VALUE
        }
    }

    companion object {
        /**
         * Highest IV a stat can have.
         */
        private const val PERFECT_IV_VALUE: Int = 31
    }
}
