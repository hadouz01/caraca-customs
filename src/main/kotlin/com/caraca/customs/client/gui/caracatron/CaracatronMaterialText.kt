package com.caraca.customs.client.gui.caracatron

import com.caraca.customs.network.CaracatronMaterialBlock
import com.caraca.customs.network.CaracatronMaterialInfoPayload
import com.caraca.customs.network.CaracatronMaterialLoot
import com.caraca.customs.network.CaracatronMaterialSpot
import com.caraca.customs.network.CaracatronMaterialStructure
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.Items

/**
 * Turns where an item comes from into the lines of the Caracatron's materials page, split in two tabs like the spawn
 * rarities:
 *
 * - [WORLD]: every generated block that is the item, or drops it when mined, with its icon, then each place it generates
 *   in naturally (dimension, biomes, and for ores the altitude, how often and how big the veins are), followed by where
 *   a berry grows, its mutations, a recipe making the item and how many Pokémon drop it, which links to the drops page.
 * - [STRUCTURES]: the same blocks with every structure they're part of, then the suspicious blocks dug up with a brush
 *   and the chests holding the item, grouped by the structure or feature placing them, with its biomes.
 *
 * Each line's label (the dimension, "Altitude:", the structure...) is bold and in the screen color's label color
 * ([CaracatronTextColors.label]), apart from the section titles. Biome groups carry their tag as style insertion
 * ([CaracatronSpawnText.biomeName]), so hovering them lists their biomes, like on the spawn tabs.
 *
 * @author hadouz01
 * @since 1.27.0
 */
object CaracatronMaterialText {

    /**
     * Tab of natural generation: biomes, altitude, frequency, plus recipes, berries and Pokémon drops.
     */
    const val WORLD = "world"

    /**
     * Tab of structures: blocks they're built with, brushing and chests.
     */
    const val STRUCTURES = "structures"

    /**
     * Tabs in the order of their icons.
     */
    val TABS = listOf(WORLD, STRUCTURES)

    /**
     * Style insertion of the Pokémon drops line, followed by the item id: clicking it opens the drops page searching
     * that item.
     */
    const val DROPS_LINK = "drops:"

    private const val KEY = "text.caraca_customs.caracatron.materials"
    private const val INDENT = "  "

    /**
     * Whether a tab has anything to show for an item.
     *
     * @param info Where the item comes from.
     * @param tab [WORLD] or [STRUCTURES].
     * @param droppingSpecies How many listed Pokémon drop the item.
     */
    fun hasContent(info: CaracatronMaterialInfoPayload, tab: String, droppingSpecies: Int): Boolean {
        if (tab == STRUCTURES) return info.blocks.any { it.structures.isNotEmpty() } || info.loot.isNotEmpty()
        return info.blocks.any { it.spots.isNotEmpty() } || sourceLines(info, 0).isNotEmpty() || droppingSpecies > 0
    }

    /**
     * Builds and wraps the lines of a tab.
     *
     * @param info Where the item comes from, sent by the server.
     * @param tab [WORLD] or [STRUCTURES].
     * @param droppingSpecies How many listed Pokémon drop the item.
     * @param labelColor RGB color of the labels and the drops page link ([CaracatronTextColors.label]).
     * @param font Font used to wrap the lines.
     * @param width Wrapping width, in font units.
     * @return The wrapped lines, blank ones included as spacing.
     */
    fun lines(
        info: CaracatronMaterialInfoPayload,
        tab: String,
        droppingSpecies: Int,
        labelColor: Int,
        font: Font,
        width: Int
    ): List<CaracatronInfoLine> {
        val lines = mutableListOf<CaracatronInfoLine>()

        fun add(component: Component, color: Int = CaracatronSpawnText.TEXT_COLOR) {
            font.split(component, width).forEach { lines += CaracatronInfoLine(it, color) }
        }

        fun space() {
            lines += CaracatronInfoLine(FormattedCharSequence.EMPTY, CaracatronSpawnText.TEXT_COLOR)
        }

        fun title(key: String) = add(tr(key).withStyle(ChatFormatting.BOLD), CaracatronSpawnText.TITLE_COLOR)

        if (tab == STRUCTURES) {
            info.blocks.filter { it.structures.isNotEmpty() }.forEach { block ->
                blockTitle(block, if (block.dropsItem) "block.drops_structures" else "block.structures", font, width, lines)
                block.structures.forEach { structure -> structureLines(structure, labelColor).forEach(::add) }
                space()
            }

            listOf(CaracatronMaterialLoot.BRUSH to "loot.brush", CaracatronMaterialLoot.CHEST to "loot.chest").forEach { (kind, key) ->
                val loot = info.loot.filter { it.kind == kind }
                if (loot.isEmpty()) return@forEach
                title(key)
                lootGroups(loot).forEach { group -> lootLines(group, kind == CaracatronMaterialLoot.BRUSH, labelColor).forEach(::add) }
                val unplaced = loot.filter { it.place == null }.map { Component.literal(CaracatronSpawnText.readable(it.table.path)) }.distinctBy { it.string }
                if (unplaced.isNotEmpty()) {
                    add(Component.literal("- ").append(label(tr("label.other"), labelColor)).append(" ").append(CaracatronSpawnText.join(unplaced)))
                }
                space()
            }

            if (lines.isEmpty()) add(tr("none_structures"))
            return lines
        }

        info.blocks.filter { it.spots.isNotEmpty() }.forEach { block ->
            blockTitle(block, if (block.dropsItem) "block.drops" else "block.self", font, width, lines)
            block.spots.forEach { spot -> spotLines(spot, labelColor).forEach(::add) }
            space()
        }

        val source = sourceLines(info, labelColor)
        if (source.isNotEmpty()) {
            title("other")
            source.forEach(::add)
            space()
        }

        if (droppingSpecies > 0) {
            add(tr("pokemon", droppingSpecies).withStyle { it.withUnderlined(true).withColor(labelColor).withInsertion("$DROPS_LINK${info.item}") })
            space()
        }

        if (lines.isEmpty()) add(tr("none"))
        return lines
    }

