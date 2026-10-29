package com.caraca

import com.caraca.customs.balls.CustomBallRegistry
import com.caraca.customs.berry.BerryGrowthModifier
import com.caraca.customs.berry.BerryHarvestHandler
import com.caraca.customs.caracatron.Caracatron
import com.caraca.customs.command.CaracaCommand
import com.caraca.customs.config.DynamicConfig
import com.caraca.customs.config.FeatureConditions
import com.caraca.customs.follow.FollowSpeedHandler
import com.caraca.customs.loot.CustomLootInjector
import com.caraca.customs.network.ServerNetworkHandler
import com.caraca.customs.progression.ExpShareAll
import com.caraca.customs.progression.LevelCapHandler
import com.caraca.customs.progression.SharedPokedex
import com.caraca.customs.progression.SpawnGroupRegistry
import com.caraca.customs.progression.SpawnTierTable
import com.caraca.customs.progression.TierSpawningInfluence
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
 * Custom Poké Balls are registered before [DynamicConfig.load] so the configuration file
 * can be populated with an override entry for every registered ball.
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
		CustomBallRegistry.initialize()
		DynamicConfig.load()
		FeatureConditions.register()

		ServerNetworkHandler.register()
		Caracatron.register()
		BerryGrowthModifier.register()
		BerryHarvestHandler.register()
		BuiltinPackRegistry.register()
		CustomLootInjector.register()
		SharedPokedex.register()
		LevelCapHandler.register()
		ExpShareAll.register()
		SpawnGroupRegistry.register()
		SpawnTierTable.register()
		TierSpawningInfluence.register()
		FollowSpeedHandler.register()
		CaracaCommand.register()

		LOGGER.info("Custom PokéBalls, Berry System, player tiers, spawn groups, Exp. Share All and the Caracatron 3000 initialized successfully!")
	}

	/**
	 * Utility factory function to construct namespaced [ResourceLocation] instances under [MOD_ID].
	 *
	 * @param path The relative path string within the mod's domain.
	 * @return A fully qualified [ResourceLocation] under the `caraca_customs` namespace.
	 */
	fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}
