package com.caraca.customs.client.gui.caracatron

import com.caraca.customs.network.CaracatronSpawnEntry
import com.caraca.customs.network.CaracatronSpawnInfoPayload
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence
import java.util.Locale

/**
 * A wrapped line of the Caracatron info panel.
 *
 * @property text Text of the line.
 * @property color Text color.
 * @property item Item icon drawn at the start of the line, spanning it and the next one.
 * @property indented Whether the text starts after the icon column ([ICON_INDENT]).
 * @property textOffset How far down the text is drawn, in font units. [HALF_LINE] centers a single line on its icon.
 * @property link Drops page item the line belongs to, underlined on hover and searched on click.
 * @property grid Recipe grid drawn from the line down, over the empty lines after it, centered on the panel.
 * @property centered Whether the text is centered on the panel.
 * @property stages Growth stages drawn from the line down, over the empty lines after it.
 *
 * @author hadouz01
 * @since 1.12.0
 */
data class CaracatronInfoLine(
    val text: FormattedCharSequence,
    val color: Int,
    val item: ResourceLocation? = null,
    val indented: Boolean = item != null,
    val textOffset: Int = 0,
    val link: ResourceLocation? = null,
    val grid: CaracatronRecipeGrid? = null,
    val centered: Boolean = false,
    val stages: CaracatronGrowthStages? = null
) {

    companion object {
        /**
         * Width of the icon column, in font units: the 16 unit icon and a small gap.
         */
        const val ICON_INDENT = 20

        /**
         * Half the 10 font unit line height, moving a single line of text to the middle of a two line icon.
         */
        const val HALF_LINE = 5
    }
}

/**
 * Turns a species' spawn pool into the lines shown on the Caracatron spawn panel: how the shown tier affects the species,
 * followed by every spawn of the selected rarity, most common first.
 *
 * Biomes, structures and time ranges use their translations when the game has one, otherwise a readable version of their
 * identifier (`#cobblemon:is_forest` becomes "Forest").
 *
 * @author hadouz01
 * @since 1.12.0
 */
object CaracatronSpawnText {

    /**
     * Spawn buckets in the order of the rarity icons.
     */
    val BUCKETS = listOf("common", "uncommon", "rare", "ultra-rare")

    /**
     * RGB colors the info panel lines are built with: titles and plain text (swapped for the screen color's panel text
     * color when drawn), the locked tier warning, which keeps its red, and the "Biomes:" and "Except:" labels of the
     * spawns.
     */
    internal const val TITLE_COLOR = 0x35444A
    internal const val TEXT_COLOR = 0x606B6E
    internal const val LOCKED_COLOR = 0xB04A4A
    private const val EXCLUDED_COLOR = 0xA43B42
    private const val BIOMES_COLOR = 0x049720
    private const val BULLET = "• "

    /**
     * Bullet of the items listed under a heading, like the biomes under "Biomes:".
     */
    internal const val LIST_BULLET = "   ◦ "

    /**
     * Mark after a biome group name, so it reads apart from a single biome.
     */
    private const val GROUP_MARK = "*"

    private const val KEY = "text.caraca_customs.caracatron"

    /**
     * Builds and wraps the panel lines.
     *
     * @param info Spawn pool of the selected species.
     * @param bucket Selected rarity bucket.
     * @param shownTier Tier shown on the Caracatron.
     * @param font Font used to wrap the lines.
     * @param width Wrapping width, in font units.
     * @return The wrapped lines, blank ones included as spacing.
     */
    fun lines(info: CaracatronSpawnInfoPayload, bucket: String, shownTier: Int, font: Font, width: Int): List<CaracatronInfoLine> {
        val out = CaracatronInfoLines(font, width)

        when {
            info.entries.isEmpty() -> {
                out.add(tr("info.no_spawns"))
                return out.lines
            }
            info.firstTier == 0 -> out.add(tr("info.never"), LOCKED_COLOR)
            info.firstTier > shownTier -> out.add(tr("info.locked", bold(CaracatronScreen.roman(info.firstTier))), LOCKED_COLOR)
            else -> {
                val rate = bold("${number(info.tierMultiplier)}x")
                out.add(tr("info.rate", bold(CaracatronScreen.roman(info.firstTier)), rate), TITLE_COLOR)
            }
        }
        info.tierLevels?.let {
            out.space()
            out.add(labeled(tr("info.tier_levels"), tr("info.level_range", it.first, it.last)))
        }
        out.space()

        val entries = info.entries.filter { it.bucket == bucket }.sortedByDescending { it.weight }
        if (entries.isEmpty()) {
            out.add(tr("info.empty_bucket", tr("rarity.$bucket")))
            return out.lines
        }

        entries.forEach { entry ->
            out.add(title(entry), TITLE_COLOR)
            if (entry.biomes.isEmpty()) {
                out.add(Component.literal(BULLET).append(labeled(tr("info.biomes"), tr("info.anywhere"), BIOMES_COLOR)))
            } else {
                out.add(heading(tr("info.biomes"), BIOMES_COLOR))
                entry.biomes.forEach { out.add(listed(biomeName(it))) }
            }
            if (entry.excludedBiomes.isNotEmpty()) {
                out.add(heading(tr("info.excluded"), EXCLUDED_COLOR))
                entry.excludedBiomes.forEach { out.add(listed(biomeName(it))) }
            }
            if (entry.timeRange.isNotEmpty() && entry.timeRange != "any") out.add(labeled(tr("info.time"), timeName(entry.timeRange)))
            if (entry.weather.isNotEmpty()) out.add(labeled(tr("info.weather"), tr("weather.${entry.weather}")))
            entry.canSeeSky?.let { out.add(tr(if (it) "info.sky" else "info.no_sky")) }
            heightLine(entry)?.let { out.add(labeled(tr("info.height"), it)) }
            if (entry.structures.isNotEmpty()) out.add(labeled(tr("info.structures"), join(entry.structures.map(::structureName))))
            if (info.tierLevels == null) entry.levels?.let { out.add(labeled(tr("info.levels"), tr("info.level_range", it.first, it.last))) }
            out.space()
        }
        return out.lines
    }

