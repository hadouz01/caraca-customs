package com.caraca.customs.balls

import com.caraca.customs.balls.customs.CustomBalls
import com.caraca.customs.balls.modules.ModularPokeBall
import com.caraca.customs.balls.modules.ResourceGenerator
import com.caraca.customs.config.Feature
import com.cobblemon.mod.common.api.pokeball.PokeBalls
import com.cobblemon.mod.common.item.group.CobblemonItemGroups
import com.cobblemon.mod.common.pokeball.PokeBall
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * Registers the custom Poké Balls.
 *
 * Cobblemon has no public API for it, so the balls go straight into the private `PokeBalls.defaults` map and get linked
 * to their item through the internal `PokeBall.item` field.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CustomBallRegistry {

    private val registeredBalls = mutableListOf<ModularPokeBall>()
    private val ballsByName = mutableMapOf<String, ModularPokeBall>()
    private val ballsByItem = mutableMapOf<Item, ModularPokeBall>()

    /**
     * Every custom ball, in registration order.
     */
    fun getRegisteredBalls(): List<ModularPokeBall> = registeredBalls

    /**
     * Finds a custom ball by its path.
     *
     * @param nameId Path of the ball, any case.
     * @return The ball, or `null` when there's none.
     */
    fun getBallByName(nameId: String): ModularPokeBall? = ballsByName[nameId.lowercase()]

    /**
     * Finds the custom ball of an item.
     *
     * @param item Item.
     * @return The ball, or `null` when the item isn't a custom ball.
     */
    fun getBallByItem(item: Item): ModularPokeBall? = ballsByItem[item]

    /**
     * Registers every custom ball and its item, and adds them to the creative tab.
     */
    fun initialize() {
        @Suppress("UNCHECKED_CAST")
        val defaults = PokeBalls::class.java.getDeclaredField("defaults")
            .apply { isAccessible = true }
            .get(PokeBalls) as MutableMap<ResourceLocation, PokeBall>
        val itemField = PokeBall::class.java.getDeclaredField("item").apply { isAccessible = true }

        CustomBalls.ALL.forEach { properties ->
            val modularBall = ModularPokeBall(properties)

            defaults[modularBall.id] = modularBall.pokeBall
            itemField.set(modularBall.pokeBall, modularBall.item)
            Registry.register(BuiltInRegistries.ITEM, modularBall.id, modularBall.item)

            registeredBalls += modularBall
            ballsByName[modularBall.props.nameId.lowercase()] = modularBall
            ballsByItem[modularBall.item] = modularBall

            ResourceGenerator.generate(modularBall.props)
        }

        registerCreativeTabEntries()
    }

    /**
     * Hides the Cobblemon balls replaced by custom ones and prepends the custom balls to Cobblemon's utility tab, while
     * [Feature.CUSTOM_BALLS] is enabled. The tab is rebuilt by the game when joining a world.
     */
    private fun registerCreativeTabEntries() {
        ItemGroupEvents.modifyEntriesEvent(CobblemonItemGroups.UTILITY_ITEMS_KEY).register { entries ->
            if (!Feature.CUSTOM_BALLS.isEnabled) return@register

            val isReplacedCobblemonBall = { stack: ItemStack ->
                val key = BuiltInRegistries.ITEM.getKey(stack.item)
                key.namespace == "cobblemon" && ballsByName.containsKey(key.path)
            }

            entries.displayStacks.removeIf(isReplacedCobblemonBall)
            entries.searchTabStacks.removeIf(isReplacedCobblemonBall)

            registeredBalls.asReversed().forEach { modularBall ->
                entries.prepend(modularBall.item)
            }
        }
    }
}
