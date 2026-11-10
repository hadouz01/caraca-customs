package com.caraca.customs.mixin.pokeball;

import com.caraca.customs.balls.PokeBallPropertyAliases;
import com.cobblemon.mod.common.pokemon.properties.PropertiesCompletionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

/**
 * Mixin targeting Cobblemon's {@link PropertiesCompletionProvider} so the {@code pokeball=} tab completion lists
 * the custom Poké Balls by their bare name, matching how {@link PokemonPropertiesPokeBallMixin} resolves them.
 *
 * @author hadouz01
 * @since 1.3.20
 */
@Mixin(PropertiesCompletionProvider.class)
public abstract class PropertiesCompletionProviderMixin {

    /**
     * Swaps the default Poké Ball suggestions for {@link PokeBallPropertyAliases#suggestions()}, leaving every other
     * property untouched.
     *
     * @param provider    Cobblemon's completion provider.
     * @param keys        Property keys of the injected suggestions.
     * @param suggestions Default suggestions.
     */
    @Redirect(
            method = "addDefaults",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/cobblemon/mod/common/pokemon/properties/PropertiesCompletionProvider;inject(Ljava/lang/Iterable;Ljava/util/Collection;)V"
            ),
            remap = false
    )
    private void caraca$injectCustomPokeBalls(PropertiesCompletionProvider provider, Iterable<String> keys, Collection<String> suggestions) {
        boolean isPokeBallKey = false;
        for (String key : keys) {
            if (PokeBallPropertyAliases.INSTANCE.getKEYS().contains(key)) {
                isPokeBallKey = true;
                break;
            }
        }
        provider.inject(keys, isPokeBallKey ? PokeBallPropertyAliases.suggestions() : suggestions);
    }
}
