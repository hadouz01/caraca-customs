package com.caraca.customs.mixin.compat;

import com.caraca.customs.config.BerryConfigEntry;
import com.caraca.customs.config.DynamicConfig;
import com.cobblemon.mod.common.api.berry.Berry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting Cobblemon's Berry class to dynamically expand maximum harvest yield limits.
 *
 * Overrides native maxYield evaluations using dynamic parameters from the mod's configuration,
 * permitting custom yield expansion and Fortune enchantments without encountering bounds crashes.
 *
 * @author hadouz01
 * @since 1.0.0
 */
@Mixin(Berry.class)
public abstract class BerryMaxYieldMixin {

    /**
     * Shadow accessor for the base minimum yield configured in the Berry itself.
     */
    @Shadow(remap = false)
    public abstract int minYield();

    /**
     * Intercepts maxYield calls to return dynamic limits.
     *
     * Appends a safety margin (+10) above the configured maximum yield to safely accommodate
     * external Fortune enchantment bonuses without triggering index exceptions.
     *
     * @param cir Callback info context intercepting the integer return value.
     */
    @Inject(method = "maxYield", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectDynamicMaxYield(CallbackInfoReturnable<Integer> cir) {
        Berry currentBerry = (Berry) (Object) this;
        String berryId = currentBerry.getIdentifier().toString();

        // Removed INSTANCE call since getConfigFor is @JvmStatic
        BerryConfigEntry config = DynamicConfig.getConfigFor(berryId);
        int configuredMaxYield = config.getMaxYield();

        int safeMaxYield = Math.max(configuredMaxYield + 10, this.minYield());
        cir.setReturnValue(safeMaxYield);
    }
}