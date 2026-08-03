package com.caraca.customs.balls.customs.regular

import com.caraca.customs.balls.effects.PerfectIVsCaptureEffect
import com.caraca.customs.balls.modules.CustomBallProperties
import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.api.pokeball.catching.CaptureEffect
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes

/**
 * Registry container holding dynamic declarations for all custom regular Poké Ball properties.
 *
 * Integrates catch rates dynamically resolved from [DynamicConfig] to ensure runtime modification
 * support via configuration files while keeping initialization lightweight.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CustomRegularBalls {

    /**
     * Immutable collection of all custom Poké Ball definitions dynamically configured from [DynamicConfig].
     */
    val ALL: List<CustomBallProperties> by lazy {
        listOf(
            createBall("aquamarine_ball", setOf(ElementalTypes.FLYING, ElementalTypes.ICE)),
            createBall("azure_ball", setOf(ElementalTypes.WATER, ElementalTypes.DRAGON)),
            createBall("cherry_ball", setOf(ElementalTypes.PSYCHIC)),
            createBall("cinnamon_ball", setOf(ElementalTypes.ROCK, ElementalTypes.GROUND)),
            createBall("citrine_ball", setOf(ElementalTypes.ELECTRIC)),
            createBall("lavender_ball", setOf(ElementalTypes.POISON, ElementalTypes.GHOST)),
            createBall("lime_ball", setOf(ElementalTypes.BUG)),
            createBall("poke_ball", setOf(ElementalTypes.NORMAL, ElementalTypes.FIGHTING)),
            createBall("premier_ball", setOf(ElementalTypes.NORMAL, ElementalTypes.FIGHTING)),
            createBall("roseate_ball", setOf(ElementalTypes.FAIRY)),
            createBall("saffron_ball", setOf(ElementalTypes.FIRE)),
            createBall("slate_ball", setOf(ElementalTypes.DARK)),
            createBall("smoked_ball", setOf(ElementalTypes.STEEL)),
            createBall("verdant_ball", setOf(ElementalTypes.GRASS)),
            createBall(
                nameId = "caraca_ball",
                types = ElementalTypes.all().toSet(),
                extraCaptureEffects = listOf(PerfectIVsCaptureEffect())
            )
        )
    }

    /**
     * Factory helper to construct a [CustomBallProperties] instance fetching capture rate parameters
     * dynamically from memory configuration.
     *
     * @param nameId The unique path identifier string for the Poké Ball.
     * @param types Target set of elemental types associated with this Poké Ball.
     * @param extraCaptureEffects Optional list of capture effect handlers executed upon successful capture.
     * @return Fully initialized [CustomBallProperties] populated with configured multipliers.
     */
    private fun createBall(
        nameId: String,
        types: Set<ElementalType>,
        extraCaptureEffects: List<CaptureEffect> = emptyList()
    ): CustomBallProperties {
        val config = DynamicConfig.getPokeBallConfigFor(nameId)

        return CustomBallProperties(
            nameId = nameId,
            elementalTypes = types,
            defaultCatchRate = config.defaultCaptureRate,
            boostedCatchRate = config.boostedCaptureRate,
            extraCaptureEffects = extraCaptureEffects
        )
    }
}