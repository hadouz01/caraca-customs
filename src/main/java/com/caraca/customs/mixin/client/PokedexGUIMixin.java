package com.caraca.customs.mixin.client;

import com.caraca.customs.client.gui.caracatron.CaracatronPages;
import com.caraca.customs.client.gui.caracatron.CaracatronPokedexSkin;
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUI;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Turns Cobblemon's Pokédex into the Pokédex page of the Caracatron 3000 when it's opened from the Caracatron's page
 * navigation, through {@link CaracatronPages}: the Caracatron frame is drawn in place of the Pokédex's, the Pokédex is
 * dressed in the Caracatron's screen color while it renders ({@link CaracatronPokedexSkin}), and the page
 * navigation and the screen color select are drawn on top and take their clicks, scrolls and drags. Any other Pokédex
 * is left untouched.
 *
 * @author hadouz01
 * @since 1.25.0
 */
@Mixin(PokedexGUI.class)
public abstract class PokedexGUIMixin {

    @ModifyExpressionValue(
        method = "render",
        at = @At(value = "INVOKE", target = "Lcom/cobblemon/mod/common/client/pokedex/PokedexType;getTexturePath()Lnet/minecraft/resources/ResourceLocation;", remap = false)
    )
    private ResourceLocation caraca$useCaracatronFrame(ResourceLocation original) {
        return CaracatronPages.isFramed((Screen) (Object) this) ? CaracatronPages.FRAME : original;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void caraca$dressCaracatronPokedex(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        CaracatronPokedexSkin.begin((Screen) (Object) this);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void caraca$renderCaracatronNavigation(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        CaracatronPokedexSkin.end();
        CaracatronPages.render((Screen) (Object) this, context, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void caraca$clickCaracatronNavigation(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (CaracatronPages.mouseClicked((Screen) (Object) this, mouseX, mouseY, button)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void caraca$scrollCaracatronColorSelect(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (CaracatronPages.mouseScrolled((Screen) (Object) this, mouseX, mouseY, scrollY)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void caraca$dragCaracatronColorSelect(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (CaracatronPages.mouseDragged((Screen) (Object) this, deltaY)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void caraca$releaseCaracatronColorSelect(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        CaracatronPages.mouseReleased((Screen) (Object) this);
    }
}
