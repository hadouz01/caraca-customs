package com.caraca.customs.balls

import com.caraca.customs.config.Feature
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack

/**
 * Makes recipes that craft a Cobblemon ball replaced by a custom one (same path, like `cobblemon:poke_ball`) craft the
 * custom ball instead, while [Feature.CUSTOM_BALLS] is on.
 *
 * `caraca_recipe_overrides` already replaces Cobblemon's recipes, but a pack loaded above it can bring them back, so
 * `ShapedRecipeResultMixin` swaps the result no matter where the recipe comes from.
 *
 * @author hadouz01
 * @since 1.9.0
 */
object NativeBallRecipeResults {

    private const val COBBLEMON_NAMESPACE = "cobblemon"

    /**
     * Gets the result a recipe should give.
     *
     * @param result The recipe's own result.
     * @return The matching custom ball stack, or [result] itself when there's nothing to replace.
     */
    @JvmStatic
    fun replace(result: ItemStack): ItemStack {
        if (result.isEmpty || !Feature.CUSTOM_BALLS.isEnabled) return result
        val key = BuiltInRegistries.ITEM.getKey(result.item)
        if (key.namespace != COBBLEMON_NAMESPACE) return result
        val customBall = CustomBallRegistry.getBallByName(key.path) ?: return result
        return result.transmuteCopy(customBall.item, result.count)
    }
}
