package com.caraca.customs.balls.modifiers

import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.api.pokeball.catching.CatchRateModifier
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.world.entity.LivingEntity

/**
 * Dynamic catch rate modifier reading base rate directly from memory configuration at runtime.
 *
 * Ensures real-time configuration changes via Cloth Config GUI apply immediately without restart.
 *
 * @property ballNameId Identifier path of the host Poké Ball.
 *
 * @author hadouz01
 * @since 1.0.0
 */
class DynamicCatchRateModifier(
    val ballNameId: String
) : CatchRateModifier {

    /**
     * Evaluates effective multiplier reading directly from dynamic config memory state.
     *
     * @param livingEntity Target throwing entity.
     * @param pokemon Target wild Pokémon entity.
     * @return Current configured base capture multiplier.
     */
    override fun value(livingEntity: LivingEntity, pokemon: Pokemon): Float {
        return DynamicConfig.getPokeBallConfigFor(ballNameId).defaultCaptureRate
    }

    override fun behavior(livingEntity: LivingEntity, pokemon: Pokemon): CatchRateModifier.Behavior {
        return CatchRateModifier.Behavior.MULTIPLY
    }

    override fun isValid(livingEntity: LivingEntity, pokemon: Pokemon): Boolean = true

    override fun modifyCatchRate(currentRate: Float, livingEntity: LivingEntity, pokemon: Pokemon): Float {
        return behavior(livingEntity, pokemon).mutator.invoke(currentRate, value(livingEntity, pokemon))
    }
}