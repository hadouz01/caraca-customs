package com.caraca.customs.client.gui.caracatron

import com.caraca.customs.network.CaracatronMaterialBlock
import com.caraca.customs.network.CaracatronMaterialInfoPayload
import com.caraca.customs.network.CaracatronMaterialLoot
import com.caraca.customs.network.CaracatronMaterialSpot
import com.caraca.customs.network.CaracatronMaterialStructure
import com.caraca.customs.network.CaracatronRecipe
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.Items

/**
 * Lines of the materials page, in four tabs: [WORLD] (natural generation, growth stages, berries and Pokémon drops),
 * [STRUCTURES] (structures and chests), [RECIPES] and [DIGGING] (brushing).
 *
 * Line labels are bold in [CaracatronTextColors.label], and biome groups keep their tag as style insertion so they can
 * be hovered. Biome lists show [MAX_BIOMES] biomes, the rest goes in the tooltip of their last line.
 *
 * @author hadouz01
 * @since 1.27.0
 */
object CaracatronMaterialText {

    /**
     * Tab of natural generation: growth stages, biomes, altitude, frequency, plus berries and Pokémon drops.
     */
    const val WORLD = "world"

    /**
     * Tab of structures: blocks they're built with and their chests.
     */
    const val STRUCTURES = "structures"

    /**
     * Tab of every recipe making the item.
     */
    const val RECIPES = "recipes"

    /**
     * Tab of what's dug up with a brush.
     */
    const val DIGGING = "digging"

    /**
     * Tabs in the order of their icons.
     */
    val TABS = listOf(WORLD, STRUCTURES, RECIPES, DIGGING)

    /**
     * Style insertion of the Pokémon drops line, followed by the item id: clicking it opens the drops page searching
     * that item.
     */
    const val DROPS_LINK = "drops:"

    /**
     * Style insertion of the last line of a cut biome list, followed by the biomes left out, split by [BIOME_SEPARATOR].
     */
    const val MORE_BIOMES = "biomes:"

    /**
     * Separator of the biomes in a [MORE_BIOMES] insertion.
     */
    const val BIOME_SEPARATOR = ";"

    private const val KEY = "text.caraca_customs.caracatron.materials"
    private const val BULLET = "• "
    private const val SUB_BULLET = "  ◦ "
    private const val MAX_BIOMES = 6

    /**
     * Recipe types merged when they share their ingredients.
     */
    private val COOKING_TYPES = setOf("minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking")

    /**
     * Recipe types that burn fuel, shown with a flame under their ingredient.
     */
    private val FUEL_TYPES = setOf("minecraft:smelting", "minecraft:blasting", "minecraft:smoking")
    private val OVERWORLD = ResourceLocation.withDefaultNamespace("overworld")
    private val BRUSH = BuiltInRegistries.ITEM.getKey(Items.BRUSH)

    /**
     * Whether a tab has anything to show for an item.
     *
     * @param info Where the item comes from.
     * @param tab One of [TABS].
     * @param droppingSpecies How many listed Pokémon drop the item.
     * @param stages Growth stages of the item, `null` when it isn't a plant.
     */
    fun hasContent(info: CaracatronMaterialInfoPayload, tab: String, droppingSpecies: Int, stages: CaracatronGrowthStages?): Boolean = when (tab) {
        STRUCTURES -> info.blocks.any { it.structures.isNotEmpty() } || info.loot.any { it.kind == CaracatronMaterialLoot.CHEST }
        RECIPES -> info.source.recipes.isNotEmpty()
        DIGGING -> info.loot.any { it.kind == CaracatronMaterialLoot.BRUSH }
        else -> info.blocks.any { it.spots.isNotEmpty() } || sourceLines(info, 0).isNotEmpty() || droppingSpecies > 0 || stages != null
    }

