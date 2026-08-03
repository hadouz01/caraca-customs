package com.caraca.customs.balls.modifiers

import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.api.pokeball.catching.CatchRateModifier
import com.cobblemon.mod.common.api.pokemon.status.Statuses
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.status.PersistentStatus
import net.minecraft.world.entity.LivingEntity

/**
 * High-performance catch rate modifier for Poké Balls operating on target elemental type alignments.
 *
 * Dynamically resolves multipliers from [DynamicConfig] at runtime to ensure real-time config changes.
 *
 * @property ballNameId Identifier path of the host Poké Ball.
 * @property types Set of target [ElementalType] instances governing boosted multiplier calculations.
 *
 * @author hadouz01
 * @since 1.0.0
 */
class ElementalBallCatchRateModifier(
    val ballNameId: String,
    val types: Set<ElementalType>
) : CatchRateModifier {

    /**
     * Evaluates the effective catch multiplier by querying current dynamic configuration.
     *
     * @param livingEntity The throwing entity attempting the capture.
     * @param pokemon The target [Pokemon] instance being evaluated.
     * @return Dynamic multiplier based on target status, elemental alignments, and real-time settings.
     */
    override fun value(livingEntity: LivingEntity, pokemon: Pokemon): Float {
        val config = DynamicConfig.getPokeBallConfigFor(ballNameId)
        val defaultMultiplier = config.defaultCaptureRate
        val boostedMultiplier = config.boostedCaptureRate

        val matchesType = types.contains(pokemon.primaryType) ||
                (pokemon.secondaryType != null && types.contains(pokemon.secondaryType))

        val activeStatus = pokemon.status?.status
        val matchesStatus = verifyStatus(activeStatus)

        if (!matchesType && !matchesStatus) {
            return defaultMultiplier
        }

        val isSleeping = activeStatus == Statuses.SLEEP
        return if (isSleeping) boostedMultiplier * 2.0f else boostedMultiplier
    }

    override fun behavior(livingEntity: LivingEntity, pokemon: Pokemon): CatchRateModifier.Behavior {
        return CatchRateModifier.Behavior.MULTIPLY
    }

    override fun isValid(livingEntity: LivingEntity, pokemon: Pokemon): Boolean = true

    override fun modifyCatchRate(currentRate: Float, livingEntity: LivingEntity, pokemon: Pokemon): Float {
        return behavior(livingEntity, pokemon).mutator.invoke(currentRate, value(livingEntity, pokemon))
    }

    /**
     * Verifies if a persistent status aligns with target types.
     *
     * @param pokemonStatus Target active status.
     * @return `true` if matched; `false` otherwise.
     */
    fun verifyStatus(pokemonStatus: PersistentStatus?): Boolean {
        if (pokemonStatus == null) return false

        if (types.contains(ElementalTypes.FIRE) && pokemonStatus == Statuses.BURN) return true
        if (types.contains(ElementalTypes.ELECTRIC) && pokemonStatus == Statuses.PARALYSIS) return true
        if (types.contains(ElementalTypes.POISON) && (pokemonStatus == Statuses.POISON || pokemonStatus == Statuses.POISON_BADLY)) return true
        if (types.contains(ElementalTypes.ICE) && pokemonStatus == Statuses.FROZEN) return true

        return false
    }
}