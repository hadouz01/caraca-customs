package com.caraca.customs.client.gui.caracatron

import com.caraca.customs.network.CaracatronPastureLoot
import com.cobblemon.mod.common.api.drop.ItemDropEntry
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Turns a species' drops into the lines of the Caracatron's drops page: every item it drops when defeated, shown by its
 * icon (named in a tooltip) with how many and the chance of its drop entry, the species' own drops first and then the
 * forms that drop something else.
 *
 * With Pasture Loot on the server, the panel also says how often a pastured Pokémon drops something and marks the items
 * Pasture Loot's blocklist keeps out of pastures. The whole line of the items matching the search is bold and green,
 * and single line entries sit in the middle of their icon. The item under the mouse is underlined, and clicking it
 * searches its id ([CaracatronInfoLine.link]). Entries keep the drop table's order, since Cobblemon tries
 * them in that order.
 *
 * @author hadouz01
 * @since 1.26.0
 */
object CaracatronDropText {

    private const val KEY = "text.caraca_customs.caracatron.drops"

    /**
     * Builds and wraps the drops page lines. Each item takes at least two lines, so its icon never overlaps the next one.
     *
     * @param groups Drops of the species and its forms, from [CaracatronDropIndex].
     * @param pastureLoot Pasture Loot's settings, `null` when the server doesn't have it.
     * @param matched Items matching the search, whose lines are highlighted.
     * @param matchColor RGB color of the highlighted lines ([CaracatronTextColors.match]).
     * @param hovered Item under the mouse, whose lines are underlined.
     * @param font Font used to wrap the lines.
     * @param width Wrapping width, in font units.
     * @return The wrapped lines, blank ones included as spacing.
     */
    fun lines(
        groups: List<CaracatronDropGroup>,
        pastureLoot: CaracatronPastureLoot?,
        matched: Set<ResourceLocation>,
        matchColor: Int,
        hovered: ResourceLocation?,
        font: Font,
        width: Int
    ): List<CaracatronInfoLine> {
        val lines = mutableListOf<CaracatronInfoLine>()
        val itemWidth = width - CaracatronInfoLine.ICON_INDENT

        fun add(component: Component, color: Int = CaracatronSpawnText.TEXT_COLOR) {
            font.split(component, width).forEach { lines += CaracatronInfoLine(it, color) }
        }

        fun space() {
            lines += CaracatronInfoLine(FormattedCharSequence.EMPTY, CaracatronSpawnText.TEXT_COLOR)
        }

        if (groups.all { it.entries.isEmpty() }) {
            add(tr("none"))
            return lines
        }

        if (pastureLoot != null && pastureLoot.chancePerMinute >= 0) {
            add(tr("pasture_chance", percent(pastureLoot.chancePerMinute * 100)))
            space()
        }

        groups.forEach { group ->
            val title = group.formName?.let { tr("form", it) } ?: tr("title")
            add(title.withStyle(ChatFormatting.BOLD), CaracatronSpawnText.TITLE_COLOR)
            if (group.entries.isEmpty()) add(tr("form_none"))

            group.entries.forEach { entry ->
                val text = tr("entry", quantity(entry), percent(entry.percentage.toDouble()))
                if (entry.item in matched) text.withStyle { it.withBold(true).withColor(matchColor) }
                if (entry.item == hovered) text.withStyle(ChatFormatting.UNDERLINE)

                val entryLines = font.split(text, itemWidth)
                val pasture = pastureLoot?.let { tr(if (entry.item in it.blacklist) "pasture_blocked" else "pasture_allowed") }
                val pastureColor = if (pastureLoot != null && entry.item in pastureLoot.blacklist) CaracatronSpawnText.LOCKED_COLOR else CaracatronSpawnText.TEXT_COLOR
                val pastureLines = pasture?.let { font.split(it, itemWidth) }.orEmpty()
                val singleLine = entryLines.size + pastureLines.size < 2
                val textOffset = if (singleLine) CaracatronInfoLine.HALF_LINE else 0

                entryLines.forEachIndexed { index, line ->
                    lines += CaracatronInfoLine(line, CaracatronSpawnText.TEXT_COLOR, if (index == 0) entry.item else null, indented = true, textOffset = textOffset, link = entry.item)
                }
                pastureLines.forEach { lines += CaracatronInfoLine(it, pastureColor, indented = true, link = entry.item) }
                if (singleLine) lines += CaracatronInfoLine(FormattedCharSequence.EMPTY, CaracatronSpawnText.TEXT_COLOR, indented = true, link = entry.item)
            }
            space()
        }
        return lines
    }

    /**
     * Writes how many of an item a drop gives: its range when it has one, e.g. `1-3`.
     */
    private fun quantity(entry: ItemDropEntry): String {
        val range = entry.quantityRange ?: return entry.quantity.toString()
        return if (range.first == range.last) range.first.toString() else "${range.first}-${range.last}"
    }

    /**
     * Writes a percentage with up to two decimals, e.g. `2.5%` or `100%`.
     */
    private fun percent(value: Double): String {
        return BigDecimal(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%"
    }

    private fun tr(key: String, vararg args: Any): MutableComponent = Component.translatable("$KEY.$key", *args)
}
