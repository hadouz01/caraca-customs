package com.caraca.customs.mixin.compat;

import com.caraca.customs.client.compat.CatchRateDisplayCompat;
import com.catchrate.BallMultiplierCalculator;
import com.catchrate.BallMultiplierCalculator.BallContext;
import com.catchrate.BallMultiplierCalculator.BallResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side Mixin targeting {@link BallMultiplierCalculator} from the `catchrate-display` mod.
 * <p>
 * Namespaced ids (e.g. {@code caraca_customs:fire_ball}) are already resolved through the public
 * {@code CatchRateBallRegistry} API registered by {@link CatchRateDisplayCompat}. The held-ball HUD, however,
 * only forwards the ball path (e.g. {@code poke_ball}), so this hook resolves bare ids against the mod's balls.
 *
 * @author hadouz01
 * @since 1.0.0
 */
@Mixin(value = BallMultiplierCalculator.class, remap = false)
public abstract class BallMultiplierCalculatorMixin {

    /**
     * Returns the mod's multiplier for bare ball ids that match a registered custom Poké Ball.
     *
     * @param ballId The ball id forwarded by catchrate-display.
     * @param ctx    The target Pokémon context.
     * @param cir    Callback holding the ball result.
     */
    @Inject(method = "calculate", at = @At("HEAD"), cancellable = true)
    private static void caraca$calculateBareBallId(String ballId, BallContext ctx, CallbackInfoReturnable<BallResult> cir) {
        if (ballId == null || ballId.contains(":")) {
            return;
        }

        BallResult result = CatchRateDisplayCompat.INSTANCE.evaluateBareId(
                ballId, ctx.getPrimaryType(), ctx.getSecondaryType(), ctx.getStatusPath()
        );
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
