package com.caraca

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

/**
 * Entrypoint for Fabric Data Generation API within `caraca_customs`.
 *
 * Implements [DataGeneratorEntrypoint] to allow automated generation of data assets
 * such as loot tables, recipes, tags, and language providers at build time.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CaracaCustomsDataGenerator : DataGeneratorEntrypoint {

	/**
	 * Initializes data generator providers.
	 *
	 * @param fabricDataGenerator The [FabricDataGenerator] instance used to register asset providers.
	 */
	override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
	}
}