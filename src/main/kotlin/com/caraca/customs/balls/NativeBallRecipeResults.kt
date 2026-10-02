package com.caraca.customs.balls

import com.caraca.customs.config.Feature
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack

/**
 * Swaps crafting results that are Cobblemon balls replaced by a custom ball (same path, like `cobblemon:poke_ball` and
 * `caraca_customs:poke_ball`) for the custom ball, while [Feature.CUSTOM_BALLS] is enabled.
 *
 * The built-in `caraca_recipe_overrides` pack already replaces Cobblemon's recipes, but any pack loaded above it (another
 * mod, a modpack or a world datapack) can bring Cobblemon's originals back. Applied by `ShapedRecipeResultMixin`, this
 * keeps those recipes crafting the custom balls no matter where they come from. Balls without a replacement, like the
 * Great Ball, are left alone, and nothing changes while this mod's custom balls are off.
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
