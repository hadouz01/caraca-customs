package com.caraca.customs.mixin.client;

import com.caraca.customs.client.gui.caracatron.CaracatronPokedexSkin;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Swaps the textures Cobblemon's Pokédex binds for the Caracatron screen color's versions while the Pokédex is the
 * Caracatron's Pokédex page ({@link CaracatronPokedexSkin#texture}). Both Cobblemon's {@code blitk} and Minecraft's
 * {@code GuiGraphics.blit} bind their textures here, so a single hook covers every Pokédex texture.
 *
 * @author hadouz01
 * @since 1.29.0
 */
@Mixin(RenderSystem.class)
public abstract class PokedexTextureMixin {

    @ModifyVariable(method = "setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V", at = @At("HEAD"), argsOnly = true)
    private static ResourceLocation caraca$dressPokedexTexture(ResourceLocation texture) {
        return CaracatronPokedexSkin.texture(texture);
    }
}
