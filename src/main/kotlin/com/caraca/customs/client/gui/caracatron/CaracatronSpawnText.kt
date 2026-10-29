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
 * @property item Item whose icon is drawn at the start of the line, spanning it and the next one (since 1.27.0).
 * @property indented Whether the text starts after the icon column, [ICON_INDENT] font units in (since 1.27.0).
 * @property textOffset How far down the text is drawn, in font units, e.g. [HALF_LINE] to center a single line on the
 * icon spanning it and the next one (since 1.28.1).
 * @property link Item the line belongs to on the drops page: hovering the line underlines that item, clicking it
 * searches the item's id (since 1.28.7).
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
    val link: ResourceLocation? = null
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
     * color when drawn) and the locked tier warning, which keeps its red.
     */
    internal const val TITLE_COLOR = 0x35444A
    internal const val TEXT_COLOR = 0x606B6E
    internal const val LOCKED_COLOR = 0xB04A4A

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
        val lines = mutableListOf<CaracatronInfoLine>()

        fun add(component: Component, color: Int = TEXT_COLOR) {
            font.split(component, width).forEach { lines += CaracatronInfoLine(it, color) }
        }

        fun space() {
            lines += CaracatronInfoLine(FormattedCharSequence.EMPTY, TEXT_COLOR)
        }

        when {
            info.entries.isEmpty() -> {
                add(tr("info.no_spawns"))
                return lines
            }
            info.firstTier == 0 -> add(tr("info.never"), LOCKED_COLOR)
            info.firstTier > shownTier -> add(tr("info.locked", CaracatronScreen.roman(info.firstTier)), LOCKED_COLOR)
            else -> add(tr("info.rate", CaracatronScreen.roman(info.firstTier), number(info.tierMultiplier)), TITLE_COLOR)
        }
        info.tierLevels?.let { add(tr("info.tier_levels", it.first, it.last)) }
        space()

        val entries = info.entries.filter { it.bucket == bucket }.sortedByDescending { it.weight }
        if (entries.isEmpty()) {
            add(tr("info.empty_bucket", tr("rarity.$bucket")))
            return lines
        }

        entries.forEach { entry ->
            add(title(entry), TITLE_COLOR)
            add(tr("info.biomes", if (entry.biomes.isEmpty()) tr("info.anywhere") else join(entry.biomes.map(::biomeName))))
            if (entry.excludedBiomes.isNotEmpty()) add(tr("info.excluded", join(entry.excludedBiomes.map(::biomeName))))
            if (entry.timeRange.isNotEmpty() && entry.timeRange != "any") add(tr("info.time", timeName(entry.timeRange)))
            if (entry.weather.isNotEmpty()) add(tr("info.weather", tr("weather.${entry.weather}")))
            entry.canSeeSky?.let { add(tr(if (it) "info.sky" else "info.no_sky")) }
            heightLine(entry)?.let(::add)
            if (entry.structures.isNotEmpty()) add(tr("info.structures", join(entry.structures.map(::structureName))))
            if (info.tierLevels == null) entry.levels?.let { add(tr("info.levels", it.first, it.last)) }
            space()
        }
        return lines
    }

    private fun title(entry: CaracatronSpawnEntry): Component {
        val title = Component.translatableWithFallback("$KEY.position.${entry.position}", readable(entry.position))
            .withStyle(ChatFormatting.BOLD)
        if (entry.isHerd) title.append(" · ").append(tr("info.herd"))
        return title.append(" · ").append(tr("info.weight", number(entry.weight)))
    }

    private fun heightLine(entry: CaracatronSpawnEntry): Component? = when {
        entry.minY != null && entry.maxY != null -> tr("info.height", entry.minY, entry.maxY)
        entry.minY != null -> tr("info.height_min", entry.minY)
        entry.maxY != null -> tr("info.height_max", entry.maxY)
        else -> null
    }

    /**
     * Names a biome or a biome tag (`#` prefixed). Tag names carry the raw tag as their style's insertion, so the screen can
     * tell which tag is under the mouse and list its biomes ([CaracatronBiomeTags]).
     *
     * @param raw Biome or tag identifier, e.g. `minecraft:forest` or `#cobblemon:is_forest`.
     */
    internal fun biomeName(raw: String): Component {
        val isTag = raw.startsWith("#")
        val id = ResourceLocation.tryParse(raw.removePrefix("#")) ?: return Component.literal(raw)
        val prefix = if (isTag) "tag.worldgen.biome" else "biome"
        val name = Component.translatableWithFallback("$prefix.${id.namespace}.${id.path.replace('/', '.')}", readable(id.path))
        return if (isTag) name.withStyle { it.withInsertion(raw) } else name
    }

    private fun structureName(raw: String): Component {
        val id = ResourceLocation.tryParse(raw.removePrefix("#")) ?: return Component.literal(raw)
        return Component.literal(readable(id.path))
    }

    private fun timeName(key: String): Component = Component.translatableWithFallback("cobblemon.ui.time_range.$key", readable(key))

    /**
     * Makes an identifier path readable, e.g. `is_snowy_forest` becomes "Snowy Forest".
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
     * Writes a number with up to two decimals and no trailing zeros, e.g. `10` or `0.25`.
     */
    internal fun number(value: Float): String {
        if (value % 1.0f == 0.0f) return value.toInt().toString()
        return String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
    }

    private fun tr(key: String, vararg args: Any): MutableComponent = Component.translatable("$KEY.$key", *args)
}
