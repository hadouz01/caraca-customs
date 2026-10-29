package com.caraca.customs.client.gui.caracatron

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

/**
 * Item search of the Caracatron's materials page, over every registered item (other mods' included), built on the
 * client the first time the page searches and kept between Caracatron screens ([shared]) until the game's language
 * or item count changes.
 *
 * Each item is indexed with its name in the game's language (lowercase, without accents) and its id, so "ferr" finds
 * "Ferro bruto" and "minecraft:iro" finds every Minecraft item whose id starts like that. Searches shorter than
 * [MIN_QUERY_LENGTH] list nothing, which keeps the list empty until the player starts typing. A search containing the
 * previous one (the player kept typing) only looks through the previous results.
 *
 * @param normalize Lowercases a text and strips its accents, the same way species names are searched.
 *
 * @author hadouz01
 * @since 1.27.0
 */
class CaracatronMaterialIndex private constructor(normalize: (String) -> String) {

    private class IndexedItem(val item: Item, val searchText: String)

    private val items: List<IndexedItem> = BuiltInRegistries.ITEM
        .filter { it != Items.AIR }
        .map { IndexedItem(it, "${normalize(it.description.string)}\n${BuiltInRegistries.ITEM.getKey(it)}") }

    private var lastQuery: String? = null
    private var lastMatches: List<IndexedItem> = emptyList()
    private var lastResult: List<Item> = emptyList()

    /**
     * Finds the items whose name or id contains the query, in registry order. The last search is kept.
     *
     * @param query Search text, already lowercase and without accents.
     * @return The matching items, none when the query is too short.
     */
    fun search(query: String): List<Item> {
        if (query.length < MIN_QUERY_LENGTH) return emptyList()
        val previous = lastQuery
        if (query == previous) return lastResult
        val pool = if (previous != null && query.contains(previous)) lastMatches else items
        val matches = pool.filter { query in it.searchText }
        lastQuery = query
        lastMatches = matches
        lastResult = matches.map { it.item }
        return lastResult
    }

    companion object {
        /**
         * Shortest search that lists items.
         */
        const val MIN_QUERY_LENGTH = 2

        private var cached: CaracatronMaterialIndex? = null
        private var cachedLanguage: String? = null
        private var cachedSize = -1

        /**
         * Gets the index, building it again only when the game's language or the number of items changed since it was
         * last built.
         *
         * @param normalize Lowercases a text and strips its accents.
         */
        fun shared(normalize: (String) -> String): CaracatronMaterialIndex {
            val language = Minecraft.getInstance().languageManager.selected
            val size = BuiltInRegistries.ITEM.size()
            cached?.takeIf { language == cachedLanguage && size == cachedSize }?.let { return it }
            return CaracatronMaterialIndex(normalize).also {
                cached = it
                cachedLanguage = language
                cachedSize = size
            }
        }
    }
}
