package com.caraca.customs.balls.customs

import com.caraca.customs.balls.effects.PerfectIVsCaptureEffect
import com.caraca.customs.balls.modules.BallTier
import com.caraca.customs.balls.modules.CustomBallProperties
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes

/**
 * Single source of truth for every custom Poké Ball definition.
 *
 * Balls are grouped by family (regular, regular ultra, ancient, ancient ultra). The declaration order
 * is kept by [ALL] and drives the creative tab order.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CustomBalls {

    private val NORMAL_FIGHTING = setOf(ElementalTypes.NORMAL, ElementalTypes.FIGHTING)

    /**
     * Standard custom Poké Balls.
     */
    val REGULAR: List<CustomBallProperties> by lazy {
        listOf(
            ball("aquamarine_ball", BallTier.REGULAR, ElementalTypes.FLYING, ElementalTypes.ICE),
            ball("azure_ball", BallTier.REGULAR, ElementalTypes.WATER, ElementalTypes.DRAGON),
            ball("cherry_ball", BallTier.REGULAR, ElementalTypes.PSYCHIC),
            ball("cinnamon_ball", BallTier.REGULAR, ElementalTypes.ROCK, ElementalTypes.GROUND),
            ball("citrine_ball", BallTier.REGULAR, ElementalTypes.ELECTRIC),
            ball("lavender_ball", BallTier.REGULAR, ElementalTypes.POISON, ElementalTypes.GHOST),
            ball("lime_ball", BallTier.REGULAR, ElementalTypes.BUG),
            ball("poke_ball", BallTier.REGULAR, NORMAL_FIGHTING),
            ball("premier_ball", BallTier.REGULAR, NORMAL_FIGHTING),
            ball("roseate_ball", BallTier.REGULAR, ElementalTypes.FAIRY),
            ball("saffron_ball", BallTier.REGULAR, ElementalTypes.FIRE),
            ball("slate_ball", BallTier.REGULAR, ElementalTypes.DARK),
            ball("smoked_ball", BallTier.REGULAR, ElementalTypes.STEEL),
            ball("verdant_ball", BallTier.REGULAR, ElementalTypes.GRASS),
            caracaBall("caraca_ball")
        )
    }

    /**
     * Ultra variants of the standard custom Poké Balls.
     */
    val REGULAR_ULTRA: List<CustomBallProperties> by lazy {
        listOf(
            ball("ultra_aquamarine_ball", BallTier.ULTRA, ElementalTypes.FLYING, ElementalTypes.ICE),
            ball("ultra_azure_ball", BallTier.ULTRA, ElementalTypes.WATER, ElementalTypes.DRAGON),
            ball("ultra_cherry_ball", BallTier.ULTRA, ElementalTypes.PSYCHIC),
            ball("ultra_cinnamon_ball", BallTier.ULTRA, ElementalTypes.ROCK, ElementalTypes.GROUND),
            ball("ultra_citrine_ball", BallTier.ULTRA, ElementalTypes.ELECTRIC),
            ball("ultra_lavender_ball", BallTier.ULTRA, ElementalTypes.POISON, ElementalTypes.GHOST),
            ball("ultra_lime_ball", BallTier.ULTRA, ElementalTypes.BUG),
            ball("ultra_poke_ball", BallTier.ULTRA, NORMAL_FIGHTING),
            ball("ultra_premier_ball", BallTier.ULTRA, NORMAL_FIGHTING),
            ball("ultra_roseate_ball", BallTier.ULTRA, ElementalTypes.FAIRY),
            ball("ultra_saffron_ball", BallTier.ULTRA, ElementalTypes.FIRE),
            ball("ultra_slate_ball", BallTier.ULTRA, ElementalTypes.DARK),
            ball("ultra_smoked_ball", BallTier.ULTRA, ElementalTypes.STEEL),
            ball("ultra_verdant_ball", BallTier.ULTRA, ElementalTypes.GRASS)
        )
    }

    /**
     * Hisuian/Ancient custom Poké Balls.
     */
    val ANCIENT: List<CustomBallProperties> by lazy {
        listOf(
            ball("ancient_aquamarine_ball", BallTier.REGULAR, ElementalTypes.FLYING, ElementalTypes.ICE),
            ball("ancient_azure_ball", BallTier.REGULAR, ElementalTypes.WATER, ElementalTypes.DRAGON),
            ball("ancient_cherry_ball", BallTier.REGULAR, ElementalTypes.PSYCHIC),
            ball("ancient_cinnamon_ball", BallTier.REGULAR, ElementalTypes.ROCK, ElementalTypes.GROUND),
            ball("ancient_citrine_ball", BallTier.REGULAR, ElementalTypes.ELECTRIC),
            ball("ancient_lavender_ball", BallTier.REGULAR, ElementalTypes.POISON, ElementalTypes.GHOST),
            ball("ancient_lime_ball", BallTier.REGULAR, ElementalTypes.BUG),
            ball("ancient_poke_ball", BallTier.REGULAR, NORMAL_FIGHTING),
            ball("ancient_ivory_ball", BallTier.REGULAR, NORMAL_FIGHTING),
            ball("ancient_roseate_ball", BallTier.REGULAR, ElementalTypes.FAIRY),
            ball("ancient_saffron_ball", BallTier.REGULAR, ElementalTypes.FIRE),
            ball("ancient_slate_ball", BallTier.REGULAR, ElementalTypes.DARK),
            ball("ancient_smoked_ball", BallTier.REGULAR, ElementalTypes.STEEL),
            ball("ancient_verdant_ball", BallTier.REGULAR, ElementalTypes.GRASS),
            caracaBall("ancient_caraca_ball")
        )
    }

    /**
     * Ultra variants of the Hisuian/Ancient custom Poké Balls.
     */
    val ANCIENT_ULTRA: List<CustomBallProperties> by lazy {
        listOf(
            ball("ancient_ultra_aquamarine_ball", BallTier.ULTRA, ElementalTypes.FLYING, ElementalTypes.ICE),
            ball("ancient_ultra_azure_ball", BallTier.ULTRA, ElementalTypes.WATER, ElementalTypes.DRAGON),
            ball("ancient_ultra_cherry_ball", BallTier.ULTRA, ElementalTypes.PSYCHIC),
            ball("ancient_ultra_cinnamon_ball", BallTier.ULTRA, ElementalTypes.ROCK, ElementalTypes.GROUND),
            ball("ancient_ultra_citrine_ball", BallTier.ULTRA, ElementalTypes.ELECTRIC),
            ball("ancient_ultra_lavender_ball", BallTier.ULTRA, ElementalTypes.POISON, ElementalTypes.GHOST),
            ball("ancient_ultra_lime_ball", BallTier.ULTRA, ElementalTypes.BUG),
            ball("ancient_ultra_poke_ball", BallTier.ULTRA, NORMAL_FIGHTING),
            ball("ancient_ultra_ivory_ball", BallTier.ULTRA, NORMAL_FIGHTING),
            ball("ancient_ultra_roseate_ball", BallTier.ULTRA, ElementalTypes.FAIRY),
            ball("ancient_ultra_saffron_ball", BallTier.ULTRA, ElementalTypes.FIRE),
            ball("ancient_ultra_slate_ball", BallTier.ULTRA, ElementalTypes.DARK),
            ball("ancient_ultra_smoked_ball", BallTier.ULTRA, ElementalTypes.STEEL),
            ball("ancient_ultra_verdant_ball", BallTier.ULTRA, ElementalTypes.GRASS)
        )
    }

    /**
     * Every custom Poké Ball definition, in registration order.
     */
    val ALL: List<CustomBallProperties> by lazy {
        REGULAR + REGULAR_ULTRA + ANCIENT + ANCIENT_ULTRA
    }

    private fun ball(nameId: String, tier: BallTier, vararg types: ElementalType): CustomBallProperties =
        ball(nameId, tier, types.toSet())

    private fun ball(nameId: String, tier: BallTier, types: Set<ElementalType>): CustomBallProperties =
        CustomBallProperties(nameId = nameId, tier = tier, elementalTypes = types)

    private fun caracaBall(nameId: String): CustomBallProperties =
        CustomBallProperties(
            nameId = nameId,
            tier = BallTier.CARACA,
            elementalTypes = ElementalTypes.all().toSet(),
            extraCaptureEffects = listOf(PerfectIVsCaptureEffect())
        )
}
