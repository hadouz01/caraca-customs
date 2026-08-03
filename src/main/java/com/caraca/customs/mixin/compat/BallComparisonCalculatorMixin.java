package com.caraca.customs.mixin.compat;

import com.caraca.customs.balls.CustomBallRegistry;
import com.catchrate.client.BallComparisonCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side Mixin targeting {@link BallComparisonCalculator} from the `catchrate-display` mod.
 * <p>
 * Injects fully qualified namespaced Poké Ball identifiers into the comparison list
 * to trigger HUD rendering for both custom Poké Balls.
 *
 * @author hadouz01
 * @since 1.0.0
 */
@Mixin(value = BallComparisonCalculator.class, remap = false)
public abstract class BallComparisonCalculatorMixin {

    /**
     * Injects custom ball identifiers into the return list of {@code comparableBalls()}.
     *
     * @param cir The callback info returnable holding the original list of comparable ball IDs.
     */
    @Inject(method = "comparableBalls", at = @At("RETURN"), cancellable = true)
    private static void injectCaracaBalls(CallbackInfoReturnable<List<String>> cir) {
        List<String> currentList = new ArrayList<>(cir.getReturnValue());

        CustomBallRegistry.INSTANCE.getRegisteredBalls().forEach(ball -> {
            String fullIdentifier = ball.getId().toString();
            if (!currentList.contains(fullIdentifier)) {
                currentList.add(fullIdentifier);
            }
        });

        cir.setReturnValue(currentList);
    }
}