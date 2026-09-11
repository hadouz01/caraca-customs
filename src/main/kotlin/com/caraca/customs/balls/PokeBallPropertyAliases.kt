package com.caraca.customs.balls

import com.caraca.CaracaCustoms
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.pokeball.PokeBalls

/**
 * Makes the custom Poké Balls usable through the `pokeball=` Pokémon property (`/givepokemon`, `/pokeedit`, spawns...).
 *
 * Cobblemon defaults namespace-less values to `cobblemon:`, so `pokeball=lavender_ball` would never find a custom
 * ball and `pokeball=poke_ball` would always pick the native one. Bare names now resolve to the custom ball first,
 * while the native ones it replaces stay reachable through their full id (e.g. `cobblemon:poke_ball`).
 *
 * @author hadouz01
 * @since 1.3.20
 */
object PokeBallPropertyAliases {

    /**
     * Property keys that hold a Poké Ball value.
     */
    val KEYS: Set<String> = setOf("pokeball")

    /**
     * Resolves a `pokeball=` value, giving the custom balls priority over Cobblemon's when no namespace is given.
     *
     * @param value Raw property value typed by the user.
     * @return The namespaced custom ball id, or `null` when the value should be left untouched.
     */
    @JvmStatic
    fun resolve(value: String?): String? {
        if (value.isNullOrBlank() || value.contains(':')) return null
        val ball = CustomBallRegistry.getBallByName(value) ?: return null
        return ball.id.toString()
    }

    /**
     * Builds the tab completion list for `pokeball=`: custom balls by their bare name, native balls replaced by a
     * custom one by their full id, and every other ball as Cobblemon already shows it.
     *
     * @return Suggestions in registration order, custom balls first.
     */
    @JvmStatic
    fun suggestions(): List<String> {
        val customSuggestions = CustomBallRegistry.getRegisteredBalls().map { it.id.path }
        val others = PokeBalls.all().filter { it.name.namespace != CaracaCustoms.MOD_ID }
        val otherSuggestions = others.map { ball ->
            val id = ball.name
            when {
                id.namespace != Cobblemon.MODID -> id.toString()
                CustomBallRegistry.getBallByName(id.path) != null -> id.toString()
                else -> id.path
            }
        }
        return customSuggestions + otherSuggestions
    }
}
