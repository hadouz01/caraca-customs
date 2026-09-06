package com.caraca.customs.balls.effects

import com.cobblemon.mod.common.api.pokeball.catching.CaptureEffect
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.world.entity.LivingEntity

/**
 * Capture effect that maximizes all Individual Values (IVs) of a captured Pokémon to 31.
 *
 * Iterates through Cobblemon's permanent stats ([Stats.PERMANENT]) and sets each corresponding IV value to 31.
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
         * Maximum achievable Individual Value (IV) for any stat slot.
         */
        private const val PERFECT_IV_VALUE: Int = 31
    }
}
