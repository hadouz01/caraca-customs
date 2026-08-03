package com.caraca.customs.balls.customs.ancient

import com.caraca.customs.balls.effects.PerfectIVsCaptureEffect
import com.caraca.customs.balls.modules.CustomBallProperties
import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.api.pokeball.catching.CaptureEffect
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes

/**
 * Registry container holding static declarations for all custom ancient Poké Ball properties.
 *
 * Serves as the single source of truth for ancient Poké Ball definitions within the mod.
 * Separates definition declarations from system registration mechanics performed by [CustomBallRegistry].
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CustomAncientBalls {

    /**
     * Immutable collection of all custom Poké Ball definitions dynamically configured from [DynamicConfig].
     */
    val ALL: List<CustomBallProperties> by lazy {
        listOf(
            createBall("ancient_aquamarine_ball", setOf(ElementalTypes.FLYING, ElementalTypes.ICE)),
            createBall("ancient_azure_ball", setOf(ElementalTypes.WATER, ElementalTypes.DRAGON)),
            createBall("ancient_cherry_ball", setOf(ElementalTypes.PSYCHIC)),
            createBall("ancient_cinnamon_ball", setOf(ElementalTypes.ROCK, ElementalTypes.GROUND)),
            createBall("ancient_citrine_ball", setOf(ElementalTypes.ELECTRIC)),
            createBall("ancient_lavender_ball", setOf(ElementalTypes.POISON, ElementalTypes.GHOST)),
            createBall("ancient_lime_ball", setOf(ElementalTypes.BUG)),
            createBall("ancient_poke_ball", setOf(ElementalTypes.NORMAL, ElementalTypes.FIGHTING)),
            createBall("ancient_ivory_ball", setOf(ElementalTypes.NORMAL, ElementalTypes.FIGHTING)),
            createBall("ancient_roseate_ball", setOf(ElementalTypes.FAIRY)),
            createBall("ancient_saffron_ball", setOf(ElementalTypes.FIRE)),
            createBall("ancient_slate_ball", setOf(ElementalTypes.DARK)),
            createBall("ancient_smoked_ball", setOf(ElementalTypes.STEEL)),
            createBall("ancient_verdant_ball", setOf(ElementalTypes.GRASS)),
            createBall(
                nameId = "ancient_caraca_ball",
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