package com.caraca.customs.mixin.client;

import com.caraca.customs.client.scan.CaracatronScanner;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes Cobblemon treat a player using the Caracatron 3000 as using a Pokédex, so its HUD runs the scanner overlay and its
 * party keybinds stay off while scanning, through {@link CaracatronScanner}.
 *
 * @author hadouz01
 * @since 1.20.0
 */
@Mixin(value = PlayerExtensionsKt.class, remap = false)
public abstract class PlayerExtensionsMixin {

    @Inject(method = "isUsingPokedex", at = @At("RETURN"), cancellable = true)
    private static void caraca$countCaracatronAsPokedex(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && CaracatronScanner.isUsing(player)) cir.setReturnValue(true);
    }
}
