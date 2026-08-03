package com.caraca.customs.balls.effects

import com.cobblemon.mod.common.api.pokeball.catching.CaptureEffect
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.world.entity.LivingEntity

/**
 * Capture effect that maximizes all Individual Values (IVs) of a captured Pokémon to 31.
 *
 * Implements [CaptureEffect] to execute immediately upon successful capture processing.
 * Iterates through Cobblemon's permanent stats ([Stats.PERMANENT]) and sets each corresponding IV value to 31.
 *
 * @author hadouz01
 * @since 1.0.0
 */
class PerfectIVsCaptureEffect : CaptureEffect {

    companion object {
        /**
         * Constant representing the maximum achievable Individual Value (IV) for any stat slot.
         */
        private const val PERFECT_IV_VALUE: Int = 31
    }

    /**
     * Executes the IV maximization logic on the target [Pokemon] during capture resolution.
     *
     * @param livingEntity The throwing entity performing the capture attempt.
     * @param pokemon The target [Pokemon] instance undergoing capture processing.
     */
    override fun apply(livingEntity: LivingEntity, pokemon: Pokemon) {
        maximizeIVs(pokemon)
    }

    /**
     * Iterates through all permanent stats registered in Cobblemon and sets each IV slot to [PERFECT_IV_VALUE].
     *
     * Delegates iterative logic into a dedicated private function to maintain a minimal Cognitive Complexity rating.
     *
     * @param pokemon Target [Pokemon] whose IV container is updated.
     */
    private fun maximizeIVs(pokemon: Pokemon) {
        val pokemonIvs = pokemon.ivs

        Stats.PERMANENT.forEach { stat ->
            pokemonIvs[stat] = PERFECT_IV_VALUE
        }
    }
}