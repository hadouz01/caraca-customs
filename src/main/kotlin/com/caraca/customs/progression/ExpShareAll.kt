package com.caraca.customs.progression

import com.caraca.customs.config.DynamicConfig
import com.caraca.customs.config.Feature
import com.caraca.customs.constants.ExpDivisionMethod
import com.caraca.customs.constants.ExpShareMethod
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.pokemon.experience.ExperienceCalculator
import com.cobblemon.mod.common.api.pokemon.stats.EvCalculator
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.tags.CobblemonItemTags
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.pokemon.Pokemon
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import kotlin.math.roundToInt

/**
 * Exp. Share All: every player-owned party member earns experience when one of them defeats an opponent.
 *
 * The active Pokémon's experience, without its held item boost, is split (equally, or half for the active Pokémon) or
 * copied with a multiplier. Each member's own Lucky Egg or Exp. Share only boosts its own slice, and benched members get
 * no EVs unless they really hold an Exp. Share.
 *
 * `ExpShareAllMixin` lets every member reach the calculator, and every slice still goes through Cobblemon's experience
 * event, so the level cap and Cobblemon's battle rules still apply.
 *
 * @author hadouz01
 * @since 1.11.0
 */
object ExpShareAll {

    private const val BALANCED_ACTIVE_SHARE = 0.5

    /**
     * Wraps Cobblemon's experience and EV calculators once the server starts, keeping whatever calculators are already
     * there.
     */
    fun register() {
        ServerLifecycleEvents.SERVER_STARTING.register {
            val currentExperience = Cobblemon.experienceCalculator
            if (currentExperience !is ExpShareAllCalculator) {
                Cobblemon.experienceCalculator = ExpShareAllCalculator(currentExperience)
            }

            val currentEvs = Cobblemon.evYieldCalculator
            if (currentEvs !is ExpShareAllEvCalculator) {
                Cobblemon.evYieldCalculator = ExpShareAllEvCalculator(currentEvs)
            }
        }
    }

    /**
     * Whether a Pokémon should be treated as an Exp. Share holder, used by `ExpShareAllMixin`.
     *
     * @param pokemon Party member being rewarded.
     * @return Whether Exp. Share All is enabled and the Pokémon belongs to a player.
     */
    @JvmStatic
    fun appliesTo(pokemon: Pokemon): Boolean = Feature.EXP_SHARE_ALL.isEnabled && pokemon.isPlayerOwned()

    /**
     * Experience multiplier given by a Pokémon's own Lucky Egg.
     *
     * @param pokemon Pokémon holding the item.
     * @return Cobblemon's Lucky Egg multiplier when it holds one, otherwise 1.
     */
    fun heldItemMultiplier(pokemon: Pokemon): Double {
        return if (holdsLuckyEgg(pokemon)) Cobblemon.config.luckyEggMultiplier else 1.0
    }

    /**
     * Experience multiplier given by a held Exp. Share while Exp. Share All is on. Every party member holding a Lucky Egg
     * adds the configured bonus, and the total is split between every member holding an Exp. Share.
     * Ex: 4 Lucky Eggs and 2 Exp. Shares with a 0.5 bonus = +2× split in two, so 2× for each holder.
     *
     * @param battlePokemon Party member holding the item.
     * @return The Exp. Share multiplier, or 1 when it doesn't hold one.
     */
    fun expShareMultiplier(battlePokemon: BattlePokemon): Double {
        if (!holdsExpShare(battlePokemon.effectedPokemon)) return 1.0

        val party = battlePokemon.actor.pokemonList
        val luckyEggs = party.count { holdsLuckyEgg(it.effectedPokemon) }
        val expShares = party.count { holdsExpShare(it.effectedPokemon) }
        return 1.0 + DynamicConfig.data.defaultExpShareConfig.expShareItemBonus * luckyEggs / expShares
    }

    /**
     * Experience a party member earns from a fainted opponent under Exp. Share All.
     *
     * @param battlePokemon Party member being rewarded.
     * @param opponentPokemon Fainted opponent.
     * @param delegate Calculator installed before Exp. Share All.
     * @return The member's experience, or `null` when Cobblemon's own rules should apply.
     */
    fun experienceFor(battlePokemon: BattlePokemon, opponentPokemon: BattlePokemon, delegate: ExperienceCalculator): Int? {
        if (!Feature.EXP_SHARE_ALL.isEnabled) return null
        if (battlePokemon.actor !is PlayerBattleActor) return null

        val faced = battlePokemon.actor.pokemonList.filter { opponentPokemon in it.facedOpponents }
        if (faced.isEmpty()) return null

        val config = DynamicConfig.data.defaultExpShareConfig
        val isActive = battlePokemon in faced
        if (isActive && config.expDivisionMethod == ExpDivisionMethod.MULTIPLIED) {
            val experience = delegate.calculate(battlePokemon, opponentPokemon, 1.0) * expShareMultiplier(battlePokemon)
            return experience.roundToInt().coerceAtLeast(1)
        }

        val base = faced.maxOf { baseExperience(it, opponentPokemon, delegate) }
        val members = rewardedMembers(battlePokemon, opponentPokemon)
        val activeMembers = faced.count { it in members }

        val slice = when (config.expDivisionMethod) {
            ExpDivisionMethod.MULTIPLIED -> base * config.expShareMultiplier
            ExpDivisionMethod.SHARED -> sharedSlice(base, members.size, activeMembers, isActive, config.expShareMethod)
        }
        val boosted = slice * heldItemMultiplier(battlePokemon.effectedPokemon) * expShareMultiplier(battlePokemon)
        return boosted.roundToInt().coerceAtLeast(1)
    }

