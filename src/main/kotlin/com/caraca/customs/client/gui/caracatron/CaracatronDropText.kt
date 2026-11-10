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
 * Lines of the drops page: every item the species drops, by icon, with its amount and chance, then the forms that drop
 * something else. Entries keep the drop table's order, since Cobblemon tries them in that order.
 *
 * Searched items are bold and green, and the hovered one is underlined and can be clicked to search it
 * ([CaracatronInfoLine.link]). With Pasture Loot, each chance is followed by whether pastures drop the item.
 *
 * @author hadouz01
 * @since 1.26.0
 */
object CaracatronDropText {

    /**
     * Prefix of the style insertion of the pasture tags, followed by their lang key (`pasture_allowed` or
     * `pasture_blocked`), whose `.tooltip` explains them.
     */
    const val PASTURE_INSERTION = "pasture:"

    private const val KEY = "text.caraca_customs.caracatron.drops"
    private const val PASTURE_ALLOWED_COLOR = 0x035F15

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
        val out = CaracatronInfoLines(font, width)
        val itemWidth = width - CaracatronInfoLine.ICON_INDENT

        if (groups.all { it.entries.isEmpty() }) {
            out.add(tr("none"))
            return out.lines
        }

        if (pastureLoot != null && pastureLoot.chancePerMinute >= 0) {
            out.add(tr("pasture_chance", percent(pastureLoot.chancePerMinute * 100)))
            out.space()
        }

        groups.forEach { group ->
            val title = group.formName?.let { tr("form", it) } ?: tr("title")
            out.add(title.withStyle(ChatFormatting.BOLD), CaracatronSpawnText.TITLE_COLOR)
            if (group.entries.isEmpty()) out.add(tr("form_none"))

            group.entries.forEach { entry ->
                val text = tr("entry", quantity(entry), percent(entry.percentage.toDouble()))
                pastureLoot?.let { text.append(" ").append(pastureTag(entry.item in it.blacklist)) }
                if (entry.item in matched) text.withStyle { it.withBold(true).withColor(matchColor) }
                if (entry.item == hovered) text.withStyle(ChatFormatting.UNDERLINE)

                val entryLines = font.split(text, itemWidth)
                val singleLine = entryLines.size < 2
                val textOffset = if (singleLine) CaracatronInfoLine.HALF_LINE else 0

                entryLines.forEachIndexed { index, line ->
                    out.lines += CaracatronInfoLine(
                        text = line,
                        color = CaracatronSpawnText.TEXT_COLOR,
                        item = if (index == 0) entry.item else null,
                        indented = true,
                        textOffset = textOffset,
                        link = entry.item
                    )
                }
                if (singleLine) {
                    out.lines += CaracatronInfoLine(FormattedCharSequence.EMPTY, CaracatronSpawnText.TEXT_COLOR, indented = true, link = entry.item)
                }
            }
            out.space()
        }
        return out.lines
    }

    /**
     * Writes whether pastures drop an item, like "(✔ Pasture)" in green or "(✘ Pasture)" in red. It carries
     * [PASTURE_INSERTION] so the screen can explain it on hover, and keeps its color when the entry is highlighted.
     *
     * @param blocked Whether Pasture Loot's blocklist has the item.
     */
    private fun pastureTag(blocked: Boolean): Component {
        val key = if (blocked) "pasture_blocked" else "pasture_allowed"
        val color = if (blocked) CaracatronSpawnText.LOCKED_COLOR else PASTURE_ALLOWED_COLOR
        return tr(key).withStyle { it.withColor(color).withInsertion("$PASTURE_INSERTION$key") }
    }

    /**
     * Writes how many of an item a drop gives: its range when it has one, like `1-3`.
     */
    private fun quantity(entry: ItemDropEntry): String {
        val range = entry.quantityRange ?: return entry.quantity.toString()
        return if (range.first == range.last) range.first.toString() else "${range.first}-${range.last}"
    }

    /**
     * Writes a percentage with up to two decimals, like `2.5%` or `100%`.
     */
    private fun percent(value: Double): String {
        return BigDecimal(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%"
    }

    private fun tr(key: String, vararg args: Any): MutableComponent = Component.translatable("$KEY.$key", *args)
}
