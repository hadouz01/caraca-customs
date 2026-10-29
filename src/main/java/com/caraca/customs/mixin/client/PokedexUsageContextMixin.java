package com.caraca.customs.mixin.client;

import com.caraca.customs.client.scan.CaracatronScanner;
import com.cobblemon.mod.common.pokedex.scanner.PokedexUsageContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Speeds up Cobblemon's scanner while it runs from the Caracatron 3000, adding the extra progress through
 * {@link CaracatronScanner#speedUp} before each render update.
 *
 * @author hadouz01
 * @since 1.20.0
 */
@Mixin(value = PokedexUsageContext.class, remap = false)
public abstract class PokedexUsageContextMixin {

    @Inject(method = "renderUpdate", at = @At("HEAD"))
    private void caraca$speedUpCaracatronScan(GuiGraphics graphics, DeltaTracker tickCounter, CallbackInfo ci) {
        CaracatronScanner.speedUp((PokedexUsageContext) (Object) this, tickCounter);
    }
}
