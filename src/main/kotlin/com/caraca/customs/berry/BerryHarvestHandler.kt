package com.caraca.customs.berry

import com.caraca.customs.config.BerryConfigEntry
import com.caraca.customs.config.DynamicConfig
import com.caraca.customs.mulch.MulchTracker
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.berry.BerryHarvestEvent
import com.cobblemon.mod.common.api.events.berry.BerryYieldCalculationEvent
import com.cobblemon.mod.common.api.mulch.MulchVariant
import com.cobblemon.mod.common.block.entity.BerryBlockEntity
import net.minecraft.core.registries.Registries
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.HoeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments

/**
 * Event listener handling berry yield recalculations, Rich Mulch yield boosts,
 * mulch consumption and Hoe Fortune bonus drops.
 *
 * - [BerryYieldCalculationEvent]: applies configured yield bounds and Rich Mulch, then consumes one mulch use.
 * - [BerryHarvestEvent]: appends Hoe Fortune bonus drops, since it is the only event that carries the harvesting player.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object BerryHarvestHandler {

    private const val RICH_MULCH_YIELD_BOOST = 3

    /**
     * Registers the berry yield and harvest subscribers into Cobblemon's event bus.
     */
    fun register() {
        CobblemonEvents.BERRY_YIELD.subscribe { event -> handleBerryYield(event) }
        CobblemonEvents.BERRY_HARVEST.subscribe { event -> handleBerryHarvest(event) }
    }

    /**
     * Replaces the native yield with the configured bounds, applies Rich Mulch and consumes one mulch use.
     *
     * @param event Target yield calculation event context.
     */
    private fun handleBerryYield(event: BerryYieldCalculationEvent) {
        val config = DynamicConfig.getConfigFor(event.berry.identifier.toString())
        val blockEntity = event.world.getBlockEntity(event.pos)
        val isRichMulchActive = (blockEntity as? BerryBlockEntity)?.mulchVariant == MulchVariant.RICH

        event.yield = calculateBaseYield(config, isRichMulchActive)

        (blockEntity as? MulchTracker)?.`caraca$consumeMulchHarvest`()
    }

    /**
     * Appends extra berries to the harvest drops based on the player's held Fortune hoe.
     *
     * @param event Target harvest event context.
     */
    private fun handleBerryHarvest(event: BerryHarvestEvent) {
        val berryItem = event.berry.item() ?: return
        val bonus = calculateHoeFortuneBonus(event.player, event.world.random)
        if (bonus > 0) {
            event.drops += ItemStack(berryItem, bonus)
        }
    }

    /**
     * Calculates the base berry quantity using the configured bounds, boosted by Rich Mulch
     * and capped at [DynamicConfig.MAX_YIELD_CAP].
     *
     * @param config Resolved berry configuration.
     * @param isRichMulch Indicates whether the active mulch is [MulchVariant.RICH].
     * @return Calculated yield quantity.
     */
    private fun calculateBaseYield(config: BerryConfigEntry, isRichMulch: Boolean): Int {
        val boost = if (isRichMulch) RICH_MULCH_YIELD_BOOST else 0
        val minYield = (config.minYield + boost).coerceAtMost(DynamicConfig.MAX_YIELD_CAP)
        val maxYield = (config.maxYield + boost).coerceAtMost(DynamicConfig.MAX_YIELD_CAP)

        return if (minYield <= maxYield) (minYield..maxYield).random() else minYield
    }

    /**
     * Rolls one chance per Fortune level of the held hoe to add an extra berry.
     *
     * @param entity The entity performing the harvest.
     * @param random World random instance used for chance rolls.
     * @return Number of extra berries to drop.
     */
    private fun calculateHoeFortuneBonus(entity: LivingEntity, random: RandomSource): Int {
        val hoeStack = resolveHeldHoe(entity) ?: return 0
        val fortuneLevel = resolveFortuneLevel(entity, hoeStack)
        val chancePerLevel = DynamicConfig.data.globalHoeFortuneChance

        return (0 until fortuneLevel).count { random.nextFloat() < chancePerLevel }
    }

    /**
     * Finds a held [HoeItem] stack, checking the main hand first and the offhand second.
     *
     * @param entity Target entity holding the item.
     * @return Matching hoe stack, or `null` if none is held.
     */
    private fun resolveHeldHoe(entity: LivingEntity): ItemStack? {
        return InteractionHand.entries
            .map(entity::getItemInHand)
            .firstOrNull { it.item is HoeItem }
    }

    /**
     * Queries the Fortune enchantment level of an item stack.
     *
     * @param entity Entity providing registry access.
     * @param stack Target item stack.
     * @return Fortune level, or 0 if absent.
     */
    private fun resolveFortuneLevel(entity: LivingEntity, stack: ItemStack): Int {
        val fortune = entity.level().registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.FORTUNE)
        return EnchantmentHelper.getItemEnchantmentLevel(fortune, stack)
    }
}
