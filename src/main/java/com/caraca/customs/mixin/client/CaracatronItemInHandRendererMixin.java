package com.caraca.customs.mixin.client;

import com.caraca.customs.client.scan.CaracatronScanner;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the first person hands while scanning with the Caracatron 3000, like Cobblemon does for the Pokédex, so they
 * don't cover the scanner overlay.
 *
 * @author hadouz01
 * @since 1.20.0
 */
@Mixin(ItemInHandRenderer.class)
public abstract class CaracatronItemInHandRendererMixin {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void caraca$hideHandsWhileScanning(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equipProgress, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (CaracatronScanner.isUsing(player)) ci.cancel();
    }
}
