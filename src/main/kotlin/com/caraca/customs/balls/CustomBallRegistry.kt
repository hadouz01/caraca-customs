package com.caraca.customs.balls

import com.caraca.customs.balls.customs.CustomBalls
import com.caraca.customs.balls.modules.ModularPokeBall
import com.caraca.customs.balls.modules.ResourceGenerator
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
 * Central registry manager handling lifecycle initialization and registration for custom Poké Balls.
 *
 * Cobblemon exposes no public Poké Ball registration API, so balls are inserted into the private
 * `PokeBalls.defaults` map and linked to their item through the internal `PokeBall.item` field.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CustomBallRegistry {

    private val registeredBalls = mutableListOf<ModularPokeBall>()
    private val ballsByName = mutableMapOf<String, ModularPokeBall>()
    private val ballsByItem = mutableMapOf<Item, ModularPokeBall>()

    /**
     * Gets all registered modular Poké Balls, in registration order.
     *
     * @return Read-only list of custom Poké Balls.
     */
    fun getRegisteredBalls(): List<ModularPokeBall> = registeredBalls

    /**
     * Looks up a custom Poké Ball by its path identifier.
     *
     * @param nameId Item path string, case-insensitive.
     * @return Matching [ModularPokeBall] instance, or `null` if absent.
     */
    fun getBallByName(nameId: String): ModularPokeBall? = ballsByName[nameId.lowercase()]

    /**
     * Looks up a custom Poké Ball by its item.
     *
     * @param item Target item.
     * @return Matching [ModularPokeBall] instance, or `null` if the item is not a custom Poké Ball.
     */
    fun getBallByItem(item: Item): ModularPokeBall? = ballsByItem[item]

    /**
     * Initializes all declared custom Poké Balls, registers item entries, and hooks into creative tabs.
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
     * Hides the Cobblemon balls replaced by custom ones and prepends the custom balls to Cobblemon's utility tab.
     */
    private fun registerCreativeTabEntries() {
        ItemGroupEvents.modifyEntriesEvent(CobblemonItemGroups.UTILITY_ITEMS_KEY).register { entries ->
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
