package com.caraca.customs.mixin.compat;

import com.cobblemon.mod.common.block.entity.BerryBlockEntity;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.ArrayList;

/**
 * Mixin accessor interface bridging private fields within Cobblemon's BerryBlockEntity.
 *
 * Exposes the internal 'growthPoints' list, which replaced the old integer 'yield' system
 * in Cobblemon 1.7.3. Modifying this list directly dictates the items dropped upon harvest.
 *
 * @author hadouz01
 * @since 1.0.0
 */
@Mixin(BerryBlockEntity.class)
public interface BerryBlockEntityAccessor {

    /**
     * Retrieves the internal list of growth points (harvest drops) for the berry bush.
     *
     * @return The mutable list containing {@link ResourceLocation} identifiers of the berries to drop.
     */
    @Accessor(value = "growthPoints", remap = false)
    ArrayList<ResourceLocation> caraca$getGrowthPoints();
}