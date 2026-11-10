package com.caraca.customs.mixin.pokeball;

import com.caraca.customs.balls.BallMaterials;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Fills the empty corners of the Ultra Poké Ball recipes when the config asks for it, through {@link BallMaterials},
 * before the recipe JSONs are parsed.
 *
 * @author hadouz01
 * @since 1.42.0
 */
@Mixin(RecipeManager.class)
public abstract class BallCornerFillMixin {

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"))
    private void caraca$fillUltraCorners(Map<ResourceLocation, JsonElement> recipes, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        BallMaterials.fillUltraCorners(recipes);
    }
}
