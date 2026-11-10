package com.caraca.customs.balls.modifiers

import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.api.pokeball.catching.CatchRateModifier
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.world.entity.LivingEntity

/**
 * Catch rate modifier of the balls without elemental types. The multiplier is read from [DynamicConfig] on every call,
 * so config changes apply right away.
 *
 * @property ballNameId Path of the ball's identifier.
 *
 * @author hadouz01
 * @since 1.0.0
 */
class DynamicCatchRateModifier(
    val ballNameId: String
) : CatchRateModifier {

    override fun value(thrower: LivingEntity, pokemon: Pokemon): Float = resolveMultiplier()

    override fun behavior(thrower: LivingEntity, pokemon: Pokemon): CatchRateModifier.Behavior =
        CatchRateModifier.Behavior.MULTIPLY

    override fun isValid(thrower: LivingEntity, pokemon: Pokemon): Boolean = true

    override fun modifyCatchRate(currentCatchRate: Float, thrower: LivingEntity, pokemon: Pokemon): Float =
        behavior(thrower, pokemon).mutator.invoke(currentCatchRate, value(thrower, pokemon))

    /**
     * Resolves the configured default multiplier for this ball.
     *
     * @return Current configured base capture multiplier.
     */
    fun resolveMultiplier(): Float = DynamicConfig.getPokeBallConfigFor(ballNameId).defaultCaptureRate
}
