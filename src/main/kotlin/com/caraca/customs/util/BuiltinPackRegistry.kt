package com.caraca.customs.util

import com.caraca.CaracaCustoms
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.ResourcePackActivationType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * Utility registry managing the activation of internal built-in resource and data packs.
 *
 * Facilitates the programmatic registration of embedded datapacks to ensure custom data overrides
 * (such as recipes and loot tables) take precedence over base mod data packs like Cobblemon.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object BuiltinPackRegistry {

    /**
     * Registers all embedded datapacks into Fabric's resource manager hierarchy.
     *
     * Automatically enables the packs on world loading with elevated priority over standard mod resources.
     * Keeps the cognitive complexity low by delegating the registration logic to a dedicated helper function.
     */
    fun register() {
        registerPack("caraca_recipe_overrides", "Caraca Customs Recipes")
        registerPack("caraca_loot_overrides", "Caraca Customs Loot Tables")
    }

    /**
     * Helper method to register a specific built-in data pack.
     *
     * This method resolves the ModContainer from Fabric API and securely injects the
     * targeted datapack into the game's registry, forcing it to be enabled by default
     * without requiring player intervention in the UI.
     *
     * @param packPath The exact directory name of the pack inside `resources/resourcepacks/`.
     * @param displayName The human-readable name displayed in the game's datapack menu.
     */
    private fun registerPack(packPath: String, displayName: String) {
        val packId = ResourceLocation.fromNamespaceAndPath(CaracaCustoms.MOD_ID, packPath)
        val modContainer = FabricLoader.getInstance().getModContainer(CaracaCustoms.MOD_ID)

        if (modContainer.isPresent) {
            ResourceManagerHelper.registerBuiltinResourcePack(
                packId,
                modContainer.get(),
                Component.literal(displayName),
                ResourcePackActivationType.ALWAYS_ENABLED
            )
        }
    }
}