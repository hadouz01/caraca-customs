package com.caraca.customs.util

import com.caraca.CaracaCustoms
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.ResourcePackActivationType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * Registers the mod's built-in packs, so its recipes and loot tables win over Cobblemon's.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object BuiltinPackRegistry {

    /**
     * Registers the recipe and loot packs, always on and above the other mods' data.
     */
    fun register() {
        registerPack("caraca_recipe_overrides", "Caraca Customs Recipes")
        registerPack("caraca_loot_overrides", "Caraca Customs Loot Tables")
    }

    /**
     * Registers one of the mod's built-in packs, turned on by default.
     *
     * @param packPath Folder of the pack inside `resources/resourcepacks/`.
     * @param displayName Name shown in the datapack list.
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