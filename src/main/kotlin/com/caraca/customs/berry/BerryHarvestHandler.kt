package com.caraca.customs.berry

import com.caraca.customs.config.DynamicConfig
import com.caraca.customs.mulch.MulchTracker
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.berry.BerryYieldCalculationEvent
import com.cobblemon.mod.common.api.mulch.MulchVariant
import net.minecraft.core.registries.Registries
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.HoeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments

/**
 * Event listener handling berry harvest yield recalculations, Rich Mulch yield boosts,
 * and Hoe Fortune bonus mechanics.
 *
 * Hooks into Cobblemon's [BerryYieldCalculationEvent] to apply dynamic yield bounds,
 * process Rich Mulch bonuses, and manage mulch consumption upon harvesting.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object BerryHarvestHandler {

    private const val RICH_MULCH_YIELD_BOOST = 3
    private const val MAX_RICH_MULCH_YIELD_CAP = 6

    /**
     * Registers the berry yield subscriber into Cobblemon's event bus.
     */
    fun register() {
        CobblemonEvents.BERRY_YIELD.subscribe { event ->
            handleBerryYield(event)
        }
    }

    /**
     * Evaluates configured base yield ranges, applies Rich Mulch modifiers,
     * appends calculated Hoe Fortune bonus drops, and consumes active mulch usage.
     *
     * @param event Target harvest calculation event context.
     */
    private fun handleBerryYield(event: BerryYieldCalculationEvent) {
        val berryId = event.berry.identifier.toString()
        val config = DynamicConfig.getConfigFor(berryId)

        val mulchTracker = event.world.getBlockEntity(event.pos) as? MulchTracker
        val isRichMulchActive = mulchTracker?.mulch == MulchVariant.RICH

        val baseYield = calculateBaseYield(config.minYield, config.maxYield, isRichMulchActive)
        val fortuneBonus = calculateHoeFortuneBonus(event.placer, event.world.random)

        val calculatedYield = baseYield + fortuneBonus
        val maxAllowedYield = (if (isRichMulchActive) MAX_RICH_MULCH_YIELD_CAP else config.maxYield) + 10

        event.yield = calculatedYield.coerceAtMost(maxAllowedYield)

        mulchTracker?.`caraca$consumeMulchHarvest`()
    }

    /**
     * Calculates base berry drop quantity considering Rich Mulch boost and cap limits.
     *
     * @param minConfig Configured minimum yield.
     * @param maxConfig Configured maximum yield.
     * @param isRichMulch Indicates whether active mulch is [MulchVariant.RICH].
     * @return Calculated base yield quantity before fortune modifiers.
     */
    private fun calculateBaseYield(minConfig: Int, maxConfig: Int, isRichMulch: Boolean): Int {
        var minYield = minConfig
        var maxYield = maxConfig

        if (isRichMulch) {
            minYield = (minYield + RICH_MULCH_YIELD_BOOST).coerceAtMost(MAX_RICH_MULCH_YIELD_CAP)
            maxYield = (maxYield + RICH_MULCH_YIELD_BOOST).coerceAtMost(MAX_RICH_MULCH_YIELD_CAP)
        }

        return if (minYield <= maxYield) {
            (minYield..maxYield).random()
        } else {
            minYield
        }
    }

    /**
     * Computes extra berry yield based on the player's active Hoe Fortune enchantment level.
     *
     * @param placer The entity performing the harvest interaction.
     * @param random World random instance used to calculate chance checks.
     * @return Calculated extra drop quantity based on Fortune chance rolls.
     */
    private fun calculateHoeFortuneBonus(placer: LivingEntity?, random: net.minecraft.util.RandomSource): Int {
        if (placer == null) return 0

        val hoeStack = resolveHeldHoe(placer) ?: return 0
        val fortuneLevel = resolveFortuneLevel(placer, hoeStack)

        if (fortuneLevel <= 0) return 0

        var extraDrops = 0
        val chancePerLevel = DynamicConfig.data.globalHoeFortuneChance

        for (i in 0 until fortuneLevel) {
            if (random.nextFloat() < chancePerLevel) {
                extraDrops++
            }
        }

        return extraDrops
    }

    /**
     * Finds and returns a held [HoeItem] stack from main hand or offhand.
     *
     * @param entity Target entity holding the item.
     * @return Matching [ItemStack] containing a hoe, or `null` if none held.
     */
    private fun resolveHeldHoe(entity: LivingEntity): ItemStack? {
        val mainHand = entity.getItemInHand(InteractionHand.MAIN_HAND)
        if (mainHand.item is HoeItem) return mainHand

        val offHand = entity.getItemInHand(InteractionHand.OFF_HAND)
        if (offHand.item is HoeItem) return offHand

        return null
    }

    /**
     * Queries the level of the Fortune enchantment on a specific item stack.
     *
     * @param entity Entity providing world registry access context.
     * @param stack Target item stack evaluated for enchantment level.
     * @return Level integer of Fortune applied, or 0 if absent.
     */
    private fun resolveFortuneLevel(entity: LivingEntity, stack: ItemStack): Int {
        val registryAccess = entity.level().registryAccess()
        val fortuneEnchantment = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE)
        return EnchantmentHelper.getItemEnchantmentLevel(fortuneEnchantment, stack)
    }
}