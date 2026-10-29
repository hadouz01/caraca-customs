package com.caraca.customs.caracatron

import com.caraca.CaracaCustoms
import com.caraca.customs.network.CaracatronPastureLoot
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory
import java.nio.file.Files

/**
 * Reads the settings of the Pasture Loot mod (`pasture-loot`), which makes Pokémon in a pasture drop items from their
 * drop table now and then, except the items in its blocklist.
 *
 * Pasture Loot has no API, so its own config file, `config/PastureLoot.json`, is read on every request: `item_blacklist`
 * (item ids the pastures never drop) and `drop_chance_per_minute` (chance each pastured Pokémon drops something each
 * minute). Reading it fresh keeps the Caracatron in line with edits made while the server runs, and the file is tiny.
 *
 * @author hadouz01
 * @since 1.26.0
 */
object PastureLootCompat {

    private const val MOD_ID = "pasture-loot"
    private const val CONFIG_FILE = "PastureLoot.json"
    private const val BLACKLIST_KEY = "item_blacklist"
    private const val CHANCE_KEY = "drop_chance_per_minute"

    private val LOGGER = LoggerFactory.getLogger(CaracaCustoms.MOD_ID)

    /**
     * Reads Pasture Loot's settings.
     *
     * @return The settings, with an empty blocklist and no chance when the file can't be read, or `null` when Pasture
     * Loot isn't installed on the server.
     */
    fun settings(): CaracatronPastureLoot? {
        val loader = FabricLoader.getInstance()
        if (!loader.isModLoaded(MOD_ID)) return null

        val file = loader.configDir.resolve(CONFIG_FILE)
        return runCatching {
            val json = Files.newBufferedReader(file).use { JsonParser.parseReader(it).asJsonObject }
            val blacklist = json.getAsJsonArray(BLACKLIST_KEY)
                ?.mapNotNullTo(HashSet()) { element -> ResourceLocation.tryParse(element.asString.trim()) }
                .orEmpty()
            val chance = json.get(CHANCE_KEY)?.takeIf { it.isJsonPrimitive }?.asDouble ?: -1.0
            CaracatronPastureLoot(chance, blacklist)
        }.getOrElse {
            LOGGER.warn("Couldn't read Pasture Loot's config file {}: {}", file, it.message)
            CaracatronPastureLoot(-1.0, emptySet())
        }
    }
}
