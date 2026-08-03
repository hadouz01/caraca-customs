package com.caraca.customs.mixin.compat;

import com.caraca.customs.config.BerryConfigEntry;
import com.caraca.customs.config.DynamicConfig;
import com.cobblemon.mod.common.api.berry.Berry;
import com.cobblemon.mod.common.block.entity.BerryBlockEntity;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mixin targeting Cobblemon's BerryBlockEntity to intercept default yield generation.
 *
 * In Cobblemon 1.7.3, yields are driven by an ArrayList of 'growthPoints'. This Mixin
 * clears that list and repopulates it based on the dynamic configuration bounds,
 * bypassing the native 6-item cap completely.
 *
 * @author hadouz01
 * @since 1.0.0
 */
@Mixin(BerryBlockEntity.class)
public abstract class BerryBlockEntityYieldDropMixin {

    /**
     * Shadow accessor to retrieve the currently planted Berry instance.
     *
     * @return The active {@link Berry} object, or null if the bush is empty.
     */
    @Shadow(remap = false)
    public abstract Berry berry();

    /**
     * Shadow accessor for the Kotlin-generated getter of the berryIdentifier property.
     * Bypasses the need to shadow the variable directly, avoiding Kotlin property visibility issues.
     *
     * @return The {@link ResourceLocation} representing the currently planted berry.
     */
    @Shadow(remap = false)
    public abstract ResourceLocation getBerryIdentifier();

    /**
     * Intercepts generateSimpleYields at execution head.
     *
     * Evaluates custom yield bounds using runtime configuration and applies them
     * by populating the underlying growthPoints list, cancelling standard execution.
     *
     * @param ci Callback info context allowing execution cancellation.
     */
    @Inject(method = "generateSimpleYields", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectDynamicYieldGeneration(CallbackInfo ci) {
        Berry currentBerry = this.berry();
        ResourceLocation identifier = this.getBerryIdentifier();

        if (currentBerry == null || identifier == null) {
            return;
        }

        String berryId = currentBerry.getIdentifier().toString();
        BerryConfigEntry config = DynamicConfig.getConfigFor(berryId);

        int calculatedYield = this.caraca$calculateYield(config.getMinYield(), config.getMaxYield());
        this.caraca$applyYieldToGrowthPoints(calculatedYield, identifier);

        ci.cancel();
    }

    /**
     * Replaces the contents of the growthPoints list with the specified amount of berry identifiers.
     *
     * @param yieldAmount The target quantity of berries to drop.
     * @param identifier The specific berry item identifier to populate the list with.
     */
    @Unique
    private void caraca$applyYieldToGrowthPoints(int yieldAmount, ResourceLocation identifier) {
        ArrayList<ResourceLocation> growthPoints = ((BerryBlockEntityAccessor) this).caraca$getGrowthPoints();

        // Clearing previous native calculations
        growthPoints.clear();

        // Repopulating with dynamic configurations
        for (int i = 0; i < yieldAmount; i++) {
            growthPoints.add(identifier);
        }
    }

    /**
     * Calculates a random yield quantity based on dynamic configuration bounds.
     *
     * @param min Configured minimum yield constraint.
     * @param max Configured maximum yield constraint.
     * @return Calculated randomized integer yield.
     */
    @Unique
    private int caraca$calculateYield(int min, int max) {
        if (min <= max) {
            // Using ThreadLocalRandom for higher performance in concurrent chunk ticks
            return ThreadLocalRandom.current().nextInt(min, max + 1);
        }
        return min;
    }
}