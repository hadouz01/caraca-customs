package com.caraca.customs.client.gui.caracatron

import com.cobblemon.mod.common.api.drop.ItemDropEntry
import com.cobblemon.mod.common.pokemon.Species
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation

/**
 * Item drops of a species' form, or of the species itself.
 *
 * @property formName Name of the form when its drops differ from the species', `null` for the species' own drops.
 * @property entries Item drops, in the order of the drop table.
 *
 * @author hadouz01
 * @since 1.26.0
 */
data class CaracatronDropGroup(val formName: String?, val entries: List<ItemDropEntry>)

/**
 * Item search of the drops page, built from the drop tables Cobblemon already syncs with the species, so it never asks
 * the server anything.
 *
 * Each dropped item is indexed once with its name and id, along with the species that drop it, so a search only walks a
 * few hundred items.
 *
 * @param species Listed species, in the order of the Caracatron's list.
 * @param normalize Lowercases a text and strips its accents, the same way species names are searched.
 *
 * @author hadouz01
 * @since 1.26.0
 */
class CaracatronDropIndex(species: List<Species>, normalize: (String) -> String) {

    /**
     * An indexed item.
     *
     * @property id Item id.
     * @property searchText Translated name and id, both lowercase.
     * @property holders Positions in the species list of the species that drop it.
     */
    private class IndexedItem(val id: ResourceLocation, val searchText: String, val holders: IntArray)

    /**
     * Result of a search.
     *
     * @property species Positions in the species list of the species that drop a matching item.
     * @property items Matching items.
     */
    class Result(val species: BooleanArray, val items: Set<ResourceLocation>)

    private val groups: List<List<CaracatronDropGroup>> = species.map(::groupsOf)

    private val items: List<IndexedItem>

    init {
        val holders = LinkedHashMap<ResourceLocation, MutableList<Int>>()
        groups.forEachIndexed { index, speciesGroups ->
            speciesGroups.asSequence().flatMap { it.entries }.map { it.item }.distinct().forEach { item ->
                holders.getOrPut(item) { mutableListOf() } += index
            }
        }
        items = holders.map { (id, positions) ->
            val name = BuiltInRegistries.ITEM.getOptional(id).map { it.description.string }.orElse(id.path)
            IndexedItem(id, "${normalize(name)}\n$id", positions.toIntArray())
        }
    }

    private val speciesCount = species.size
    private val holderCounts: Map<ResourceLocation, Int> = items.associate { it.id to it.holders.size }
    private var lastQuery: String? = null
    private var lastResult: Result? = null

    /**
     * Gets the drops of the species at a position of the species list.
     *
     * @param index Position in the species list.
     */
    fun dropsAt(index: Int): List<CaracatronDropGroup> = groups.getOrElse(index) { emptyList() }

    /**
     * Counts the listed species that drop an item, for the materials page.
     *
     * @param item Item identifier.
     * @since 1.27.0
     */
    fun holderCount(item: ResourceLocation): Int = holderCounts[item] ?: 0

    /**
     * Scores how much of the given items a species drops: the best of its matching drops, by average amount times chance,
     * so a sure single drop and a rare stack can be compared.
     *
     * @param index Position in the species list.
     * @param items Searched items.
     */
    fun bestDrop(index: Int, items: Set<ResourceLocation>): Double {
        return dropsAt(index).asSequence()
            .flatMap { it.entries }
            .filter { it.item in items }
            .maxOfOrNull { entry ->
                val range = entry.quantityRange
                val amount = if (range != null) (range.first + range.last) / 2.0 else entry.quantity.toDouble()
                amount * entry.percentage
            } ?: 0.0
    }

    /**
     * Finds the items whose name or id contains the query, and the species that drop them. The last search is kept, so
     * redrawing or refiltering with the same text costs nothing.
     *
     * @param query Search text, already lowercase and without accents.
     */
    fun search(query: String): Result {
        if (query == lastQuery) lastResult?.let { return it }
        val marked = BooleanArray(speciesCount)
        val matched = HashSet<ResourceLocation>()
        for (item in items) {
            if (!item.searchText.contains(query)) continue
            matched += item.id
            for (holder in item.holders) marked[holder] = true
        }
        return Result(marked, matched).also {
            lastQuery = query
            lastResult = it
        }
    }

    /**
     * Groups a species' item drops: its standard form's first, then every other form with its own drop table, like
     * regional forms.
     */
    private fun groupsOf(species: Species): List<CaracatronDropGroup> {
        val base = species.standardForm.drops
        val groups = mutableListOf(CaracatronDropGroup(null, itemDrops(base.entries)))
        species.forms.filter { it.drops !== base && it.name != species.standardForm.name }.forEach { form ->
            groups += CaracatronDropGroup(form.name, itemDrops(form.drops.entries))
        }
        return groups.filter { it.formName == null || it.entries.isNotEmpty() }
    }

    private fun itemDrops(entries: List<*>): List<ItemDropEntry> = entries.filterIsInstance<ItemDropEntry>()
}
