package com.caraca.customs.balls.customs.regular

import com.caraca.customs.balls.effects.PerfectIVsCaptureEffect
import com.caraca.customs.balls.modules.CustomBallProperties
import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.api.pokeball.catching.CaptureEffect
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes

/**
 * Registry container holding static declarations for all custom regular Ultra Poké Ball properties.
 *
 * Serves as the single source of truth for regular Ultra Poké Ball definitions within the mod.
 * Separates definition declarations from system registration mechanics performed by [CustomBallRegistry].
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CustomRegularUltraBalls {

    /**
     * Immutable collection of all custom Poké Ball definitions dynamically configured from [DynamicConfig].
     */
    val ALL: List<CustomBallProperties> by lazy {
        listOf(
            createBall("ultra_aquamarine_ball", setOf(ElementalTypes.FLYING, ElementalTypes.ICE)),
            createBall("ultra_azure_ball", setOf(ElementalTypes.WATER, ElementalTypes.DRAGON)),
            createBall("ultra_cherry_ball", setOf(ElementalTypes.PSYCHIC)),
            createBall("ultra_cinnamon_ball", setOf(ElementalTypes.ROCK, ElementalTypes.GROUND)),
            createBall("ultra_citrine_ball", setOf(ElementalTypes.ELECTRIC)),
            createBall("ultra_lavender_ball", setOf(ElementalTypes.POISON, ElementalTypes.GHOST)),
            createBall("ultra_lime_ball", setOf(ElementalTypes.BUG)),
            createBall("ultra_poke_ball", setOf(ElementalTypes.NORMAL, ElementalTypes.FIGHTING)),
            createBall("ultra_premier_ball", setOf(ElementalTypes.NORMAL, ElementalTypes.FIGHTING)),
            createBall("ultra_roseate_ball", setOf(ElementalTypes.FAIRY)),
            createBall("ultra_saffron_ball", setOf(ElementalTypes.FIRE)),
            createBall("ultra_slate_ball", setOf(ElementalTypes.DARK)),
            createBall("ultra_smoked_ball", setOf(ElementalTypes.STEEL)),
            createBall("ultra_verdant_ball", setOf(ElementalTypes.GRASS))
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