package com.caraca.customs.mixin.progression;

import com.caraca.customs.progression.ExpShareAll;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin targeting Cobblemon's {@link PokemonBattle} to let benched party members into the Exp. Share branch of
 * {@code end()}.
 * <p>
 * Cobblemon only rewards a Pokémon that faced the fainted opponent or holds an Exp. Share. While Exp. Share All is on,
 * every player-owned member passes the held item check, and {@code ExpShareAllCalculator} replaces the Exp. Share
 * multiplier with the configured split.
 *
 * @author hadouz01
 * @since 1.11.0
 */
@Mixin(PokemonBattle.class)
public abstract class ExpShareAllMixin {

    /**
     * Treats every player-owned party member as an Exp. Share holder while Exp. Share All is enabled.
     *
     * @param original Whether the Pokémon really holds an Exp. Share.
     * @param pokemon  Party member being rewarded.
     * @return Whether the Pokémon enters the Exp. Share branch.
     */
    @ModifyExpressionValue(
            method = "end",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z",
                    remap = true
            ),
            remap = false
    )
    private boolean caraca$shareWithBench(boolean original, @Local(name = "pokemon") Pokemon pokemon) {
        return original || ExpShareAll.appliesTo(pokemon);
    }
}
