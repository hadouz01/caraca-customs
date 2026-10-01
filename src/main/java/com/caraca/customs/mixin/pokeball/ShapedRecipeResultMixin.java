package com.caraca.customs.mixin.pokeball;

import com.caraca.customs.balls.NativeBallRecipeResults;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes shaped recipes that craft a Cobblemon ball replaced by a custom one give the custom ball instead, through
 * {@link NativeBallRecipeResults}, both in the crafting result and in the recipe book.
 *
 * @author hadouz01
 * @since 1.9.0
 */
@Mixin(ShapedRecipe.class)
public abstract class ShapedRecipeResultMixin {

    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), cancellable = true)
    private void caraca$replaceAssembledBall(CraftingInput input, HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(NativeBallRecipeResults.replace(cir.getReturnValue()));
    }

    @Inject(method = "getResultItem", at = @At("RETURN"), cancellable = true)
    private void caraca$replaceResultBall(HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(NativeBallRecipeResults.replace(cir.getReturnValue()));
    }
}
