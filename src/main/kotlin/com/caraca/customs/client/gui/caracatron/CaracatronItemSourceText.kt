package com.caraca.customs.client.gui.caracatron

import com.caraca.customs.network.CaracatronItemSource
import net.minecraft.ChatFormatting
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation

/**
 * Tooltips telling how to get a snack tab item: where a berry grows and its mutations, or a recipe making it. Items that
 * are neither just say they're found in the world.
 *
 * @author hadouz01
 * @since 1.23.0
 */
object CaracatronItemSourceText {

    private const val KEY = "text.caraca_customs.caracatron.source"
    private const val INDENT = "  "

    /**
     * Builds the tooltip of an item.
     *
     * @param item Hovered item.
     * @param source How to get it, `null` when the server didn't send it.
     * @return The tooltip lines, the item name in bold first.
     */
    fun tooltip(item: ResourceLocation, source: CaracatronItemSource?): List<Component> {
        val lines = mutableListOf<Component>(BuiltInRegistries.ITEM.get(item).description.copy().withStyle(ChatFormatting.BOLD))
        if (source == null) return lines + tr("unknown").withStyle(ChatFormatting.GRAY)

        when {
            source.growsEverywhere -> lines += tr("grows_everywhere")
            source.biomes.isNotEmpty() -> {
                lines += tr("grows_in")
                lines += CaracatronBiomeTags.biomeLines(source.biomes).map(::indented)
            }
        }

        if (source.mutations.isNotEmpty()) {
            lines += tr("mutations")
            lines += source.mutations.map { (first, second) -> indented(tr("mutation", itemName(first), itemName(second))) }
        }

        source.recipes.firstOrNull()?.let { recipe ->
            lines += tr("recipe", stationName(recipe.type))
            recipe.ingredients.forEach {
                lines += indented(Component.translatable("text.caraca_customs.caracatron.snack.ingredient", it.count, itemName(it.item)))
            }
        }

        if (lines.size == 1) lines += tr("found").withStyle(ChatFormatting.GRAY)
        return lines
    }

    /**
     * Names the station a recipe type is made at, like "Crafting Table" for `minecraft:crafting`, or a readable version
     * of the type when there's no translation.
     */
    internal fun stationName(recipeType: String): Component {
        val id = ResourceLocation.tryParse(recipeType) ?: return Component.literal(recipeType)
        return Component.translatableWithFallback("$KEY.station.${id.namespace}.${id.path}", CaracatronSpawnText.readable(id.path))
    }

    private fun itemName(id: ResourceLocation): Component = BuiltInRegistries.ITEM.get(id).description

    private fun indented(line: Component): Component = Component.literal(INDENT).append(line)

    private fun tr(key: String, vararg args: Any): MutableComponent = Component.translatable("$KEY.$key", *args)
}