    /**
     * Whether a benched party member should get no EVs from a fainted opponent: Exp. Share All is on, the member didn't
     * face it and doesn't really hold an Exp. Share.
     *
     * @param battlePokemon Party member being rewarded.
     * @param opponentPokemon Fainted opponent.
     * @return Whether the member's EV yield should be dropped.
     */
    fun skipsEvs(battlePokemon: BattlePokemon, opponentPokemon: BattlePokemon): Boolean {
        val pokemon = battlePokemon.effectedPokemon
        if (!appliesTo(pokemon)) return false
        if (opponentPokemon in battlePokemon.facedOpponents) return false
        return !holdsExpShare(pokemon)
    }

    /**
     * Party members Cobblemon rewards for a fainted opponent at the end of a battle. Fainted members only count with
     * `awardExperienceToFaintedPokemon`, and only when they fainted after the opponent, like Cobblemon's own loop.
     */
    private fun rewardedMembers(battlePokemon: BattlePokemon, opponentPokemon: BattlePokemon): List<BattlePokemon> {
        val awardToFainted = Cobblemon.config.awardExperienceToFaintedPokemon
        val opponentFaintedAt = opponentPokemon.faintedAt
        return battlePokemon.actor.pokemonList.filter { member ->
            when {
                member.health > 0 -> true
                !awardToFainted -> false
                opponentFaintedAt == null -> false
                else -> member.faintedAt.let { it == null || it > opponentFaintedAt }
            }
        }
    }

    /**
     * Experience an active Pokémon would earn at full participation, without its own held item boost.
     */
    private fun baseExperience(active: BattlePokemon, opponentPokemon: BattlePokemon, delegate: ExperienceCalculator): Double {
        return delegate.calculate(active, opponentPokemon, 1.0) / heldItemMultiplier(active.effectedPokemon)
    }

    /**
     * Slice of the base experience the shared division gives to one member. When every member faced the opponent,
     * balanced falls back to splitting the whole base between them.
     */
    private fun sharedSlice(base: Double, memberCount: Int, activeCount: Int, isActive: Boolean, method: ExpShareMethod): Double {
        return when {
            method == ExpShareMethod.EQUALLY -> base / memberCount
            activeCount >= memberCount -> base / activeCount
            isActive -> base * BALANCED_ACTIVE_SHARE / activeCount
            else -> base * (1 - BALANCED_ACTIVE_SHARE) / (memberCount - activeCount)
        }
    }

    private fun holdsLuckyEgg(pokemon: Pokemon): Boolean = pokemon.heldItem().`is`(CobblemonItemTags.LUCKY_EGG)

    private fun holdsExpShare(pokemon: Pokemon): Boolean = pokemon.heldItem().`is`(CobblemonItemTags.EXPERIENCE_SHARE)
}

/**
 * [ExperienceCalculator] applying Exp. Share All on top of the calculator installed before it.
 *
 * @property delegate Calculator used for the base experience and whenever Exp. Share All doesn't apply.
 *
 * @author hadouz01
 * @since 1.11.0
 */
class ExpShareAllCalculator(private val delegate: ExperienceCalculator) : ExperienceCalculator {

    override fun calculate(battlePokemon: BattlePokemon, opponentPokemon: BattlePokemon, participationMultiplier: Double): Int {
        return ExpShareAll.experienceFor(battlePokemon, opponentPokemon, delegate)
            ?: delegate.calculate(battlePokemon, opponentPokemon, participationMultiplier)
    }
}

/**
 * [EvCalculator] that keeps benched party members from getting EVs through Exp. Share All, on top of the calculator
 * installed before it.
 *
 * @property delegate Calculator used for every Pokémon that faced the opponent or really holds an Exp. Share.
 *
 * @author hadouz01
 * @since 1.11.0
 */
class ExpShareAllEvCalculator(private val delegate: EvCalculator) : EvCalculator {

    override fun calculate(battlePokemon: BattlePokemon): Map<Stat, Int> = delegate.calculate(battlePokemon)

    override fun calculate(battlePokemon: BattlePokemon, opponentPokemon: BattlePokemon): Map<Stat, Int> {
        if (ExpShareAll.skipsEvs(battlePokemon, opponentPokemon)) return emptyMap()
        return delegate.calculate(battlePokemon, opponentPokemon)
    }
}
