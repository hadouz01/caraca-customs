package com.caraca.customs.client.gui.caracatron

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.ResourceLocation
import java.nio.file.Files

/**
 * What the Caracatron remembers between uses on this client, kept in `config/caraca_customs/caracatron_state.json`
 * apart from the mod config, so a server's settings never touch it: the last page, each page's search, the selected
 * species and item, and their open tabs.
 *
 * @author hadouz01
 * @since 1.34.0
 */
object CaracatronState {

    private const val LAST_PAGE = "lastPage"
    private const val SEARCHES = "searches"
    private const val SPECIES = "species"
    private const val MATERIAL = "material"
    private const val SPAWN_TAB = "spawnTab"
    private const val MATERIAL_TAB = "materialTab"

    private val file = FabricLoader.getInstance().configDir.resolve("caraca_customs").resolve("caracatron_state.json")

    private val json: JsonObject? = runCatching { JsonParser.parseString(Files.readString(file)).asJsonObject }.getOrNull()

    /**
     * Page the Caracatron was on when it was last closed, the one it opens on next time.
     */
    var lastPage: CaracatronPage = CaracatronPage.entries.firstOrNull { it.key == string(LAST_PAGE) } ?: CaracatronPage.CARACA
        set(value) {
            if (field == value) return
            field = value
            save()
        }

    /**
     * Search text of each page the last time the Caracatron was closed.
     */
    var searches: Map<CaracatronPage, String> = readSearches()
        private set

    /**
     * Species selected the last time the Caracatron was closed.
     */
    var species: ResourceLocation? = string(SPECIES)?.let(ResourceLocation::tryParse)
        private set

    /**
     * Item selected on the materials page the last time the Caracatron was closed.
     */
    var material: ResourceLocation? = string(MATERIAL)?.let(ResourceLocation::tryParse)
        private set

    /**
     * Spawn tab open the last time the Caracatron was closed.
     */
    var spawnTab: String? = string(SPAWN_TAB)
        private set

    /**
     * Materials tab open the last time the Caracatron was closed.
     */
    var materialTab: String? = string(MATERIAL_TAB)
        private set

    /**
     * Remembers the searches, selections and tabs of a Caracatron that's closing.
     *
     * @param searches Search text of each page.
     * @param species Selected species.
     * @param material Selected item of the materials page.
     * @param spawnTab Open spawn tab.
     * @param materialTab Open materials tab.
     */
    fun remember(searches: Map<CaracatronPage, String>, species: ResourceLocation?, material: ResourceLocation?, spawnTab: String, materialTab: String) {
        this.searches = searches.filterValues { it.isNotEmpty() }
        this.species = species
        this.material = material
        this.spawnTab = spawnTab
        this.materialTab = materialTab
        save()
    }

    private fun string(key: String): String? = runCatching { json?.get(key)?.asString }.getOrNull()

    private fun readSearches(): Map<CaracatronPage, String> {
        val saved = runCatching { json?.getAsJsonObject(SEARCHES) }.getOrNull() ?: return emptyMap()
        return CaracatronPage.entries.mapNotNull { page -> runCatching { saved.get(page.key)?.asString }.getOrNull()?.let { page to it } }.toMap()
    }

    private fun save() {
        runCatching {
            val out = JsonObject()
            out.addProperty(LAST_PAGE, lastPage.key)
            out.add(SEARCHES, JsonObject().apply { searches.forEach { (page, text) -> addProperty(page.key, text) } })
            species?.let { out.addProperty(SPECIES, it.toString()) }
            material?.let { out.addProperty(MATERIAL, it.toString()) }
            spawnTab?.let { out.addProperty(SPAWN_TAB, it) }
            materialTab?.let { out.addProperty(MATERIAL_TAB, it) }
            Files.createDirectories(file.parent)
            Files.writeString(file, out.toString())
        }
    }
}