    /**
     * Writes a line as a bold label followed by its plain value, like "**Biomes:** Forest, Plains".
     *
     * @param label Label, with its colon.
     * @param value Value after the label.
     * @param labelColor RGB color of the label, the line's own color when `null`.
     */
    private fun labeled(label: MutableComponent, value: Component, labelColor: Int? = null): Component {
        val styled = label.withStyle { style -> style.withBold(true).let { if (labelColor != null) it.withColor(labelColor) else it } }
        return Component.empty().append(styled).append(" ").append(value)
    }

    /**
     * Writes a bold label alone on its line after a bullet, with a list under it.
     */
    private fun heading(label: MutableComponent, color: Int): Component {
        return Component.literal(BULLET).append(label.withStyle { it.withBold(true).withColor(color) })
    }

    /**
     * Writes an item of a list under a heading, indented after a hollow bullet.
     */
    private fun listed(item: Component): Component = Component.literal(LIST_BULLET).append(item)

    private fun bold(text: String): Component = Component.literal(text).withStyle(ChatFormatting.BOLD)

    private fun title(entry: CaracatronSpawnEntry): Component {
        val title = Component.translatableWithFallback("$KEY.position.${entry.position}", readable(entry.position))
            .withStyle(ChatFormatting.BOLD)
        if (entry.isHerd) title.append(" · ").append(tr("info.herd"))
        return title.append(" · ").append(tr("info.weight", number(entry.weight)))
    }

    private fun heightLine(entry: CaracatronSpawnEntry): Component? = when {
        entry.minY != null && entry.maxY != null -> tr("info.height_range", entry.minY, entry.maxY)
        entry.minY != null -> tr("info.height_min", entry.minY)
        entry.maxY != null -> tr("info.height_max", entry.maxY)
        else -> null
    }

    /**
     * Names a biome or a biome tag (`#` prefixed). Tag names end with [GROUP_MARK] and carry the raw tag as their style's
     * insertion, so the screen can tell which tag is under the mouse and list its biomes ([CaracatronBiomeTags]).
     *
     * @param raw Biome or tag identifier, like `minecraft:forest` or `#cobblemon:is_forest`.
     * @param marked Whether a tag name gets [GROUP_MARK].
     */
    internal fun biomeName(raw: String, marked: Boolean = true): Component {
        val isTag = raw.startsWith("#")
        val id = ResourceLocation.tryParse(raw.removePrefix("#")) ?: return Component.literal(raw)
        val prefix = if (isTag) "tag.worldgen.biome" else "biome"
        val name = Component.translatableWithFallback("$prefix.${id.namespace}.${id.path.replace('/', '.')}", readable(id.path))
        if (!isTag) return name
        if (marked) name.append(GROUP_MARK)
        return name.withStyle { it.withInsertion(raw) }
    }

    private fun structureName(raw: String): Component {
        val id = ResourceLocation.tryParse(raw.removePrefix("#")) ?: return Component.literal(raw)
        return Component.literal(readable(id.path))
    }

    private fun timeName(key: String): Component = Component.translatableWithFallback("cobblemon.ui.time_range.$key", readable(key))

    /**
     * Makes an identifier path readable, so `is_snowy_forest` becomes "Snowy Forest".
     */
    internal fun readable(path: String): String {
        return path.substringAfterLast('/')
            .removePrefix("is_")
            .removePrefix("has_")
            .split('_', '-')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.ROOT) } }
    }

    /**
     * Joins components with commas.
     */
    internal fun join(parts: List<Component>): Component {
        val joined = Component.empty()
        parts.forEachIndexed { index, part ->
            if (index > 0) joined.append(", ")
            joined.append(part)
        }
        return joined
    }

    /**
     * Writes a number with up to two decimals and no trailing zeros, like `10` or `0.25`.
     */
    internal fun number(value: Float): String {
        if (value % 1.0f == 0.0f) return value.toInt().toString()
        return String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
    }

    private fun tr(key: String, vararg args: Any): MutableComponent = Component.translatable("$KEY.$key", *args)
}
