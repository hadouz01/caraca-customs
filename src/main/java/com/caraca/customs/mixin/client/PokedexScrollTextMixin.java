package com.caraca.customs.mixin.client;

import com.caraca.customs.client.gui.caracatron.CaracatronPokedexSkin;
import com.cobblemon.mod.common.client.gui.pokedex.widgets.InfoTextScrollWidget;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells {@link CaracatronPokedexSkin} when the Pokédex's scroll text box (description, abilities, moves) is drawing, so
 * its gray text keeps its color on every screen color.
 *
 * @author hadouz01
 * @since 1.30.0
 */
@Mixin(InfoTextScrollWidget.class)
public abstract class PokedexScrollTextMixin {

    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void caraca$startScrollText(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        CaracatronPokedexSkin.keepPanelText(true);
    }

    @Inject(method = "renderWidget", at = @At("TAIL"))
    private void caraca$endScrollText(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        CaracatronPokedexSkin.keepPanelText(false);
    }
}
