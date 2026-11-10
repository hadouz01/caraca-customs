package com.caraca.customs.client.gui.caracatron

import com.caraca.customs.network.CaracatronRecipe
import com.caraca.customs.network.CaracatronSnackSeasoning
import com.caraca.customs.network.CaracatronSpawnInfoPayload
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.pokemon.Species
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence

/**
 * Lines of the snack tab: the best seasonings for the species, the Poké Snack and Poké Bait Campfire Pot grids (with
 * those seasonings), every seasoning that attracts the species and how Cobblemon combines them. Items are listed after
 * their icon, and a seasoning matching the same kind of effect twice (like both egg groups) takes a single line.
 *
 * @author hadouz01
 * @since 1.15.0
 */
object CaracatronSnackText {

    /**
     * Prefix of the style insertion marking hoverable item names, followed by the item identifier.
     */
    const val ITEM_INSERTION = "item:"

    private const val KEY = "text.caraca_customs.caracatron.snack"

    private const val TYPING = "typing"
    private const val EGG_GROUP = "egg_group"
    private const val EV = "ev"
    private const val RARITY = "rarity_bucket"
    private const val COMMON = "common"
    private const val MAX_SEASONINGS = 3

    private val POKE_SNACK = ResourceLocation.fromNamespaceAndPath("cobblemon", "poke_snack")
    private val POKE_BAIT = ResourceLocation.fromNamespaceAndPath("cobblemon", "poke_bait")

    /**
     * Builds and wraps the snack tab lines.
     *
     * @param info Spawn pool and Poké Snack data of the selected species.
     * @param species Selected species, used to pick the recommended seasonings.
     * @param hovered Item name under the mouse, drawn in [linkColor].
     * @param linkColor RGB color of the hovered item name.
     * @param font Font used to wrap the lines.
     * @param width Wrapping width, in font units.
     * @return The wrapped lines, blank ones included as spacing.
     */
    fun lines(
        info: CaracatronSpawnInfoPayload,
        species: Species,
        hovered: ResourceLocation?,
        linkColor: Int,
        font: Font,
        width: Int
    ): List<CaracatronInfoLine> {
        val out = CaracatronInfoLines(font, width)
        val herdOnly = info.entries.all { it.isHerd }
        val seasonings = info.snackSeasonings.filter { !herdOnly || it.effect == RARITY }
        val recommended = if (info.entries.isEmpty()) emptyList() else recommended(info, species, seasonings)
        fun name(item: ResourceLocation) = hoverableName(item, hovered, linkColor)

        if (recommended.isNotEmpty()) {
            title(out, tr("recommended"))
            recommended.forEach { group -> itemLine(out, group.first().item, name(group.first().item).withStyle(ChatFormatting.BOLD)) }
            out.space()
        }

        val side = List(MAX_SEASONINGS) { recommended.getOrNull(it)?.first()?.item }
        recipe(out, itemName(POKE_SNACK).copy(), info.snackRecipe, side, POKE_SNACK)
        recipe(out, tr("bait_title"), info.baitRecipe, side, POKE_BAIT)

        if (info.entries.isEmpty()) {
            out.add(tr("no_spawns"))
            return out.lines
        }

        if (seasonings.isEmpty()) {
            out.add(tr("none"))
        } else {
            title(out, tr("seasonings"))
            seasonings.groupBy { it.item to it.effect }.forEach { (key, group) ->
                itemLine(out, key.first, seasoningLine(name(key.first), key.second, group))
            }
        }

        if (seasonings.any { it.effect == TYPING || it.effect == EGG_GROUP }) {
            out.space()
            out.add(tr("stacking"))
        }
        if (info.entries.any { it.isHerd }) {
            out.space()
            out.add(tr("herds"))
        }
        return out.lines
    }

    /**
     * Adds a Campfire Pot recipe: its title, the ingredients heading and the pot's grid, with the recommended seasonings
     * in the column right of it and the result after the arrow.
     */
    private fun recipe(out: CaracatronInfoLines, title: MutableComponent, recipe: CaracatronRecipe?, side: List<ResourceLocation?>, result: ResourceLocation) {
        if (recipe == null) return
        title(out, title)
        title(out, tr("recipe"))
        out.grid(CaracatronRecipeGrid(recipe.width, recipe.grid, side = side, result = result, resultCount = recipe.count))
        out.space()
    }

    private fun title(out: CaracatronInfoLines, text: MutableComponent) = out.add(text.withStyle(ChatFormatting.BOLD), CaracatronSpawnText.TITLE_COLOR)

