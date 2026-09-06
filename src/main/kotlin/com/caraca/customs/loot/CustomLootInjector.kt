package com.caraca.customs.loot

import com.caraca.customs.balls.CustomBallRegistry
import com.caraca.customs.balls.modules.BallTier
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue

/**
 * Injects custom Poké Balls into vanilla structure chest loot tables based on [BallTier].
 *
 * Chests are grouped into access tiers; higher tiers also receive every lower-tier pool:
 * - [BallTier.REGULAR]: villages, simple dungeons, mansions, temples, pyramids, outposts and every higher tier.
 * - [BallTier.ULTRA]: mineshafts, strongholds and every higher tier.
 * - [BallTier.CARACA]: Nether fortresses, End cities and ancient cities.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CustomLootInjector {

    private val CARACA_TIER_CHESTS = setOf(
        "chests/nether_bridge",
        "chests/end_city_treasure",
        "chests/ancient_city",
        "chests/ancient_city_ice_box"
    )

    private val ULTRA_TIER_CHESTS = CARACA_TIER_CHESTS + setOf(
        "chests/abandoned_mineshaft",
        "chests/stronghold_corridor",
        "chests/stronghold_crossing",
        "chests/stronghold_library"
    )

    private val REGULAR_TIER_CHESTS = ULTRA_TIER_CHESTS + setOf(
        "chests/simple_dungeon",
        "chests/desert_pyramid",
        "chests/jungle_temple",
        "chests/woodland_mansion",
        "chests/pillager_outpost"
    )

    private const val VILLAGE_CHEST_PREFIX = "chests/village/"

    /**
     * Pool layout of a single ball tier.
     *
     * @property rolls Number of rolls of the pool.
     * @property ballWeight Weight of each ball of the tier.
     * @property emptyWeight Weight of the empty entry.
     */
    private data class PoolSpec(val rolls: Float, val ballWeight: Int, val emptyWeight: Int)

    private val POOL_SPECS = mapOf(
        BallTier.REGULAR to PoolSpec(rolls = 3.0f, ballWeight = 3, emptyWeight = 3),
        BallTier.ULTRA to PoolSpec(rolls = 5.0f, ballWeight = 3, emptyWeight = 6),
        BallTier.CARACA to PoolSpec(rolls = 5.0f, ballWeight = 1, emptyWeight = 3)
    )

    /**
     * Registers the Fabric [LootTableEvents.MODIFY] listener appending tier pools to built-in chest tables.
     */
    fun register() {
        LootTableEvents.MODIFY.register { key, tableBuilder, source, _ ->
            if (!source.isBuiltin) return@register

            val path = key.location().path

            if (path.startsWith(VILLAGE_CHEST_PREFIX) || path in REGULAR_TIER_CHESTS) {
                tableBuilder.withPool(buildPool(BallTier.REGULAR))
            }
            if (path in ULTRA_TIER_CHESTS) {
                tableBuilder.withPool(buildPool(BallTier.ULTRA))
            }
            if (path in CARACA_TIER_CHESTS) {
                tableBuilder.withPool(buildPool(BallTier.CARACA))
            }
        }
    }

    private fun buildPool(tier: BallTier): LootPool.Builder {
        val spec = POOL_SPECS.getValue(tier)
        val pool = LootPool.lootPool().setRolls(ConstantValue.exactly(spec.rolls))

        CustomBallRegistry.getRegisteredBalls()
            .filter { it.props.tier == tier }
            .forEach { pool.add(LootItem.lootTableItem(it.item).setWeight(spec.ballWeight)) }

        pool.add(EmptyLootItem.emptyItem().setWeight(spec.emptyWeight))
        return pool
    }
}
