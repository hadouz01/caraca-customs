package com.caraca.customs.mixin.caracatron;

import com.caraca.customs.caracatron.Caracatron;
import com.cobblemon.mod.common.net.serverhandling.pokedex.scanner.FinishScanningHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Cobblemon only registers a scan after 15 ticks on the server ({@code SUCCESS_SCAN_SERVER_TICKS}). The Caracatron
 * scans faster, so it needs fewer ticks, otherwise anything above 2x would still wait the full 15.
 *
 * @author hadouz01
 * @since 1.22.0
 */
@Mixin(value = FinishScanningHandler.class, remap = false)
public abstract class FinishScanningHandlerMixin {

    /**
     * Lowers the ticks a scan needs when the player scans with a Caracatron.
     *
     * @param original Ticks Cobblemon requires.
     * @param player   Player finishing the scan.
     * @return The ticks the scan needs.
     */
    @ModifyExpressionValue(method = "handle", at = @At(value = "CONSTANT", args = "intValue=15"))
    private int caraca$shortenCaracatronScan(int original, @Local(argsOnly = true) ServerPlayer player) {
        return Caracatron.requiredScanTicks(player, original);
    }
}
