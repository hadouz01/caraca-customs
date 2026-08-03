package com.caraca.customs.balls.modules

import com.cobblemon.mod.common.api.pokeball.catching.CaptureEffect
import com.cobblemon.mod.common.api.types.ElementalType

/**
 * Data container representing configuration parameters required to instantiate a [ModularPokeBall].
 *
 * Acts as a clean configuration contract for custom Poké Balls, specifying identifier names,
 * targeted elemental affiliations, catch rate multipliers, 3D model references, and optional capture effects.
 *
 * @property nameId The snake_case string identifier for the Poké Ball (e.g., "fire_ball", "caraca_ball").
 * @property elementalTypes The set of [ElementalType] targeted by this Poké Ball's catch logic (defaults to empty set).
 * @property reducedCatchRate The lowered base catch rate multiplier applied during standard calculation (defaults to 1.0f).
 * @property defaultCatchRate The default base catch rate multiplier applied during standard calculation (defaults to 1.0f).
 * @property boostedCatchRate The boosted catch rate multiplier applied when specific type/status criteria are met (defaults to 1.0f).
 * @property model3dPath The relative path name pointing to the 3D model JSON file (defaults to [nameId]).
 * @property extraCaptureEffects List of optional [CaptureEffect] handlers executed upon successful Pokémon capture.
 *
 * @author hadouz01
 * @since 1.0.0
 */
data class CustomBallProperties(
    val nameId: String = "null_value",
    val elementalTypes: Set<ElementalType> = emptySet(),
    val reducedCatchRate: Float = 1.0f,
    val defaultCatchRate: Float = 1.0f,
    val boostedCatchRate: Float = 1.0f,
    val model3dPath: String = nameId,
    val extraCaptureEffects: List<CaptureEffect> = emptyList()
)