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
 * Catch rate modifier for Poké Balls operating on target elemental type alignments.
 *
 * Uses the boosted multiplier when the target shares one of [types] or suffers a status tied to them
 * (Burn for Fire, Paralysis for Electric, Poison for Poison, Freeze for Ice), doubling it while asleep.
 * Multipliers are resolved from [DynamicConfig] on every call so configuration changes apply immediately.
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

    override fun value(thrower: LivingEntity, pokemon: Pokemon): Float =
        resolveMultiplier(pokemon.types, pokemon.status?.status)

    override fun behavior(thrower: LivingEntity, pokemon: Pokemon): CatchRateModifier.Behavior =
        CatchRateModifier.Behavior.MULTIPLY

    override fun isValid(thrower: LivingEntity, pokemon: Pokemon): Boolean = true

    override fun modifyCatchRate(currentCatchRate: Float, thrower: LivingEntity, pokemon: Pokemon): Float =
        behavior(thrower, pokemon).mutator.invoke(currentCatchRate, value(thrower, pokemon))

    /**
     * Checks whether the boosted multiplier applies to a target.
     *
     * @param targetTypes Elemental types of the target Pokémon.
     * @param status Active persistent status of the target, if any.
     * @return `true` if a type or a type-bound status matches.
     */
    fun isConditionMet(targetTypes: Iterable<ElementalType>, status: PersistentStatus?): Boolean =
        targetTypes.any { it in types } || verifyStatus(status)

    /**
     * Resolves the multiplier for a target using the live configuration.
     *
     * @param targetTypes Elemental types of the target Pokémon.
     * @param status Active persistent status of the target, if any.
     * @return The default multiplier, or the boosted one (doubled while asleep) when [isConditionMet].
     */
    fun resolveMultiplier(targetTypes: Iterable<ElementalType>, status: PersistentStatus?): Float {
        val config = DynamicConfig.getPokeBallConfigFor(ballNameId)
        if (!isConditionMet(targetTypes, status)) {
            return config.defaultCaptureRate
        }
        return if (status == Statuses.SLEEP) config.boostedCaptureRate * 2.0f else config.boostedCaptureRate
    }

    /**
     * Verifies if a persistent status aligns with target types.
     *
     * @param pokemonStatus Target active status.
     * @return `true` if matched; `false` otherwise.
     */
    fun verifyStatus(pokemonStatus: PersistentStatus?): Boolean {
        if (pokemonStatus == null) return false

        return (ElementalTypes.FIRE in types && pokemonStatus == Statuses.BURN) ||
                (ElementalTypes.ELECTRIC in types && pokemonStatus == Statuses.PARALYSIS) ||
                (ElementalTypes.POISON in types && (pokemonStatus == Statuses.POISON || pokemonStatus == Statuses.POISON_BADLY)) ||
                (ElementalTypes.ICE in types && pokemonStatus == Statuses.FROZEN)
    }
}
