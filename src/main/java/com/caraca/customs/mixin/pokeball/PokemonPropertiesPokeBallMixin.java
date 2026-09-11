package com.caraca.customs.mixin.pokeball;

import com.caraca.customs.balls.PokeBallPropertyAliases;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import kotlin.Pair;
import kotlin.jvm.functions.Function1;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Mixin targeting Cobblemon's {@link PokemonProperties} parser so namespace-less {@code pokeball=} values
 * resolve to the custom Poké Balls before Cobblemon defaults them to the {@code cobblemon} namespace.
 *
 * @author hadouz01
 * @since 1.3.20
 */
@Mixin(PokemonProperties.Companion.class)
public abstract class PokemonPropertiesPokeBallMixin {

    /**
     * Rewrites the matched {@code pokeball} key pair to the custom ball's full id, when one exists for that name.
     *
     * @param keyPairs     Remaining key/value pairs of the property string.
     * @param validKeys    Keys being parsed by this call.
     * @param valueFetcher Cobblemon's registry lookup.
     * @param cir          Callback holding the parsed value.
     */
    @Inject(method = "parseIdentifierOfRegistry", at = @At("HEAD"), remap = false)
    private void caraca$resolveCustomPokeBall(
            List<Pair<String, String>> keyPairs,
            List<String> validKeys,
            Function1<ResourceLocation, String> valueFetcher,
            CallbackInfoReturnable<String> cir
    ) {
        if (validKeys.stream().noneMatch(PokeBallPropertyAliases.INSTANCE.getKEYS()::contains)) return;

        for (int i = keyPairs.size() - 1; i >= 0; i--) {
            Pair<String, String> pair = keyPairs.get(i);
            if (!validKeys.contains(pair.getFirst())) continue;

            String second = pair.getSecond();
            String resolved = PokeBallPropertyAliases.resolve(second == null ? null : second.toLowerCase());
            if (resolved != null) {
                keyPairs.set(i, new Pair<>(pair.getFirst(), resolved));
            }
            return;
        }
    }
}
