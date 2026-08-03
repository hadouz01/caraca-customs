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
import java.lang.reflect.Field

/**
 * Central registry manager handling lifecycle initialization and registration for custom Poké Balls.
 *
 * Handles integration between Vanilla Minecraft registries, Fabric API, and Cobblemon core systems.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CustomBallRegistry {

    private val registeredBalls = mutableListOf<ModularPokeBall>()
    private val ballMap = mutableMapOf<String, ModularPokeBall>()

    /**
     * Gets an unmodifiable list of all registered modular Poké Balls.
     *
     * @return Immutable list of custom Poké Balls.
     */
    fun getRegisteredBalls(): List<ModularPokeBall> = registeredBalls

    /**
     * Looks up a custom Poké Ball by its string identifier in O(1) time complexity.
     *
     * @param nameId Lowercase item path string.
     * @return Matching [ModularPokeBall] instance, or `null` if absent.
     */
    fun getBallByName(nameId: String): ModularPokeBall? = ballMap[nameId.lowercase()]

    /**
     * Initializes all declared custom Poké Balls, registers item entries, and hooks into creative tabs.
     */
    fun initialize() {
        val defaultsField = PokeBalls::class.java.getDeclaredField("defaults").apply { isAccessible = true }

        @Suppress("UNCHECKED_CAST")
        val defaults = defaultsField.get(PokeBalls) as MutableMap<ResourceLocation, PokeBall>
        val itemField = PokeBall::class.java.getDeclaredField("item").apply { isAccessible = true }

        CustomBalls.ALL.forEach { properties ->
            val modularBall = ModularPokeBall(properties)
            processRegistration(modularBall, defaults, itemField)
        }

        registerCreativeTabEntries()
    }

    /**
     * Processes individual registration steps for a given custom Poké Ball.
     *
     * @param modularBall Target custom Poké Ball wrapper.
     * @param defaults Reflective reference to Cobblemon defaults map.
     * @param itemField Reflective reference to private item field in [PokeBall].
     */
    private fun processRegistration(
        modularBall: ModularPokeBall,
        defaults: MutableMap<ResourceLocation, PokeBall>,
        itemField: Field
    ) {
        val ballName = modularBall.props.nameId.lowercase()

        // Register custom ball into Cobblemon's internal maps
        defaults[modularBall.id] = modularBall.pokeBall
        itemField.set(modularBall.pokeBall, modularBall.item)

        // Register into Vanilla Minecraft item registry
        Registry.register(BuiltInRegistries.ITEM, modularBall.id, modularBall.item)

        registeredBalls.add(modularBall)
        ballMap[ballName] = modularBall

        ResourceGenerator.generate(modularBall.props)
    }

    /**
     * Configures creative tab modifications for Cobblemon items in a single batch listener.
     *
     * Filters out overridden native Cobblemon items and prepends custom Poké Balls to the
     * beginning of the creative tab to maintain high execution performance.
     */
    private fun registerCreativeTabEntries() {
        ItemGroupEvents.modifyEntriesEvent(CobblemonItemGroups.UTILITY_ITEMS_KEY).register { entries ->
            val registeredNames = ballMap.keys

            entries.displayStacks.removeIf { stack ->
                val key = BuiltInRegistries.ITEM.getKey(stack.item)
                key.namespace == "cobblemon" && registeredNames.contains(key.path)
            }
            entries.searchTabStacks.removeIf { stack ->
                val key = BuiltInRegistries.ITEM.getKey(stack.item)
                key.namespace == "cobblemon" && registeredNames.contains(key.path)
            }

            registeredBalls.asReversed().forEach { modularBall ->
                entries.prepend(modularBall.item)
            }
        }
    }
}