    /**
     * Builds and wraps the lines of a tab.
     *
     * @param info Where the item comes from, sent by the server.
     * @param tab One of [TABS].
     * @param droppingSpecies How many listed Pokémon drop the item.
     * @param stages Growth stages of the item, `null` when it isn't a plant.
     * @param labelColor RGB color of the labels ([CaracatronTextColors.label]).
     * @param linkColor RGB color of the drops page link, brighter while it's hovered.
     * @param font Font used to wrap the lines.
     * @param width Wrapping width, in font units.
     * @return The wrapped lines, blank ones included as spacing.
     */
    fun lines(
        info: CaracatronMaterialInfoPayload,
        tab: String,
        droppingSpecies: Int,
        stages: CaracatronGrowthStages?,
        labelColor: Int,
        linkColor: Int,
        font: Font,
        width: Int
    ): List<CaracatronInfoLine> {
        val out = CaracatronInfoLines(font, width)
        when (tab) {
            STRUCTURES -> structureTab(info, labelColor, font, width, out)
            RECIPES -> recipeTab(info, labelColor, out)
            DIGGING -> diggingTab(info, labelColor, font, width, out)
            else -> worldTab(info, droppingSpecies, stages, labelColor, linkColor, font, width, out)
        }
        return out.lines
    }

    private fun worldTab(
        info: CaracatronMaterialInfoPayload,
        droppingSpecies: Int,
        stages: CaracatronGrowthStages?,
        labelColor: Int,
        linkColor: Int,
        font: Font,
        width: Int,
        out: CaracatronInfoLines
    ) {
        if (stages != null) {
            title(out, "stages")
            out.stages(stages)
            out.space()
        }

        if (droppingSpecies > 0) {
            val link = "$DROPS_LINK${info.item}"
            val text = tr("pokemon", droppingSpecies).withStyle { it.withBold(true).withUnderlined(true).withColor(linkColor).withInsertion(link) }
            out.add(text, centered = true)
            out.space()
        }

        info.blocks.filter { it.spots.isNotEmpty() }.forEach { block ->
            blockTitle(block, if (block.dropsItem) "block.drops" else "block.self", font, width, out.lines)
            block.spots.forEach { spot ->
                out.space()
                spotLines(spot, labelColor).forEach(out::add)
            }
            out.space()
        }

        val source = sourceLines(info, labelColor)
        if (source.isNotEmpty()) {
            title(out, "other")
            source.forEach(out::add)
            out.space()
        }

        if (out.lines.isEmpty()) out.add(tr("none"))
    }

    private fun structureTab(info: CaracatronMaterialInfoPayload, labelColor: Int, font: Font, width: Int, out: CaracatronInfoLines) {
        info.blocks.filter { it.structures.isNotEmpty() }.forEach { block ->
            blockTitle(block, if (block.dropsItem) "block.drops_structures" else "block.structures", font, width, out.lines)
            block.structures.forEach { structure ->
                out.space()
                structureLines(structure, labelColor).forEach(out::add)
            }
            out.space()
        }

        lootSection(info.loot.filter { it.kind == CaracatronMaterialLoot.CHEST }, false, labelColor, out) { title(out, "loot.chest") }
        if (out.lines.isEmpty()) out.add(tr("none_structures"))
    }

    private fun diggingTab(info: CaracatronMaterialInfoPayload, labelColor: Int, font: Font, width: Int, out: CaracatronInfoLines) {
        lootSection(info.loot.filter { it.kind == CaracatronMaterialLoot.BRUSH }, true, labelColor, out) {
            iconTitle(BRUSH, tr("loot.brush").withStyle(ChatFormatting.BOLD), font, width, out.lines)
        }
        if (out.lines.isEmpty()) out.add(tr("none_digging"))
    }

    private fun recipeTab(info: CaracatronMaterialInfoPayload, labelColor: Int, out: CaracatronInfoLines) {
        merged(info.source.recipes).forEachIndexed { index, (recipe, types) ->
            if (index > 0) out.space()
            recipeLines(recipe, types, labelColor).forEach { out.add(it, centered = true) }
            out.grid(
                CaracatronRecipeGrid(
                    recipe.width,
                    recipe.grid,
                    fuel = types.any { it in FUEL_TYPES },
                    result = info.item,
                    resultCount = recipe.count
                )
            )
        }
        if (out.lines.isEmpty()) out.add(tr("none_recipes"))
    }

    /**
     * Adds a loot section: its title, then one group per place with the blocks or chests holding the item, and the
     * tables without a known place on an "Others:" line.
     */
    private fun lootSection(loot: List<CaracatronMaterialLoot>, brushed: Boolean, labelColor: Int, out: CaracatronInfoLines, title: () -> Unit) {
        if (loot.isEmpty()) return
        title()
        lootGroups(loot).forEach { group ->
            out.space()
            lootLines(group, brushed, labelColor).forEach(out::add)
        }
        val unplaced = loot.filter { it.place == null }.map { Component.literal(CaracatronSpawnText.readable(it.table.path)) }.distinctBy { it.string }
        if (unplaced.isNotEmpty()) {
            out.space()
            out.add(bulleted(label(tr("label.other"), labelColor).append(" ").append(CaracatronSpawnText.join(unplaced))))
        }
        out.space()
    }

