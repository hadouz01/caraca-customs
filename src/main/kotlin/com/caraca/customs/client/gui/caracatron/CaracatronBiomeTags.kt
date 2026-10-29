package com.caraca.customs.client.gui.caracatron

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import java.util.Locale

/**
 * Tooltips listing the biomes of a biome tag on the Caracatron's spawn panel, e.g. every biome in "Forest".
 *
 * Biome tags reach the client with the rest of the server's tags, so the list follows the datapacks loaded on the server:
 * Minecraft's biomes along with the ones from installed biome mods that add themselves to the tag. Each biome takes a
 * line, followed by where it comes from: "Vanilla" for Minecraft's, otherwise the name of the mod owning its namespace,
 * or the namespace itself for datapacks. Vanilla biomes come first, then each source in alphabetical order.
 *
 * @author hadouz01
 * @since 1.19.0
 */
object CaracatronBiomeTags {

    private const val KEY = "text.caraca_customs.caracatron.biome_tag"
    private const val VANILLA = "minecraft"
    private const val MIN_SHOWN_BIOMES = 2

    /**
     * A biome of the tag and where it comes from.
     */
    private data class Biome(val name: Component, val source: String, val isVanilla: Boolean)

    /**
     * Builds the tooltip of a biome tag: its name in bold, then one biome per line with its source.
     *
     * @param tag Tag identifier with its `#` prefix.
     * @return The tooltip lines, or `null` when the tag can't be read (no world loaded).
     */
    fun tooltip(tag: String): List<Component>? {
        val id = ResourceLocation.tryParse(tag.removePrefix("#")) ?: return null
        val registry = Minecraft.getInstance().connection?.registryAccess()?.registry(Registries.BIOME)?.orElse(null) ?: return null

        val biomes = registry.getTag(TagKey.create(Registries.BIOME, id))
            .map { holders -> holders.mapNotNull { holder -> holder.unwrapKey().orElse(null)?.location() } }
            .orElse(emptyList())

        val title = CaracatronSpawnText.biomeName(tag).copy().withStyle(ChatFormatting.BOLD)
        if (biomes.isEmpty()) return listOf(title, Component.translatable("$KEY.empty").withStyle(ChatFormatting.GRAY))
        return listOf(title) + biomeLines(biomes)
    }

    /**
     * Names biomes one per line, each followed by where it comes from, vanilla biomes first, then each source in
     * alphabetical order.
     *
     * @param biomes Biome identifiers.
     * @return One line per biome, e.g. "Birch Forest (Vanilla)".
     */
    fun biomeLines(biomes: List<ResourceLocation>): List<Component> {
        return biomes
            .map { biome -> Biome(CaracatronSpawnText.biomeName(biome.toString()), sourceName(biome.namespace), biome.namespace == VANILLA) }
            .sortedWith(compareBy<Biome>({ !it.isVanilla }, { it.source.lowercase() }, { it.name.string }))
            .map { biome ->
                val source = if (biome.isVanilla) Component.translatable("$KEY.vanilla") else Component.literal(biome.source)
                Component.translatable("$KEY.entry", biome.name, source.withStyle(ChatFormatting.GRAY))
            }
    }

    /**
     * Shortens a tooltip that wouldn't fit on screen, keeping its first lines and ending with how many biomes were left
     * out.
     *
     * @param lines Lines from [tooltip].
     * @param maxLines Most lines that fit on screen.
     * @return The lines to show.
     */
    fun fit(lines: List<Component>, maxLines: Int): List<Component> {
        if (lines.size <= maxLines) return lines
        val shown = lines.take((maxLines - 1).coerceAtLeast(MIN_SHOWN_BIOMES + 1))
        return shown + Component.translatable("$KEY.more", lines.size - shown.size).withStyle(ChatFormatting.GRAY)
    }

    /**
     * Names where a biome comes from: the mod owning its namespace, or the namespace itself with capitalized words.
     */
    private fun sourceName(namespace: String): String {
        FabricLoader.getInstance().getModContainer(namespace).orElse(null)?.let { return it.metadata.name }
        return namespace.split('_', '-')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.ROOT) } }
    }
}
