package com.caraca.customs.mixin.berry;

import com.caraca.customs.config.BerryConfigEntry;
import com.caraca.customs.config.DynamicConfig;
import com.caraca.customs.config.Feature;
import com.cobblemon.mod.common.api.berry.Berry;
import com.cobblemon.mod.common.block.entity.BerryBlockEntity;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mixin targeting Cobblemon's {@link BerryBlockEntity} to apply configured yields to naturally generated trees.
 * <p>
 * Runs after Cobblemon's {@code generateSimpleYields}, replacing the native growth points with the configured
 * amount while keeping the native growth point sequence shuffle intact.
 *
 * @author hadouz01
 * @since 1.0.0
 */
@Mixin(BerryBlockEntity.class)
public abstract class BerryBlockEntityYieldMixin {

    @Shadow(remap = false)
    @Final
    private ArrayList<ResourceLocation> growthPoints;

    @Shadow(remap = false)
    public abstract Berry berry();

    @Shadow(remap = false)
    public abstract ResourceLocation getBerryIdentifier();

    /**
     * Repopulates the growth points of a naturally generated tree using the configured yield bounds.
     */
    @Inject(method = "generateSimpleYields", at = @At("TAIL"), remap = false)
    private void caraca$applyConfiguredSimpleYields(CallbackInfo ci) {
        if (!Feature.berriesEnabled()) {
            return;
        }

        Berry currentBerry = this.berry();
        if (currentBerry == null) {
            return;
        }

        BerryConfigEntry config = DynamicConfig.getConfigFor(currentBerry.getIdentifier().toString());
        int min = config.getMinYield();
        int max = config.getMaxYield();
        int yieldAmount = min <= max ? ThreadLocalRandom.current().nextInt(min, max + 1) : min;

        ResourceLocation identifier = this.getBerryIdentifier();
        this.growthPoints.clear();
        for (int i = 0; i < yieldAmount; i++) {
            this.growthPoints.add(identifier);
        }
    }
}
