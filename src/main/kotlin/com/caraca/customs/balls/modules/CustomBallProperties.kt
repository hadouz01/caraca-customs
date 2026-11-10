package com.caraca.customs.balls.modules

import com.cobblemon.mod.common.api.pokeball.catching.CaptureEffect
import com.cobblemon.mod.common.api.types.ElementalType

/**
 * Everything needed to build a [ModularPokeBall]. Catch rates aren't here, they're always read live from
 * [com.caraca.customs.config.DynamicConfig] by [nameId] and [tier].
 *
 * @property nameId Snake case id of the ball, like `fire_ball`.
 * @property tier Tier of the ball, which sets its default catch rates and the chests it shows up in.
 * @property elementalTypes Types the ball is good against, none by default.
 * @property model3dPath Name of the 3D model file, [nameId] by default.
 * @property extraCaptureEffects Extra effects run when a Pokémon is caught with it.
 *
 * @author hadouz01
 * @since 1.0.0
 */
data class CustomBallProperties(
    val nameId: String,
    val tier: BallTier,
    val elementalTypes: Set<ElementalType> = emptySet(),
    val model3dPath: String = nameId,
    val extraCaptureEffects: List<CaptureEffect> = emptyList()
)

/**
 * Category of a custom Poké Ball, selecting its default catch rate configuration and loot pool.
 *
 * @author hadouz01
 * @since 1.0.0
 */
enum class BallTier {
    REGULAR,
    ULTRA,
    CARACA
}
