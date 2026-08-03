package com.caraca.customs.mixin.compat;

import com.caraca.customs.config.BerryConfigEntry;
import com.caraca.customs.config.DynamicConfig;
import com.caraca.customs.mulch.MulchTracker;
import com.caraca.customs.util.BerryDebugNotifier;
import com.cobblemon.mod.common.api.berry.Berry;
import com.cobblemon.mod.common.api.mulch.MulchVariant;
import com.cobblemon.mod.common.block.entity.BerryBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Mixin targeting Cobblemon's BerryBlockEntity to dynamically calculate growth stage durations,
 * apply Growth Mulch stage acceleration, and manage persistent Mulch harvest counters.
 *
 * This class ensures that growth logic applies safely across both integrated and dedicated server environments
 * by cleanly separating obfuscated vanilla Minecraft methods from non-remapped Cobblemon structures.
 *
 * @author hadouz01
 * @since 1.0.0
 */
@Mixin(BerryBlockEntity.class) // Let Fabric decide remapping at a baseline level
public abstract class BerryBushBlockMixin implements MulchTracker {

    private static final int TICKS_PER_MINUTE = 1200;
    private static final int LOWER_BOUND_OFFSET = 1000;
    private static final String HARVEST_KEY = "CaracaMulchHarvests";

    @Unique
    private int caraca$lastCalculatedTicks = 0;

    @Unique
    private int caraca$mulchHarvests = 3;

    /**
     * Shadow target for Cobblemon's internal MulchVariant state field.
     * Flagged with 'remap = false' since Cobblemon mappings are standardized independently of vanilla.
     */
    @Shadow(aliases = {"mulch", "mulchVariant"}, remap = false)
    private MulchVariant mulch;

    @Shadow(remap = false)
    public abstract Berry berry();

    @Shadow(remap = false)
    public abstract void setStageTimer(int value);

    @Shadow(remap = false)
    public abstract void setGrowthTimer(int value);

    @Override
    public MulchVariant getMulch() {
        return this.mulch;
    }

    public void setMulch(MulchVariant mulch) {
        this.mulch = mulch;
    }

    @Unique
    private MulchVariant caraca$getSafeMulch() {
        return this.mulch != null ? this.mulch : MulchVariant.NONE;
    }

    @Override
    public int caraca$getMulchHarvests() {
        return this.caraca$mulchHarvests;
    }

    @Override
    public void caraca$setMulchHarvests(int uses) {
        this.caraca$mulchHarvests = uses;
    }

    @Override
    public boolean caraca$consumeMulchHarvest() {
        MulchVariant activeMulch = caraca$getSafeMulch();
        if (activeMulch == MulchVariant.NONE) {
            this.caraca$mulchHarvests = 4;
            return false;
        }

        this.caraca$mulchHarvests--;

        if (this.caraca$mulchHarvests <= 0) {
            this.setMulch(MulchVariant.NONE);
            this.caraca$mulchHarvests = 4;
            return true;
        }

        return false;
    }

    /**
     * Serializes custom mulch harvest counters into the block entity compound tag.
     * Inherits default (remap = true) as this overrides a core vanilla Minecraft lifecycle method.
     */
    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void writeCustomMulchNbt(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        tag.putInt(HARVEST_KEY, this.caraca$mulchHarvests);
    }

    /**
     * Deserializes custom mulch harvest counters from the block entity compound tag.
     * Inherits default (remap = true) as this overrides a core vanilla Minecraft lifecycle method.
     */
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void readCustomMulchNbt(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        this.caraca$mulchHarvests = tag.contains(HARVEST_KEY) ? tag.getInt(HARVEST_KEY) : 4;
    }

    /**
     * Intercepts stage transition timer setups to dynamically calculate growth ticks.
     * Flagged with 'remap = false' targeting a proprietary Cobblemon function.
     */
    @Inject(method = "goToNextStageTimer", at = @At("HEAD"), cancellable = true, remap = false)
    private void normalizeStageTimer(int stagesLeft, CallbackInfo ci) {
        Berry currentBerry = this.berry();
        if (currentBerry == null || stagesLeft <= 0) {
            return;
        }

        int nextStageTicks = calculateRandomStageTicks(currentBerry, this.caraca$lastCalculatedTicks);
        MulchVariant activeMulch = caraca$getSafeMulch();

        if (activeMulch == MulchVariant.GROWTH) {
            nextStageTicks = Math.max(100, nextStageTicks / 3);
        }

        this.caraca$lastCalculatedTicks = nextStageTicks;
        this.setStageTimer(nextStageTicks);
        this.setGrowthTimer(nextStageTicks);

        dispatchDebugNotification(currentBerry, nextStageTicks, activeMulch);

        ci.cancel();
    }

    /**
     * Generates a random tick duration for a growth stage within dynamically evaluated configurations.
     * Refactored limits determination to lower localized cognitive complexity footprint.
     */
    @Unique
    private int calculateRandomStageTicks(Berry berry, int previousTicks) {
        String berryId = berry.getIdentifier().toString();
        BerryConfigEntry config = DynamicConfig.getConfigFor(berryId);

        int safeMinMinutes = Math.max(1, config.getMinGrowthTime());
        int safeMaxMinutes = Math.max(safeMinMinutes, config.getMaxGrowthTime());

        int upperBoundTicks = (safeMinMinutes == safeMaxMinutes)
                ? (safeMaxMinutes * TICKS_PER_MINUTE) / 2
                : (safeMaxMinutes * TICKS_PER_MINUTE);

        int lowerBoundTicks = Math.max(200, (safeMinMinutes * TICKS_PER_MINUTE) - LOWER_BOUND_OFFSET);

        if (lowerBoundTicks >= upperBoundTicks) {
            return lowerBoundTicks;
        }

        int targetUpperBound = resolveDynamicUpperBound(lowerBoundTicks, upperBoundTicks, previousTicks);
        return ThreadLocalRandom.current().nextInt(lowerBoundTicks, Math.max(lowerBoundTicks + 1, targetUpperBound + 1));
    }

    /**
     * Determines the optimal upper tick bound constraint based on historical generation averages.
     * Extracted from the core generator to strictly isolate branch complexity.
     */
    @Unique
    private int resolveDynamicUpperBound(int lowerBound, int upperBound, int previousTicks) {
        int averageTicks = (lowerBound + upperBound) / 2;
        if (previousTicks > averageTicks && averageTicks > lowerBound) {
            return averageTicks;
        }
        return upperBound;
    }

    /**
     * Queries runtime configuration to dispatch console or chat debug metrics.
     * Extracted to ensure logical clarity inside primary stage transition injection.
     */
    @Unique
    private void dispatchDebugNotification(Berry berry, int ticks, MulchVariant activeMulch) {
        if (DynamicConfig.getData().getDebugGrowthTick()) {
            boolean hasRichMulch = activeMulch == MulchVariant.RICH;
            boolean hasGrowthMulch = activeMulch == MulchVariant.GROWTH;

            BerryDebugNotifier.INSTANCE.notifyStageTicksCalculated(
                    (BerryBlockEntity) (Object) this,
                    berry.getIdentifier().getPath(),
                    ticks, hasRichMulch, hasGrowthMulch
            );
        }
    }
}