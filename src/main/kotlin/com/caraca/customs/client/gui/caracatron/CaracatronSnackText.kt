package com.caraca.customs.client.gui.caracatron

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
 * Turns the Poké Snack data of a species into the lines of the Caracatron's snack tab: the snack recipe, every
 * seasoning that attracts the species, the names of the best ones to put in it and how Cobblemon combines them.
 *
 * Seasonings with more than one matching effect of the same kind (e.g. a berry covering both egg groups of the species)
 * are shown in a single line.
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

    /**
     * Builds and wraps the snack tab lines.
     *
     * @param info Spawn pool and Poké Snack data of the selected species.
     * @param species Selected species, used to pick the recommended seasonings.
     * @param font Font used to wrap the lines.
     * @param width Wrapping width, in font units.
     * @return The wrapped lines, blank ones included as spacing.
     */
    fun lines(info: CaracatronSpawnInfoPayload, species: Species, font: Font, width: Int): List<CaracatronInfoLine> {
        val lines = mutableListOf<CaracatronInfoLine>()

        fun add(component: Component, color: Int = CaracatronSpawnText.TEXT_COLOR) {
            font.split(component, width).forEach { lines += CaracatronInfoLine(it, color) }
        }

        fun space() {
            lines += CaracatronInfoLine(FormattedCharSequence.EMPTY, CaracatronSpawnText.TEXT_COLOR)
        }

        add(itemName(POKE_SNACK).copy().withStyle(ChatFormatting.BOLD), CaracatronSpawnText.TITLE_COLOR)
        if (info.snackRecipe.isNotEmpty()) {
            add(tr("recipe").withStyle(ChatFormatting.BOLD), CaracatronSpawnText.TITLE_COLOR)
            info.snackRecipe.forEach { add(Component.literal("- ").append(tr("ingredient", it.count, hoverableName(it.item)))) }
        }
        space()

        if (info.entries.isEmpty()) {
            add(tr("no_spawns"))
            return lines
        }

        val herdOnly = info.entries.all { it.isHerd }
        val seasonings = info.snackSeasonings.filter { !herdOnly || it.effect == RARITY }

        if (seasonings.isEmpty()) {
            add(tr("none"))
        } else {
            add(tr("seasonings").withStyle(ChatFormatting.BOLD), CaracatronSpawnText.TITLE_COLOR)
            seasonings.groupBy { it.item to it.effect }.forEach { (key, group) -> add(seasoningLine(key.first, key.second, group)) }
        }

        val recommended = recommended(info, species, seasonings)
        if (recommended.isNotEmpty()) {
            space()
            add(tr("recommended").withStyle(ChatFormatting.BOLD), CaracatronSpawnText.TITLE_COLOR)
            recommended.forEach { group -> add(Component.literal("- ").append(hoverableName(group.first().item).withStyle(ChatFormatting.BOLD))) }
        }

        if (seasonings.any { it.effect == TYPING || it.effect == EGG_GROUP }) {
            space()
            add(tr("stacking"))
        }
        if (info.entries.any { it.isHerd }) {
            space()
            add(tr("herds"))
        }
        return lines
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

    private fun seasoningLine(item: ResourceLocation, effect: String, group: List<CaracatronSnackSeasoning>): Component {
        val name = hoverableName(item).withStyle(ChatFormatting.BOLD)
        val value = CaracatronSpawnText.number(group.maxOf { it.value })
        val line = when (effect) {
            TYPING -> tr("type", name, CaracatronSpawnText.join(group.map { typeName(it.target) }), value)
            EGG_GROUP -> tr("egg_group", name, CaracatronSpawnText.join(group.map { eggGroupName(it.target) }), value)
            EV -> tr("ev", name, CaracatronSpawnText.join(group.map { Stats.getStat(it.target).displayName }))
            else -> tr("rarity", name)
        }
        return Component.literal("- ").append(line)
    }

    private fun itemName(id: ResourceLocation): Component = BuiltInRegistries.ITEM.get(id).description

    /**
     * Names an item and marks it with its identifier as the style's insertion ([ITEM_INSERTION] prefixed), so the screen
     * can tell which item is under the mouse and show how to get it ([CaracatronItemSourceText]).
     */
    private fun hoverableName(id: ResourceLocation): MutableComponent {
        return itemName(id).copy().withStyle { it.withInsertion("$ITEM_INSERTION$id") }
    }

    private fun typeName(name: String): Component {
        return ElementalTypes.get(name)?.displayName ?: Component.literal(CaracatronSpawnText.readable(name))
    }

    private fun eggGroupName(name: String): Component {
        return Component.translatableWithFallback("cobblemon.egg_group.$name", CaracatronSpawnText.readable(name))
    }

    private fun tr(key: String, vararg args: Any): MutableComponent = Component.translatable("$KEY.$key", *args)
}
