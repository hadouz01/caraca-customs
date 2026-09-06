package com.caraca.customs.mixin.berry;

import com.caraca.customs.config.BerryConfigEntry;
import com.caraca.customs.config.DynamicConfig;
import com.caraca.customs.mulch.MulchTracker;
import com.caraca.customs.util.BerryDebugNotifier;
import com.cobblemon.mod.common.api.berry.Berry;
import com.cobblemon.mod.common.api.mulch.MulchVariant;
import com.cobblemon.mod.common.block.entity.BerryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Mixin targeting Cobblemon's {@link BerryBlockEntity} to dynamically calculate growth stage durations,
 * apply Growth Mulch stage acceleration, and manage the persistent Mulch harvest counter.
 * <p>
 * Vanilla lifecycle methods ({@code saveAdditional}, {@code loadAdditional}) are remapped, while
 * Cobblemon members are targeted with {@code remap = false}.
 *
 * @author hadouz01
 * @since 1.0.0
 */
@Mixin(BerryBlockEntity.class)
public abstract class BerryBlockEntityGrowthMixin implements MulchTracker {

    @Unique
    private static final int TICKS_PER_MINUTE = 1200;

    @Unique
    private static final int LOWER_BOUND_OFFSET = 1000;

    @Unique
    private static final int MIN_STAGE_TICKS = 200;

    @Unique
    private static final int MIN_GROWTH_MULCH_STAGE_TICKS = 100;

    @Unique
    private static final int GROWTH_MULCH_DIVISOR = 3;

    @Unique
    private static final String HARVEST_KEY = "CaracaMulchHarvests";

    @Unique
    private int caraca$lastCalculatedTicks = 0;

    @Unique
    private int caraca$mulchHarvests = MulchTracker.MULCH_HARVEST_USES;

    @Shadow(remap = false)
    private MulchVariant mulchVariant;

    @Shadow(remap = false)
    public abstract Berry berry();

    @Shadow(remap = false)
    public abstract void setStageTimer(int value);

    @Shadow(remap = false)
    public abstract void setGrowthTimer(int value);

    @Shadow(remap = false)
    public abstract void setMulch(MulchVariant mulch, Level world, BlockState state, BlockPos pos);

    @Unique
    private MulchVariant caraca$getSafeMulch() {
        return this.mulchVariant != null ? this.mulchVariant : MulchVariant.NONE;
    }

    @Override
    public boolean caraca$consumeMulchHarvest() {
        if (caraca$getSafeMulch() == MulchVariant.NONE) {
            this.caraca$mulchHarvests = MulchTracker.MULCH_HARVEST_USES;
            return false;
        }

        this.caraca$mulchHarvests--;
        if (this.caraca$mulchHarvests > 0) {
            return false;
        }

        this.caraca$mulchHarvests = MulchTracker.MULCH_HARVEST_USES;
        caraca$clearMulch();
        return true;
    }

    /**
     * Removes the active mulch through Cobblemon's own {@code setMulch}, which resets the native duration,
     * marks the entity dirty and notifies clients so the mulch model disappears.
     */
    @Unique
    private void caraca$clearMulch() {
        BerryBlockEntity self = (BerryBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) {
            this.mulchVariant = MulchVariant.NONE;
            return;
        }
        this.setMulch(MulchVariant.NONE, level, self.getBlockState(), self.getBlockPos());
    }

    /**
     * Serializes the custom mulch harvest counter into the block entity compound tag.
     */
    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void caraca$writeMulchHarvests(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        tag.putInt(HARVEST_KEY, this.caraca$mulchHarvests);
    }

    /**
     * Deserializes the custom mulch harvest counter from the block entity compound tag.
     */
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void caraca$readMulchHarvests(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        this.caraca$mulchHarvests = tag.contains(HARVEST_KEY) ? tag.getInt(HARVEST_KEY) : MulchTracker.MULCH_HARVEST_USES;
    }

    /**
     * Replaces Cobblemon's stage timer calculation with configuration-driven durations,
     * dividing the result when Growth Mulch is active.
     */
    @Inject(method = "goToNextStageTimer", at = @At("HEAD"), cancellable = true, remap = false)
    private void caraca$normalizeStageTimer(int stagesLeft, CallbackInfo ci) {
        Berry currentBerry = this.berry();
        if (currentBerry == null || stagesLeft <= 0) {
            return;
        }

        MulchVariant activeMulch = caraca$getSafeMulch();
        int nextStageTicks = caraca$calculateRandomStageTicks(currentBerry, this.caraca$lastCalculatedTicks);

        if (activeMulch == MulchVariant.GROWTH) {
            nextStageTicks = Math.max(MIN_GROWTH_MULCH_STAGE_TICKS, nextStageTicks / GROWTH_MULCH_DIVISOR);
        }

        this.caraca$lastCalculatedTicks = nextStageTicks;
        this.setStageTimer(nextStageTicks);
        this.setGrowthTimer(nextStageTicks);

        caraca$dispatchDebugNotification(currentBerry, nextStageTicks, activeMulch);
        ci.cancel();
    }

    /**
     * Generates a random tick duration for a growth stage within the berry's configured bounds.
     */
    @Unique
    private int caraca$calculateRandomStageTicks(Berry berry, int previousTicks) {
        BerryConfigEntry config = DynamicConfig.getConfigFor(berry.getIdentifier().toString());

        int safeMinMinutes = Math.max(1, config.getMinGrowthTime());
        int safeMaxMinutes = Math.max(safeMinMinutes, config.getMaxGrowthTime());

        int upperBoundTicks = (safeMinMinutes == safeMaxMinutes)
                ? (safeMaxMinutes * TICKS_PER_MINUTE) / 2
                : (safeMaxMinutes * TICKS_PER_MINUTE);
        int lowerBoundTicks = Math.max(MIN_STAGE_TICKS, (safeMinMinutes * TICKS_PER_MINUTE) - LOWER_BOUND_OFFSET);

        if (lowerBoundTicks >= upperBoundTicks) {
            return lowerBoundTicks;
        }

        int targetUpperBound = caraca$resolveDynamicUpperBound(lowerBoundTicks, upperBoundTicks, previousTicks);
        return ThreadLocalRandom.current().nextInt(lowerBoundTicks, Math.max(lowerBoundTicks + 1, targetUpperBound + 1));
    }

    /**
     * Lowers the upper bound to the average when the previous stage was already above it,
     * avoiding consecutive long stages.
     */
    @Unique
    private int caraca$resolveDynamicUpperBound(int lowerBound, int upperBound, int previousTicks) {
        int averageTicks = (lowerBound + upperBound) / 2;
        if (previousTicks > averageTicks && averageTicks > lowerBound) {
            return averageTicks;
        }
        return upperBound;
    }

    /**
     * Broadcasts the calculated stage duration when the growth tick debugger is enabled.
     */
    @Unique
    private void caraca$dispatchDebugNotification(Berry berry, int ticks, MulchVariant activeMulch) {
        if (!DynamicConfig.getData().getDebugGrowthTick()) {
            return;
        }

        BerryDebugNotifier.INSTANCE.notifyStageTicksCalculated(
                (BerryBlockEntity) (Object) this,
                berry.getIdentifier().getPath(),
                ticks,
                activeMulch == MulchVariant.RICH,
                activeMulch == MulchVariant.GROWTH
        );
    }
}
