package com.caraca.customs.client.compat

import com.caraca.customs.balls.CustomBallRegistry
import com.caraca.customs.balls.modifiers.DynamicCatchRateModifier
import com.caraca.customs.balls.modifiers.ElementalBallCatchRateModifier
import com.caraca.customs.balls.modules.ModularPokeBall
import com.catchrate.BallMultiplierCalculator.BallResult
import com.catchrate.BallTranslations
import com.catchrate.api.BallApiResult
import com.catchrate.api.CatchRateBallRegistry
import com.cobblemon.mod.common.api.pokemon.status.Statuses
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.pokemon.status.PersistentStatus
import com.cobblemon.mod.common.util.cobblemonResource

/**
 * catchrate-display integration. Every custom ball is registered through its [CatchRateBallRegistry], so they show up in
 * the comparison panel with their own names and the HUD uses the balls' real catch logic.
 *
 * Only load this when catchrate-display is installed.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CatchRateDisplayCompat {

    /**
     * Registers a [CatchRateBallRegistry] provider for every custom Poké Ball.
     */
    fun register() {
        CustomBallRegistry.getRegisteredBalls().forEach { ball ->
            CatchRateBallRegistry.registerBall(ball.id) { ctx ->
                evaluate(ball, ctx.primaryType, ctx.secondaryType, ctx.statusPath)
            }
        }
    }

    /**
     * Evaluates a bare ball id (path without namespace) as forwarded by catchrate-display's held-ball HUD.
     *
     * @param ballId Ball path, like `poke_ball`.
     * @param primaryType Primary type name of the target.
     * @param secondaryType Secondary type name of the target, if any.
     * @param statusPath Persistent status path of the target, if any.
     * @return The ball result, or `null` if the id does not match a custom Poké Ball.
     */
    fun evaluateBareId(ballId: String, primaryType: String, secondaryType: String?, statusPath: String?): BallResult? {
        val ball = CustomBallRegistry.getBallByName(ballId) ?: return null
        val result = evaluate(ball, primaryType, secondaryType, statusPath) ?: return null
        return BallResult(result.multiplier, result.conditionMet, result.reason, result.isGuaranteed)
    }

    private fun evaluate(
        ball: ModularPokeBall,
        primaryType: String,
        secondaryType: String?,
        statusPath: String?
    ): BallApiResult? {
        return when (val modifier = ball.modifier) {
            is ElementalBallCatchRateModifier -> {
                val targetTypes = listOfNotNull(primaryType, secondaryType).mapNotNull(ElementalTypes::get)
                val status = statusPath?.let { Statuses.getStatus(cobblemonResource(it)) as? PersistentStatus }
                BallApiResult(
                    multiplier = modifier.resolveMultiplier(targetTypes, status),
                    conditionMet = modifier.isConditionMet(targetTypes, status),
                    reason = BallTranslations.multiplierAlways()
                )
            }

            is DynamicCatchRateModifier -> BallApiResult(
                multiplier = modifier.resolveMultiplier(),
                conditionMet = true,
                reason = BallTranslations.multiplierAlways()
            )

            else -> null
        }
    }
}
