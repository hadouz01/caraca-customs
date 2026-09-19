package com.caraca.customs.mixin.berry;

import com.caraca.customs.config.DynamicConfig;
import com.caraca.customs.config.Feature;
import com.cobblemon.mod.common.api.berry.Berry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting Cobblemon's {@link Berry} to raise the maximum yield accepted by
 * {@code BerryYieldCalculationEvent}.
 * <p>
 * The event rejects any yield above {@code Berry.maxYield()}, so the native value is raised to at least
 * {@link DynamicConfig#MAX_YIELD_CAP}, the highest yield this mod can assign (Rich Mulch included).
 *
 * @author hadouz01
 * @since 1.0.0
 */
@Mixin(Berry.class)
public abstract class BerryMaxYieldMixin {

    /**
     * Raises the returned maximum yield to the mod's yield cap without lowering higher native values.
     *
     * @param cir Callback holding the native maximum yield.
     */
    @Inject(method = "maxYield", at = @At("RETURN"), cancellable = true, remap = false)
    private void caraca$raiseMaxYield(CallbackInfoReturnable<Integer> cir) {
        if (!Feature.berriesEnabled()) {
            return;
        }

        cir.setReturnValue(Math.max(cir.getReturnValue(), DynamicConfig.MAX_YIELD_CAP));
    }
}
