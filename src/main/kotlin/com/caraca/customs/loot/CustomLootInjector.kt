package com.caraca.customs.loot

import com.caraca.CaracaCustoms
import com.caraca.customs.balls.customs.CustomBalls
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue

/**
 * Handles the dynamic injection of custom Poké Balls into Vanilla Minecraft loot tables based on specific tier rules.
 *
 * This object registers an event listener to dynamically append custom loot pools into world generation chests.
 * It categorizes chests into access tiers (Regular, Ultra, Caraca) and dynamically filters registered custom balls
 * to populate these specific pools with precise spawn weights and empty-drop chances.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CustomLootInjector {

    /**
     * Exact paths for chests that allow the highest tier of loot (Caraca Balls).
     * Includes Nether, End, and Deep Dark structures.
     */
    private val CARACA_TIER_CHESTS = setOf(
        "chests/nether_bridge",
        "chests/end_city_treasure",
        "chests/ancient_city",
        "chests/ancient_city_ice_box"
    )

    /**
     * Exact paths for chests that allow the intermediate tier of loot (Ultra Balls).
     * Includes all [CARACA_TIER_CHESTS] plus Mineshafts and Strongholds.
     */
    private val ULTRA_TIER_CHESTS = CARACA_TIER_CHESTS + setOf(
        "chests/abandoned_mineshaft",
        "chests/stronghold_corridor",
        "chests/stronghold_crossing",
        "chests/stronghold_library"
    )

    /**
     * Exact paths for chests that allow the base tier of loot (Regular Balls).
     * Includes all [ULTRA_TIER_CHESTS] plus Simple Dungeons, Mansions, Temples, Pyramids, and Outposts.
     * Note: Village chests are handled separately via a path prefix check.
     */
    private val REGULAR_TIER_CHESTS = ULTRA_TIER_CHESTS + setOf(
        "chests/simple_dungeon",
        "chests/desert_pyramid",
        "chests/jungle_temple",
        "chests/woodland_mansion",
        "chests/pillager_outpost"
    )

    /**
     * Registers the Fabric LootTableEvents listener.
     *
     * Evaluates incoming Vanilla/Built-in loot tables against predefined architectural tiers.
     * Appends specific independent [LootPool] instances depending on the structure's permissions,
     * maintaining low cognitive complexity by avoiding nested iterations.
     */
    fun register() {
        LootTableEvents.MODIFY.register { key, tableBuilder, source, _ ->
            if (!source.isBuiltin) return@register

            val path = key.location().path
            val isVillage = path.startsWith("chests/village/")

            if (isVillage || REGULAR_TIER_CHESTS.contains(path)) {
                tableBuilder.withPool(buildRegularPool())
            }

            if (ULTRA_TIER_CHESTS.contains(path)) {
                tableBuilder.withPool(buildUltraPool())
            }

            if (CARACA_TIER_CHESTS.contains(path)) {
                tableBuilder.withPool(buildCaracaPool())
            }
        }
    }

    /**
     * Constructs a [LootPool] containing base-tier Regular Poké Balls.
     *
     * - **Rolls:** 3
     * - **Ball Weight:** 3 per ball type
     * - **Empty Weight:** 3
     *
     * Filters out any ball containing "ultra" or "caraca" in its identifier.
     *
     * @return A configured [LootPool.Builder] for regular balls.
     */
    private fun buildRegularPool(): LootPool.Builder {
        val pool = LootPool.lootPool().setRolls(ConstantValue.exactly(3.0f))

        CustomBalls.ALL
            .map { it.nameId }
            .filter { !it.contains("ultra") && !it.contains("caraca") }
            .forEach { nameId ->
                pool.add(getLootItem(nameId).setWeight(3))
            }

        pool.add(EmptyLootItem.emptyItem().setWeight(3))
        return pool
    }

    /**
     * Constructs a [LootPool] containing mid-tier Ultra Poké Balls.
     *
     * - **Rolls:** 5
     * - **Ball Weight:** 3 per ball type (Scaled up from 1.5 to maintain Int integrity)
     * - **Empty Weight:** 6 (Scaled up from 3)
     *
     * Selects only balls containing "ultra" in their identifier.
     *
     * @return A configured [LootPool.Builder] for ultra balls.
     */
    private fun buildUltraPool(): LootPool.Builder {
        val pool = LootPool.lootPool().setRolls(ConstantValue.exactly(5.0f))

        CustomBalls.ALL
            .map { it.nameId }
            .filter { it.contains("ultra") }
            .forEach { nameId ->
                pool.add(getLootItem(nameId).setWeight(3))
            }

        pool.add(EmptyLootItem.emptyItem().setWeight(6))
        return pool
    }

    /**
     * Constructs a [LootPool] containing highest-tier Caraca Poké Balls.
     *
     * - **Rolls:** 5
     * - **Ball Weight:** 1 per ball type
     * - **Empty Weight:** 3
     *
     * Selects strictly "caraca_ball" and "ancient_caraca_ball".
     *
     * @return A configured [LootPool.Builder] for caraca balls.
     */
    private fun buildCaracaPool(): LootPool.Builder {
        val pool = LootPool.lootPool().setRolls(ConstantValue.exactly(5.0f))

        CustomBalls.ALL
            .map { it.nameId }
            .filter { it == "caraca_ball" || it == "ancient_caraca_ball" }
            .forEach { nameId ->
                pool.add(getLootItem(nameId).setWeight(1))
            }

        pool.add(EmptyLootItem.emptyItem().setWeight(3))
        return pool
    }

    /**
     * Resolves the [net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer.Builder] for a specific item identifier.
     *
     * Extracts the item directly from the core registry to ensure safe initialization.
     *
     * @param nameId The snake_case identifier of the custom Poké Ball.
     * @return The item configured for injection.
     */
    private fun getLootItem(nameId: String): LootPoolSingletonContainer.Builder<*> {
        val resourceLocation = ResourceLocation.fromNamespaceAndPath(CaracaCustoms.MOD_ID, nameId)
        val item = BuiltInRegistries.ITEM.get(resourceLocation)
        return LootItem.lootTableItem(item)
    }
}