    private fun title(out: CaracatronInfoLines, key: String) = out.add(tr(key).withStyle(ChatFormatting.BOLD), CaracatronSpawnText.TITLE_COLOR)

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
     * Adds a title after an item icon, centered on the icon when it fits in one line.
     */
    private fun iconTitle(icon: ResourceLocation, text: Component, font: Font, width: Int, lines: MutableList<CaracatronInfoLine>) {
        val split = font.split(text, width - CaracatronInfoLine.ICON_INDENT)
        val single = split.size < 2
        split.forEachIndexed { index, line ->
            lines += CaracatronInfoLine(
                line,
                CaracatronSpawnText.TITLE_COLOR,
                if (index == 0) icon else null,
                indented = true,
                textOffset = if (single) CaracatronInfoLine.HALF_LINE else 0
            )
        }
        if (single) lines += CaracatronInfoLine(FormattedCharSequence.EMPTY, CaracatronSpawnText.TEXT_COLOR, indented = true)
    }

    /**
     * Describes a spot: its biomes ("Biomes:", or "Biomes (Nether):" outside the Overworld) as a list, how it generates,
     * its altitude and how often, each after its label.
     */
    private fun spotLines(spot: CaracatronMaterialSpot, labelColor: Int): List<Component> {
        val heading = if (spot.dimension == OVERWORLD) tr("label.biomes") else tr("label.biomes_in", dimensionName(spot.dimension))
        val lines = biomeLines(heading, spot.biomes, labelColor).toMutableList()

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
     * Describes a structure a block is part of: its name as a label, then where it generates.
     */
    private fun structureLines(structure: CaracatronMaterialStructure, labelColor: Int): List<Component> {
        val lines = mutableListOf<Component>(bulleted(label(placeName(structure.structure), labelColor, colon = false)))
        structure.dimension?.let { lines += generatedIn(it, structure.biomes, labelColor) }
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
     * (brushing) or the chests holding it, then where it generates.
     */
    private fun lootLines(group: LootGroup, brushed: Boolean, labelColor: Int): List<Component> {
        val holders = if (brushed) {
            group.entries.map { entry -> entry.block?.let { BuiltInRegistries.BLOCK.get(it).name } ?: tr("loot.suspicious") }
        } else {
            group.entries.map { Component.literal(CaracatronSpawnText.readable(it.table.path)) }
        }.distinctBy { it.string }

        val lines = mutableListOf<Component>(
            bulleted(label(placeName(group.place), labelColor).append(" ").append(CaracatronSpawnText.join(holders)))
        )
        group.dimension?.let { lines += generatedIn(it, group.biomes, labelColor) }
        return lines
    }

    /**
     * Describes the berry source of the item: where it grows naturally and its mutations, each as a list under its
     * heading.
     */
    private fun sourceLines(info: CaracatronMaterialInfoPayload, labelColor: Int): List<Component> {
        val source = info.source
        val lines = mutableListOf<Component>()
        when {
            source.growsEverywhere -> lines += Component.translatable("text.caraca_customs.caracatron.source.grows_everywhere")
            source.biomes.isNotEmpty() -> {
                lines += bulleted(label(Component.translatable("text.caraca_customs.caracatron.source.grows_in"), labelColor, colon = false))
                lines += cutBiomes(source.biomes.map { it.toString() }, SUB_BULLET)
            }
        }
        if (source.mutations.isNotEmpty()) {
            lines += bulleted(label(Component.translatable("text.caraca_customs.caracatron.source.mutations"), labelColor, colon = false))
            source.mutations.forEach { (first, second) ->
                lines += Component.literal(SUB_BULLET).append(Component.translatable("text.caraca_customs.caracatron.source.mutation", itemName(first), itemName(second)))
            }
        }
        return lines
    }

    /**
     * Merges the cooking recipes (furnace, blast furnace, smoker, campfire) with the same ingredients into one, keeping
     * the order of the first one.
     *
     * @return Each recipe with the types it stands for.
     */
    private fun merged(recipes: List<CaracatronRecipe>): List<Pair<CaracatronRecipe, List<String>>> {
        val groups = LinkedHashMap<Any, Pair<CaracatronRecipe, MutableList<String>>>()
        recipes.forEachIndexed { index, recipe ->
            val key: Any = if (recipe.type in COOKING_TYPES) Triple(recipe.ingredients, recipe.width, recipe.grid) else index
            groups.getOrPut(key) { recipe to mutableListOf() }.second += recipe.type
        }
        return groups.values.map { (recipe, types) -> recipe to types.distinct() }
    }

    /**
     * Describes a recipe: its stations as a label ("Furnace/Blast Furnace" when merged), then each ingredient with how
     * many it takes. Its grid comes after.
     */
    private fun recipeLines(recipe: CaracatronRecipe, types: List<String>, labelColor: Int): List<Component> {
        val station = Component.empty()
        types.forEachIndexed { index, type ->
            if (index > 0) station.append("/")
            station.append(CaracatronItemSourceText.stationName(type))
        }
        val title = label(Component.translatable("text.caraca_customs.caracatron.source.recipe", station), labelColor, colon = false)
        return listOf(title) + recipe.ingredients.map {
            Component.translatable("text.caraca_customs.caracatron.snack.ingredient", it.count, itemName(it.item))
        }
    }

    /**
     * Writes where a structure or loot generates: "Generated in:" (or "Generated in (Nether):" outside the Overworld)
     * and its biomes as a list, or "every biome" on the same line.
     */
    private fun generatedIn(dimension: ResourceLocation, biomes: List<String>, labelColor: Int): List<Component> {
        val heading = if (dimension == OVERWORLD) tr("label.generated_in") else tr("label.generated_in_dimension", dimensionName(dimension))
        return biomeLines(heading, biomes, labelColor)
    }

    /**
     * Writes a biome heading followed by its biomes one per line after a hollow bullet, or "every biome" on the heading's
     * line when the list is empty.
     */
    private fun biomeLines(heading: Component, biomes: List<String>, labelColor: Int): List<Component> {
        val label = label(heading, labelColor)
        if (biomes.isEmpty()) return listOf(bulleted(label.append(" ").append(tr("spot.every_biome"))))
        return listOf(bulleted(label)) + cutBiomes(biomes, CaracatronSpawnText.LIST_BULLET)
    }

    /**
     * Lists biomes one per line after a bullet, up to [MAX_BIOMES]. A longer list ends with "And X more...", whose
     * tooltip lists the rest ([MORE_BIOMES]).
     *
     * @param biomes Biome and biome group ids (`#` prefixed).
     * @param bullet Bullet put before each biome, with its indent.
     */
    private fun cutBiomes(biomes: List<String>, bullet: String): List<Component> {
        if (biomes.size <= MAX_BIOMES + 1) return biomes.map { Component.literal(bullet).append(CaracatronSpawnText.biomeName(it)) }
        val shown = biomes.take(MAX_BIOMES).map { Component.literal(bullet).append(CaracatronSpawnText.biomeName(it)) }
        val rest = biomes.drop(MAX_BIOMES)
        val more = tr("more_biomes", rest.size).withStyle { it.withUnderlined(true).withInsertion(MORE_BIOMES + rest.joinToString(BIOME_SEPARATOR)) }
        return shown + Component.literal(bullet).append(more)
    }

    /**
     * Writes a bulleted line made of a label and its value, like "**Altitude:** Y -64 to 72".
     */
    private fun labeled(labelKey: String, value: Component, labelColor: Int): Component {
        return bulleted(label(tr(labelKey), labelColor).append(" ").append(value))
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
     * Names a structure or feature, like "Village Plains", from its identifier.
     */
    private fun placeName(id: ResourceLocation): Component {
        return Component.translatableWithFallback("$KEY.structure.${id.namespace}.${id.path.replace('/', '.')}", CaracatronSpawnText.readable(id.path))
    }

    /**
     * Names a dimension, like "Overworld", or a readable version of its identifier when there's no translation.
     */
    private fun dimensionName(id: ResourceLocation): Component {
        return Component.translatableWithFallback("$KEY.dimension.${id.namespace}.${id.path.replace('/', '.')}", CaracatronSpawnText.readable(id.path))
    }

    private fun itemName(id: ResourceLocation): Component = BuiltInRegistries.ITEM.get(id).description

    private fun bulleted(line: Component): Component = Component.literal(BULLET).append(line)

    private fun tr(key: String, vararg args: Any): MutableComponent = Component.translatable("$KEY.$key", *args)
}
