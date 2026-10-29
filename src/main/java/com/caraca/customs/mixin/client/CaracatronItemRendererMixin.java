package com.caraca.customs.mixin.client;

import com.caraca.customs.client.render.CaracatronItemModels;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Renders the Caracatron 3000 like Cobblemon's Pokédex, through {@link CaracatronItemModels}: the 3D handheld model is
 * returned for every Caracatron model lookup, and inventories and item frames swap it back for the flat sprite, the
 * item's own model.
 *
 * @author hadouz01
 * @since 1.18.0
 */
@Mixin(ItemRenderer.class)
public abstract class CaracatronItemRendererMixin {

    @Shadow @Final private ItemModelShaper itemModelShaper;

    /**
     * Minecraft's own item render, called again with the swapped model.
     */
    @Shadow
    public abstract void render(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, BakedModel model);

    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void caraca$useCaracatronHandheldModel(ItemStack stack, Level level, LivingEntity entity, int seed, CallbackInfoReturnable<BakedModel> cir) {
        if (!CaracatronItemModels.isCaracatron(stack)) return;
        BakedModel handheld = CaracatronItemModels.handheld(stack, entity, this.itemModelShaper.getModelManager());
        if (handheld != null) cir.setReturnValue(handheld);
    }

    @Inject(
        method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void caraca$useCaracatronSprite(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, BakedModel model, CallbackInfo ci) {
        if (!CaracatronItemModels.isCaracatron(stack) || !CaracatronItemModels.usesSprite(context)) return;
        BakedModel sprite = this.itemModelShaper.getItemModel(stack);
        if (sprite == model) return;
        ci.cancel();
        render(stack, context, leftHand, poseStack, buffers, light, overlay, sprite);
    }
}