    /**
     * Adds a line after an item's icon, centered on the icon when it fits in one line.
     */
    private fun itemLine(out: CaracatronInfoLines, item: ResourceLocation, text: Component) {
        val split = out.split(text, CaracatronInfoLine.ICON_INDENT)
        val single = split.size < 2
        split.forEachIndexed { index, line ->
            out.lines += CaracatronInfoLine(
                line,
                CaracatronSpawnText.TEXT_COLOR,
                if (index == 0) item else null,
                indented = true,
                textOffset = if (single) CaracatronInfoLine.HALF_LINE else 0
            )
        }
        if (single) out.lines += CaracatronInfoLine(FormattedCharSequence.EMPTY, CaracatronSpawnText.TEXT_COLOR, indented = true)
    }

    /**
     * Picks the best three seasonings for a Poké Snack aimed at the species, following how Cobblemon combines them:
     *
     * - A type seasoning, preferring the species' primary type. Only the first type seasoning of a snack counts, so one is
     *   enough.
     * - An egg group seasoning. Only the first matching one counts, and it multiplies the type one.
     * - The last slot goes to an EV seasoning (the stat the species yields the most), which leaves only the species that
     *   yield that EV, or to the strongest rarity seasoning when the species has no common spawn, since those flatten the
     *   rarity buckets in favor of rarer spawns. Rarity seasonings are never picked for species with common spawns, as
     *   they'd make those spawns rarer.
     *
     * Free slots, when a kind is missing, go to the remaining options in the same order.
     *
     * @return Each picked seasoning as its rows, in the order they should be shown.
     */
    private fun recommended(info: CaracatronSpawnInfoPayload, species: Species, seasonings: List<CaracatronSnackSeasoning>): List<List<CaracatronSnackSeasoning>> {
        val groups = seasonings.groupBy { it.item to it.effect }.values
        fun ofEffect(effect: String) = groups.filter { it.first().effect == effect }

        val primaryType = species.primaryType.name
        val type = ofEffect(TYPING).minByOrNull { group -> if (group.any { it.target.equals(primaryType, ignoreCase = true) }) 0 else 1 }
        val eggGroup = ofEffect(EGG_GROUP).maxByOrNull { it.size }
        val ev = ofEffect(EV).maxByOrNull { group -> group.maxOf { species.standardForm.evYield[Stats.getStat(it.target)] ?: 0 } }
        val rareOnly = info.entries.none { it.bucket == COMMON }
        val rarity = if (rareOnly) ofEffect(RARITY).maxByOrNull { group -> group.maxOf { it.value } } else null

        val picks = listOfNotNull(type, eggGroup)
        val extras = if (rareOnly) listOfNotNull(rarity, ev) else listOfNotNull(ev, rarity)
        return (picks + extras).take(MAX_SEASONINGS)
    }

    private fun seasoningLine(item: MutableComponent, effect: String, group: List<CaracatronSnackSeasoning>): Component {
        val name = item.withStyle(ChatFormatting.BOLD)
        val value = CaracatronSpawnText.number(group.maxOf { it.value })
        val line = when (effect) {
            TYPING -> tr("type", name, CaracatronSpawnText.join(group.map { typeName(it.target) }), value)
            EGG_GROUP -> tr("egg_group", name, CaracatronSpawnText.join(group.map { eggGroupName(it.target) }), value)
            EV -> tr("ev", name, CaracatronSpawnText.join(group.map { Stats.getStat(it.target).displayName }))
            else -> tr("rarity", name)
        }
        return line
    }

    private fun itemName(id: ResourceLocation): Component = BuiltInRegistries.ITEM.get(id).description

    /**
     * Names an item and marks it with its identifier as the style's insertion ([ITEM_INSERTION] prefixed), so the screen
     * can tell which item is under the mouse, show how to get it ([CaracatronItemSourceText]) and open it on the materials
     * page when clicked. The hovered item takes the link color.
     */
    private fun hoverableName(id: ResourceLocation, hovered: ResourceLocation?, linkColor: Int): MutableComponent {
        val name = itemName(id).copy().withStyle { it.withInsertion("$ITEM_INSERTION$id") }
        return if (id == hovered) name.withStyle { it.withColor(linkColor).withUnderlined(true) } else name
    }

    private fun typeName(name: String): Component {
        return ElementalTypes.get(name)?.displayName ?: Component.literal(CaracatronSpawnText.readable(name))
    }

    private fun eggGroupName(name: String): Component {
        return Component.translatableWithFallback("cobblemon.egg_group.$name", CaracatronSpawnText.readable(name))
    }

    private fun tr(key: String, vararg args: Any): MutableComponent = Component.translatable("$KEY.$key", *args)
}
