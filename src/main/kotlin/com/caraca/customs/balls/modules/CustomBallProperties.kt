package com.caraca.customs.balls.modules

import com.cobblemon.mod.common.api.pokeball.catching.CaptureEffect
import com.cobblemon.mod.common.api.types.ElementalType

/**
 * Data container representing configuration parameters required to instantiate a [ModularPokeBall].
 *
 * Catch rate multipliers are not stored here: they are always read live from
 * [com.caraca.customs.config.DynamicConfig] using [nameId] and [tier].
 *
 * @property nameId The snake_case string identifier for the Poké Ball (e.g., "fire_ball", "caraca_ball").
 * @property tier The [BallTier] driving default catch rates and loot table placement.
 * @property elementalTypes The set of [ElementalType] targeted by this Poké Ball's catch logic (defaults to empty set).
 * @property model3dPath The relative path name pointing to the 3D model JSON file (defaults to [nameId]).
 * @property extraCaptureEffects List of optional [CaptureEffect] handlers executed upon successful Pokémon capture.
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
