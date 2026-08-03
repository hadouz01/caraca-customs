package com.caraca.customs.balls.modifiers

import com.cobblemon.mod.common.api.pokeball.catching.CatchRateModifier
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.world.entity.LivingEntity

/**
 * Standard catch rate modifier providing a fixed, static catch multiplier without type checks or status conditions.
 *
 * Used as a default modifier for custom Poké Balls that do not depend on [com.cobblemon.mod.common.api.types.ElementalType].
 *
 * @property multiplier The constant multiplier value applied during capture attempts.
 * @author hadouz01
 * @since 1.0.0
 */
class SimpleCatchRateModifier(
    val multiplier: Float = 1.0f
) : CatchRateModifier {

    /**
     * Evaluates and returns the constant catch rate multiplier.
     *
     * @param livingEntity The throwing entity attempting the capture.
     * @param pokemon The target [Pokemon] instance.
     * @return The configured [multiplier].
     */
    override fun value(livingEntity: LivingEntity, pokemon: Pokemon): Float = multiplier

    /**
     * Dictates multiplicative behavior in Cobblemon's catch formula pipeline.
     */
    override fun behavior(livingEntity: LivingEntity, pokemon: Pokemon): CatchRateModifier.Behavior {
        return CatchRateModifier.Behavior.MULTIPLY
    }

    /**
     * Validates applicability during capture calculation.
     */
    override fun isValid(livingEntity: LivingEntity, pokemon: Pokemon): Boolean = true

    /**
     * Mutates and applies the static value onto the current catch rate multiplier chain.
     */
    override fun modifyCatchRate(currentRate: Float, livingEntity: LivingEntity, pokemon: Pokemon): Float {
        return behavior(livingEntity, pokemon).mutator.invoke(currentRate, value(livingEntity, pokemon))
    }
}