    /**
     * Adds a block's title: its icon spanning two lines, its name in bold, and a subtitle saying whether it's the item
     * itself or drops it.
     */
    private fun blockTitle(block: CaracatronMaterialBlock, subtitleKey: String, font: Font, width: Int, lines: MutableList<CaracatronInfoLine>) {
        val blockType = BuiltInRegistries.BLOCK.get(block.block)
        val icon = blockType.asItem().takeIf { it != Items.AIR }?.let { BuiltInRegistries.ITEM.getKey(it) }
        val textWidth = width - CaracatronInfoLine.ICON_INDENT

        val title = font.split(blockType.name.copy().withStyle(ChatFormatting.BOLD), textWidth)
        val subtitle = font.split(tr(subtitleKey), textWidth)
        title.forEachIndexed { index, text ->
            lines += CaracatronInfoLine(text, CaracatronSpawnText.TITLE_COLOR, if (index == 0) icon else null, indented = true)
        }
        subtitle.forEach { lines += CaracatronInfoLine(it, CaracatronSpawnText.TEXT_COLOR, indented = true) }
    }

    /**
     * Describes a spot: dimension and biomes, how it generates, its altitude and how often, each after its label.
     */
    private fun spotLines(spot: CaracatronMaterialSpot, labelColor: Int): List<Component> {
        val lines = mutableListOf<Component>()
        lines += Component.literal("- ").append(placeLine(spot.dimension, spot.biomes, labelColor))

        val kind = when {
            spot.kind == CaracatronMaterialSpot.TERRAIN -> tr("kind.terrain")
            spot.kind == CaracatronMaterialSpot.VEIN -> tr("kind.vein")
            spot.surface.isNotEmpty() -> tr("kind.${spot.surface}")
            else -> null
        }
        kind?.let { lines += labeled("label.kind", it, labelColor) }
        altitude(spot)?.let { lines += labeled("label.altitude", it, labelColor) }
        if (spot.perChunk.isNotEmpty()) lines += labeled("label.per_chunk", Component.literal(spot.perChunk), labelColor)
        if (spot.rarity > 1) lines += labeled("label.rarity", tr("rarity", spot.rarity), labelColor)
        if (spot.size > 0) lines += labeled("label.size", tr("size", spot.size), labelColor)
        return lines
    }

    private fun altitude(spot: CaracatronMaterialSpot): Component? {
        val min = spot.minY
        val max = spot.maxY
        val range = when {
            min != null && max != null && min == max -> tr("altitude.exact", min)
            min != null && max != null -> tr("altitude.range", min, max)
            min != null -> tr("altitude.min", min)
            max != null -> tr("altitude.max", max)
            else -> return null
        }
        val peakMin = spot.peakMin
        val peakMax = spot.peakMax
        when {
            peakMin != null && peakMax != null && peakMin == peakMax -> range.append(tr("altitude.peak", peakMin))
            peakMin != null && peakMax != null -> range.append(tr("altitude.peak_range", peakMin, peakMax))
            spot.biasedLow -> range.append(tr("altitude.low"))
        }
        return range
    }

    /**
     * Describes a structure a block is part of: its name as a label, then its dimension and biomes.
     */
    private fun structureLines(structure: CaracatronMaterialStructure, labelColor: Int): List<Component> {
        val lines = mutableListOf<Component>(Component.literal("- ").append(label(placeName(structure.structure), labelColor, colon = false)))
        structure.dimension?.let { lines += indented(placeLine(it, structure.biomes, labelColor)) }
        return lines
    }

    /**
     * Loot of one structure or feature in one dimension.
     */
    private class LootGroup(val place: ResourceLocation, val dimension: ResourceLocation?, val biomes: List<String>, val entries: List<CaracatronMaterialLoot>)

