package com.caraca.customs.balls.customs.ancient

import com.caraca.customs.balls.effects.PerfectIVsCaptureEffect
import com.caraca.customs.balls.modules.CustomBallProperties
import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.api.pokeball.catching.CaptureEffect
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes

/**
 * Registry container holding static declarations for all custom ancient Ultra Poké Ball properties.
 *
 * Serves as the single source of truth for ancient Ultra Poké Ball definitions within the mod.
 * Separates definition declarations from system registration mechanics performed by [CustomBallRegistry].
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CustomAncientUltraBalls {


    /**
     * Immutable collection of all custom Poké Ball definitions dynamically configured from [DynamicConfig].
     */
    val ALL: List<CustomBallProperties> by lazy {
        listOf(
            createBall("ancient_ultra_aquamarine_ball", setOf(ElementalTypes.FLYING, ElementalTypes.ICE)),
            createBall("ancient_ultra_azure_ball", setOf(ElementalTypes.WATER, ElementalTypes.DRAGON)),
            createBall("ancient_ultra_cherry_ball", setOf(ElementalTypes.PSYCHIC)),
            createBall("ancient_ultra_cinnamon_ball", setOf(ElementalTypes.ROCK, ElementalTypes.GROUND)),
            createBall("ancient_ultra_citrine_ball", setOf(ElementalTypes.ELECTRIC)),
            createBall("ancient_ultra_lavender_ball", setOf(ElementalTypes.POISON, ElementalTypes.GHOST)),
            createBall("ancient_ultra_lime_ball", setOf(ElementalTypes.BUG)),
            createBall("ancient_ultra_poke_ball", setOf(ElementalTypes.NORMAL, ElementalTypes.FIGHTING)),
            createBall("ancient_ultra_ivory_ball", setOf(ElementalTypes.NORMAL, ElementalTypes.FIGHTING)),
            createBall("ancient_ultra_roseate_ball", setOf(ElementalTypes.FAIRY)),
            createBall("ancient_ultra_saffron_ball", setOf(ElementalTypes.FIRE)),
            createBall("ancient_ultra_slate_ball", setOf(ElementalTypes.DARK)),
            createBall("ancient_ultra_smoked_ball", setOf(ElementalTypes.STEEL)),
            createBall("ancient_ultra_verdant_ball", setOf(ElementalTypes.GRASS))
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