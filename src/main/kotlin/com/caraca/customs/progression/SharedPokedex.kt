package com.caraca.customs.progression

import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import java.util.UUID

/**
 * World Pokédex shared by every player, used by player-shared tiering.
 *
 * Captures are always recorded, even while the mechanic is off, so turning it on later counts everything caught before.
 * A species counts for the shared pool while at least one of its catchers is outside the tier-share blocklist, so
 * taking a player off the list brings their past captures back right away.
 *
 * @author hadouz01
 * @since 1.11.0
 */
object SharedPokedex {

    private const val DATA_NAME = "caraca_customs_shared_pokedex"

    private var server: MinecraftServer? = null
    private var data: SharedPokedexData? = null
    private var cachedCount: Int? = null
    private var blockedIds: Set<UUID> = emptySet()
    private var blockedNames: Set<String> = emptySet()

    /**
     * Loads the world's shared Pokédex when the server starts and drops it when the server stops.
     */
    fun register() {
        ServerLifecycleEvents.SERVER_STARTED.register { startedServer ->
            server = startedServer
            data = startedServer.overworld().dataStorage.computeIfAbsent(SharedPokedexData.FACTORY, DATA_NAME)
            refreshBlocklist()
        }
        ServerLifecycleEvents.SERVER_STOPPED.register {
            server = null
            data = null
            cachedCount = null
            blockedIds = emptySet()
            blockedNames = emptySet()
        }
    }

    /**
     * Records a player as a catcher of a species.
     *
     * @param playerId Player UUID.
     * @param speciesId Species identifier, like `cobblemon:bulbasaur`.
     * @return Whether the species just started counting for the shared pool.
     */
    fun contribute(playerId: UUID, speciesId: ResourceLocation): Boolean {
        val pokedex = data ?: return false
        val catchers = pokedex.species.getOrPut(speciesId) { mutableSetOf() }
        val wasCounted = isCounted(catchers)
        if (!catchers.add(playerId)) return false

        pokedex.setDirty()
        if (wasCounted || !isCounted(catchers)) return false
        cachedCount = null
        return true
    }

    /**
     * Contributes every species a player already owns in Cobblemon's Pokédex, covering captures made before the
     * mechanic existed. Works for offline players too.
     *
     * @param playerId Player UUID.
     * @return Whether any species started counting for the shared pool.
     */
    fun backfill(playerId: UUID): Boolean {
        val records = Cobblemon.playerDataManager.getPokedexData(playerId).speciesRecords
        var addedToPool = false
        records.forEach { (speciesId, record) ->
            if (record.getKnowledge() == PokedexEntryProgress.OWNED && contribute(playerId, speciesId)) {
                addedToPool = true
            }
        }
        return addedToPool
    }

    /**
     * Species caught by at least one player outside the blocklist. Cached, since tiers are checked on every experience
     * gain.
     *
     * @return Amount of species in the shared pool.
     */
    fun sharedCount(): Int {
        cachedCount?.let { return it }
        val count = data?.species?.values?.count(::isCounted) ?: 0
        cachedCount = count
        return count
    }

    /**
     * Species caught by any player other than the given one, blocklisted players included, since it only tells who caught
     * what and has nothing to do with the shared tiers.
     *
     * @param playerId Player to leave out.
     * @return The species identifiers.
     */
    fun caughtByOthers(playerId: UUID): Set<ResourceLocation> {
        val pokedex = data ?: return emptySet()
        return pokedex.species.filterValues { catchers -> catchers.any { it != playerId } }.keys.toSet()
    }

    /**
     * Species in the shared pool, caught by at least one player outside the blocklist.
     *
     * @return The species identifiers counted for the shared tiers.
     */
    fun countedSpecies(): Set<ResourceLocation> {
        val pokedex = data ?: return emptySet()
        return pokedex.species.filterValues(::isCounted).keys.toSet()
    }

    /**
     * Resolves the tier-share blocklist names to UUIDs through the server's profile cache. Called on server start, player
     * join and config reload.
     */
    fun refreshBlocklist() {
        val names = DynamicConfig.data.tierShareBlocklist
        val profiles = server?.profileCache
        blockedNames = names.mapTo(mutableSetOf()) { it.lowercase() }
        blockedIds = names.mapNotNullTo(mutableSetOf()) { name -> profiles?.get(name)?.orElse(null)?.id }
        cachedCount = null
    }

    /**
     * Whether a player is in the tier-share blocklist, also checking an online player's current name.
     *
     * @param playerId Player UUID.
     * @return Whether the player is left out of the shared tiers.
     */
    fun isBlocked(playerId: UUID): Boolean {
        if (playerId in blockedIds) return true
        val name = server?.playerList?.getPlayer(playerId)?.gameProfile?.name ?: return false
        return name.lowercase() in blockedNames
    }

    private fun isCounted(catchers: Set<UUID>): Boolean = catchers.any { it !in blockedIds }
}
