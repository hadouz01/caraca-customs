package com.caraca.customs.client.gui.caracatron

import net.minecraft.ChatFormatting
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity

/**
 * Tooltip of the flame under furnace recipes on the materials page: a short list of fuels, best first.
 *
 * Fuels come from the game's own fuel list (other mods' included). Only the strong ones (from coal up) get a line of
 * their own; logs and planks show up as a group, and sticks close the list.
 *
 * @author hadouz01
 * @since 1.36.0
 */
object CaracatronFuels {

    private const val KEY = "text.caraca_customs.caracatron.materials.fuel"
    private const val BULLET = "• "

    /**
     * Shortest burn time, in ticks, for a fuel to get a line of its own (coal and charcoal burn for 1600).
     */
    private const val MIN_OWN_LINE_BURN = 1600

    private var cached: List<Component>? = null

    /**
     * Gets the tooltip lines: the title in bold, then one fuel per line.
     */
    fun tooltip(): List<Component> = cached ?: build().also { cached = it }

    /**
     * Forgets the list, so it's built again with the current tags.
     */
    fun clear() {
        cached = null
    }

    private fun build(): List<Component> {
        val fuels = AbstractFurnaceBlockEntity.getFuel()
        val entries = mutableListOf<Pair<Int, Component>>()
        fuels.filterValues { it >= MIN_OWN_LINE_BURN }.forEach { (item, burn) -> entries += burn to item.description }
        group(ItemTags.LOGS_THAT_BURN, "logs", fuels)?.let(entries::add)
        group(ItemTags.PLANKS, "planks", fuels)?.let(entries::add)
        fuels[Items.STICK]?.let { entries += it to Items.STICK.description }

        val lines = entries.sortedByDescending { it.first }.map { (_, name) -> Component.literal(BULLET).append(name) }
        return listOf(tr("title").withStyle(ChatFormatting.BOLD)) + lines
    }

    /**
     * A group of fuels shown under one name, burning as long as its best item.
     */
    private fun group(tag: TagKey<Item>, key: String, fuels: Map<Item, Int>): Pair<Int, Component>? {
        val burn = BuiltInRegistries.ITEM.getTag(tag)
            .map { holders -> holders.mapNotNull { fuels[it.value()] }.maxOrNull() }
            .orElse(null) ?: return null
        return burn to tr(key)
    }

    private fun tr(key: String): MutableComponent = Component.translatable("$KEY.$key")
}