    private fun lootGroups(loot: List<CaracatronMaterialLoot>): List<LootGroup> {
        return loot.groupBy { Triple(it.place, it.dimension, it.biomes) }
            .mapNotNull { (key, entries) -> key.first?.let { LootGroup(it, key.second, key.third, entries) } }
    }

    /**
     * Describes the loot of a structure or feature: its name as a label followed by the blocks holding the item
     * (brushing) or the chests holding it, then its dimension and biomes.
     */
    private fun lootLines(group: LootGroup, brushed: Boolean, labelColor: Int): List<Component> {
        val holders = if (brushed) {
            group.entries.map { entry -> entry.block?.let { BuiltInRegistries.BLOCK.get(it).name } ?: tr("loot.suspicious") }
        } else {
            group.entries.map { Component.literal(CaracatronSpawnText.readable(it.table.path)) }
        }.distinctBy { it.string }

        val lines = mutableListOf<Component>(
            Component.literal("- ").append(label(placeName(group.place), labelColor)).append(" ").append(CaracatronSpawnText.join(holders))
        )
        group.dimension?.let { lines += indented(placeLine(it, group.biomes, labelColor)) }
        return lines
    }

    /**
     * Describes the recipe and berry source of the item: where it grows naturally, its mutations and a recipe making it,
     * with their headings as labels.
     */
    private fun sourceLines(info: CaracatronMaterialInfoPayload, labelColor: Int): List<Component> {
        val source = info.source
        val lines = mutableListOf<Component>()
        when {
            source.growsEverywhere -> lines += Component.translatable("text.caraca_customs.caracatron.source.grows_everywhere")
            source.biomes.isNotEmpty() -> lines += label(Component.translatable("text.caraca_customs.caracatron.source.grows_in"), labelColor, colon = false)
                .append(" ")
                .append(CaracatronSpawnText.join(source.biomes.map { CaracatronSpawnText.biomeName(it.toString()) }))
        }
        if (source.mutations.isNotEmpty()) {
            lines += label(Component.translatable("text.caraca_customs.caracatron.source.mutations"), labelColor, colon = false)
            source.mutations.forEach { (first, second) ->
                lines += indented(Component.translatable("text.caraca_customs.caracatron.source.mutation", itemName(first), itemName(second)))
            }
        }
        if (source.recipe.isNotEmpty()) {
            val station = CaracatronItemSourceText.stationName(source.recipeType)
            lines += label(Component.translatable("text.caraca_customs.caracatron.source.recipe", station), labelColor, colon = false)
            source.recipe.forEach {
                lines += indented(Component.translatable("text.caraca_customs.caracatron.snack.ingredient", it.count, itemName(it.item)))
            }
        }
        return lines
    }

    /**
     * Writes a dimension as a label followed by its biomes, e.g. "**Overworld:** Forest, Plains", or "**Overworld:**
     * every biome".
     */
    private fun placeLine(dimension: ResourceLocation, biomes: List<String>, labelColor: Int): Component {
        val value = if (biomes.isEmpty()) tr("spot.every_biome") else CaracatronSpawnText.join(biomes.map(CaracatronSpawnText::biomeName))
        return label(dimensionName(dimension), labelColor).append(" ").append(value)
    }

    /**
     * Writes an indented line made of a label and its value, e.g. "**Altitude:** Y -64 to 72".
     */
    private fun labeled(labelKey: String, value: Component, labelColor: Int): Component {
        return indented(label(tr(labelKey), labelColor).append(" ").append(value))
    }

    /**
     * Styles a line's label: bold, in the label color and, unless told otherwise, followed by a colon.
     *
     * @param text Label text.
     * @param color RGB label color.
     * @param colon Whether a colon is added after the label.
     */
    private fun label(text: Component, color: Int, colon: Boolean = true): MutableComponent {
        val label = if (colon) tr("label", text) else Component.empty().append(text)
        return Component.empty().append(label.withStyle { it.withBold(true).withColor(color) })
    }

    /**
     * Names a structure or feature, e.g. "Village Plains", from its identifier.
     */
    private fun placeName(id: ResourceLocation): Component {
        return Component.translatableWithFallback("$KEY.structure.${id.namespace}.${id.path.replace('/', '.')}", CaracatronSpawnText.readable(id.path))
    }

    /**
     * Names a dimension, e.g. "Overworld", or a readable version of its identifier when there's no translation.
     */
    private fun dimensionName(id: ResourceLocation): Component {
        return Component.translatableWithFallback("$KEY.dimension.${id.namespace}.${id.path.replace('/', '.')}", CaracatronSpawnText.readable(id.path))
    }

    private fun itemName(id: ResourceLocation): Component = BuiltInRegistries.ITEM.get(id).description

    private fun indented(line: Component): Component = Component.literal(INDENT).append(line)

    private fun tr(key: String, vararg args: Any): MutableComponent = Component.translatable("$KEY.$key", *args)
}
