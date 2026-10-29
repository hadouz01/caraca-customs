package com.caraca.customs.progression

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID

/**
 * World-saved record of which players caught each species, used by player-shared tiering.
 *
 * Only species and their catchers are kept, none of the forms, genders or other details of Cobblemon's Pokédex.
 * Saved in `<world>/data/caraca_customs_shared_pokedex.dat`.
 *
 * @property species Map of species identifiers to the UUIDs of every player who caught them.
 *
 * @author hadouz01
 * @since 1.11.0
 */
class SharedPokedexData(
    val species: MutableMap<ResourceLocation, MutableSet<UUID>> = mutableMapOf()
) : SavedData() {

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val entries = ListTag()
        species.forEach { (speciesId, players) ->
            val entry = CompoundTag()
            entry.putString(SPECIES_KEY, speciesId.toString())

            val playerList = ListTag()
            players.forEach { playerList.add(NbtUtils.createUUID(it)) }
            entry.put(PLAYERS_KEY, playerList)

            entries.add(entry)
        }
        tag.put(ENTRIES_KEY, entries)
        return tag
    }

    companion object {
        private const val ENTRIES_KEY = "entries"
        private const val SPECIES_KEY = "species"
        private const val PLAYERS_KEY = "players"

        /**
         * Factory used by the overworld's data storage to create or load the file.
         */
        val FACTORY: SavedData.Factory<SharedPokedexData> = SavedData.Factory({ SharedPokedexData() }, ::load, null)

        /**
         * Reads the saved species and their catchers, skipping invalid species identifiers.
         *
         * @param tag Root tag of the saved file.
         * @param registries Registry lookup provided by the game.
         * @return The loaded shared Pokédex.
         */
        fun load(tag: CompoundTag, registries: HolderLookup.Provider): SharedPokedexData {
            val data = SharedPokedexData()
            tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND.toInt()).forEach { element ->
                val entry = element as CompoundTag
                val speciesId = ResourceLocation.tryParse(entry.getString(SPECIES_KEY)) ?: return@forEach
                val players = entry.getList(PLAYERS_KEY, Tag.TAG_INT_ARRAY.toInt()).mapTo(mutableSetOf()) { NbtUtils.loadUUID(it) }
                data.species[speciesId] = players
            }
            return data
        }
    }
}
