package com.caraca

import com.caraca.customs.balls.CustomBallRegistry
import com.caraca.customs.berry.BerryGrowthModifier
import com.caraca.customs.berry.BerryHarvestHandler
import com.caraca.customs.command.CaracaCommand
import com.caraca.customs.config.DynamicConfig
import com.caraca.customs.loot.CustomLootInjector
import com.caraca.customs.util.BuiltinPackRegistry
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory

/**
 * Main entrypoint for the `caraca_customs` Fabric mod.
 *
 * Implements [ModInitializer] to hook into Fabric's lifecycle initialization phase,
 * triggering registration of custom Poké Balls and Berry mechanics.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CaracaCustoms : ModInitializer {

	/**
	 * Unique identifier namespace for the mod.
	 */
	const val MOD_ID: String = "caraca_customs"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	/**
	 * Called by Fabric Loader during mod initialization.
	 */
	override fun onInitialize() {
		DynamicConfig.load()
		BerryGrowthModifier.register()
		BerryHarvestHandler.register()
		BuiltinPackRegistry.register()
		CustomLootInjector.register()
		CustomBallRegistry.initialize()

		CaracaCommand.register()

		LOGGER.info("Custom PokéBalls and Berry System initialized successfully!")
	}

	/**
	 * Utility factory function to construct namespaced [ResourceLocation] instances under [MOD_ID].
	 *
	 * @param path The relative path string within the mod's domain.
	 * @return A fully qualified [ResourceLocation] under the `caraca_customs` namespace.
	 */
	fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}