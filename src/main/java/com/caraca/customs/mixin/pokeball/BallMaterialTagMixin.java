package com.caraca.customs.mixin.pokeball;

import com.caraca.customs.balls.BallMaterials;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

/**
 * Swaps the Poké Ball material tags for the items picked in the config, through {@link BallMaterials}, right after the
 * tags are read from the datapacks.
 *
 * @author hadouz01
 * @since 1.42.0
 */
@Mixin(TagLoader.class)
public abstract class BallMaterialTagMixin {

    @Shadow
    @Final
    private String directory;

    @Inject(method = "load", at = @At("RETURN"))
    private void caraca$overrideBallMaterials(ResourceManager resourceManager, CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir) {
        BallMaterials.overrideTags(directory, cir.getReturnValue());
    }
}
