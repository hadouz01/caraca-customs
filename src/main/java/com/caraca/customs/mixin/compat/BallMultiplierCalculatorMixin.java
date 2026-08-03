package com.caraca.customs.mixin.compat;

import com.caraca.customs.balls.CustomBallRegistry;
import com.caraca.customs.balls.modifiers.ElementalBallCatchRateModifier;
import com.caraca.customs.balls.modifiers.SimpleCatchRateModifier;
import com.caraca.customs.balls.modules.ModularPokeBall;
import com.caraca.customs.config.DynamicConfig;
import com.caraca.customs.config.PokeBallConfigEntry;
import com.catchrate.BallMultiplierCalculator;
import com.catchrate.BallMultiplierCalculator.BallContext;
import com.catchrate.BallMultiplierCalculator.BallResult;
import com.catchrate.BallTranslations;
import com.cobblemon.mod.common.api.pokeball.catching.CatchRateModifier;
import com.cobblemon.mod.common.api.types.ElementalType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;
import java.util.Set;

/**
 * Technical mixin interceptor for `catchrate-display` mod compatibility.
 *
 * Reads values directly from [DynamicConfig] to support real-time UI updates.
 *
 * @author hadouz01
 * @since 1.0.0
 */
@Mixin(value = BallMultiplierCalculator.class, remap = false)
public abstract class BallMultiplierCalculatorMixin {

    @Inject(method = "calculate", at = @At("HEAD"), cancellable = true)
    private static void calculateCaracaCustomsRate(String ballId, BallContext ctx, CallbackInfoReturnable<BallResult> cir) {
        if (ballId == null) return;

        String cleanId = ballId.contains(":") ? ballId.split(":")[1].toLowerCase(Locale.ROOT) : ballId.toLowerCase(Locale.ROOT);
        ModularPokeBall matchedBall = CustomBallRegistry.INSTANCE.getBallByName(cleanId);

        if (matchedBall == null) {
            return;
        }

        CatchRateModifier modifier = matchedBall.getModifier();

        if (modifier instanceof SimpleCatchRateModifier simpleModifier) {
            PokeBallConfigEntry config = DynamicConfig.INSTANCE.getPokeBallConfigFor(cleanId);
            cir.setReturnValue(buildSimpleResult(config.getDefaultCaptureRate()));
            return;
        }

        if (modifier instanceof ElementalBallCatchRateModifier elementalModifier) {
            cir.setReturnValue(evaluateElementalResult(cleanId, ctx, elementalModifier));
        }
    }

    @Unique
    private static BallResult buildSimpleResult(float multiplier) {
        return new BallResult(
                multiplier,
                true,
                BallTranslations.INSTANCE.multiplierAlways(),
                false
        );
    }

    @Unique
    private static BallResult evaluateElementalResult(String ballId, BallContext ctx, ElementalBallCatchRateModifier modifier) {
        PokeBallConfigEntry config = DynamicConfig.INSTANCE.getPokeBallConfigFor(ballId);
        float defaultMultiplier = config.getDefaultCaptureRate();
        float boostedMultiplier = config.getBoostedCaptureRate();

        Set<ElementalType> types = modifier.getTypes();
        boolean matchesType = isAnyTypeMatching(ctx, types);
        boolean matchesStatus = isAnyStatusMatching(ctx.getStatusPath(), types);
        boolean conditionMet = matchesType || matchesStatus;

        if (!conditionMet) {
            return new BallResult(
                    defaultMultiplier,
                    false,
                    BallTranslations.INSTANCE.multiplierAlways(),
                    false
            );
        }

        boolean isSleeping = isTargetAsleep(ctx.getStatusPath());
        float finalMultiplier = isSleeping ? boostedMultiplier * 2.0f : boostedMultiplier;

        return new BallResult(
                finalMultiplier,
                true,
                BallTranslations.INSTANCE.multiplierAlways(),
                false
        );
    }

    @Unique
    private static boolean isTargetAsleep(String statusPath) {
        if (statusPath == null) return false;
        String status = statusPath.toLowerCase(Locale.ROOT);
        return status.contains("sleep") || status.contains("slp");
    }

    @Unique
    private static boolean isAnyTypeMatching(BallContext ctx, Set<ElementalType> types) {
        String primary = ctx.getPrimaryType().toLowerCase(Locale.ROOT);
        String secondary = ctx.getSecondaryType() != null ? ctx.getSecondaryType().toLowerCase(Locale.ROOT) : null;

        for (ElementalType type : types) {
            String typeName = type.getName().toLowerCase(Locale.ROOT);
            if (primary.equals(typeName) || (secondary != null && secondary.equals(typeName))) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static boolean isAnyStatusMatching(String statusPath, Set<ElementalType> types) {
        if (statusPath == null) return false;

        String status = statusPath.toLowerCase(Locale.ROOT);
        for (ElementalType type : types) {
            String typeName = type.getName().toLowerCase(Locale.ROOT);
            boolean matched = switch (typeName) {
                case "fire" -> status.toUpperCase().contains("BURN");
                case "electric" -> status.toUpperCase().contains("PARALY");
                case "poison" -> status.toUpperCase().contains("POISON");
                case "ice" -> status.toUpperCase().contains("FREEZE") || status.toUpperCase().contains("FROZEN");
                default -> false;
            };
            if (matched) return true;
        }
        return false;
    }
}