package com.caraca.customs.mixin.client;

import com.caraca.customs.client.gui.caracatron.CaracatronPokedexSkin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Redraws the texts of Cobblemon's Pokédex in the Caracatron screen color's text colors while the Pokédex is the
 * Caracatron's Pokédex page ({@link CaracatronPokedexSkin#drawText}). Cobblemon's text helpers and Minecraft's
 * component version of {@code drawString} all end in these two methods.
 *
 * @author hadouz01
 * @since 1.29.0
 */
@Mixin(GuiGraphics.class)
public abstract class PokedexTextMixin {

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I", at = @At("HEAD"), cancellable = true)
    private void caraca$dressPokedexText(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
        Integer drawn = CaracatronPokedexSkin.drawText((GuiGraphics) (Object) this, font, text, x, y, color, shadow);
        if (drawn != null) cir.setReturnValue(drawn);
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I", at = @At("HEAD"), cancellable = true)
    private void caraca$dressPokedexString(Font font, String text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
        if (text == null) return;
        Integer drawn = CaracatronPokedexSkin.drawText((GuiGraphics) (Object) this, font, text, x, y, color, shadow);
        if (drawn != null) cir.setReturnValue(drawn);
    }
}
