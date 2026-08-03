package com.caraca.customs.mixin.compat;

import com.caraca.customs.balls.CustomBallRegistry;
import com.caraca.customs.balls.modules.ModularPokeBall;
import com.catchrate.CatchRateFormula;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting {@link CatchRateFormula} from the `catchrate-display` mod.
 * <p>
 * Overrides the display name formatting logic for custom Poké Balls, resolving
 * localized client translation keys (e.g., `item.caraca_customs.fire_ball`) instead
 * of falling back to raw identifier string manipulation.
 *
 * @author hadouz01
 * @since 1.0.0
 */
@Mixin(value = CatchRateFormula.class, remap = false)
public abstract class CatchRateFormulaMixin {

    /**
     * Intercepts {@code formatBallName()} to resolve localized translation components for `caraca_customs` Poké Balls.
     *
     * @param ballId The identifier string of the Poké Ball.
     * @param cir The callback info returnable holding the formatted string for HUD rendering.
     */
    @Inject(method = "formatBallName", at = @At("HEAD"), cancellable = true)
    private static void formatCaracaCustomBallName(String ballId, CallbackInfoReturnable<String> cir) {
        if (ballId == null) return;

        // O(1) Lookup
        String cleanId = ballId.toLowerCase().replace("caraca_customs:", "");
        ModularPokeBall matchedBall = CustomBallRegistry.INSTANCE.getBallByName(cleanId);

        if (matchedBall != null) {
            // Obtains the item description ID (e.g., "item.caraca_customs.custom_ball")
            String translationKey = matchedBall.getItem().getDescriptionId();

            // Resolves the localized client string based on selected language
            String translatedName = Component.translatable(translationKey).getString();

            cir.setReturnValue(translatedName);
        }
    }
}