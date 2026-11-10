package com.caraca.customs.progression

import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.pokemon.ExperienceGainedEvent
import com.cobblemon.mod.common.api.events.pokemon.PokedexDataChangedEvent
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress
import com.cobblemon.mod.common.api.pokemon.experience.CandyExperienceSource
import com.cobblemon.mod.common.api.pokemon.experience.CommandExperienceSource
import com.cobblemon.mod.common.util.getPlayer
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

/**
 * Enforces the level caps of [PlayerTiers] and tells players when they reach a new tier.
 *
 * Experience that would push a player-owned Pokémon past its owner's level cap is trimmed so it stops right at the cap,
 * and blocked entirely once it gets there. Experience given by commands (like `/levelup`) ignores the cap, and setting
 * the level directly (like `/pokeedit`) never goes through experience at all.
 *
 * Every new species registered as caught is also recorded in the [SharedPokedex]. When it grows the shared pool, every
 * online player who shares tiers is refreshed and told who registered it.
 *
 * @author hadouz01
 * @since 1.4.0
 */
object LevelCapHandler {

    /**
     * Subscribes to Cobblemon's experience and Pokédex events and tracks online players' tiers. Joining players also
     * contribute the species they already own to the [SharedPokedex].
     */
    fun register() {
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe(Priority.LOW, ::onExperienceGained)
        CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe(Priority.NORMAL, ::onPokedexChanged)

        ServerPlayConnectionEvents.JOIN.register { handler, _, server ->
            val player = handler.player
            SharedPokedex.refreshBlocklist()
            val addedToPool = SharedPokedex.backfill(player.uuid)
            PlayerTiers.refreshTier(player.uuid)
            if (addedToPool && PlayerTiers.sharesTier(player.uuid)) {
                refreshSharedTiers(server, player.name)
            }
        }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            PlayerTiers.forgetTier(handler.player.uuid)
        }
    }

    /**
     * Recomputes the tiers of every online player without notifying them, used after the config is reloaded.
     *
     * @param server Running server.
     */
    fun refreshTiers(server: MinecraftServer) {
        SharedPokedex.refreshBlocklist()
        server.playerList.players.forEach { player ->
            PlayerTiers.refreshTier(player.uuid)
        }
    }

    private fun onExperienceGained(event: ExperienceGainedEvent.Pre) {
        if (!DynamicConfig.data.levelCapEnabled) return
        if (event.source is CommandExperienceSource) return

        val pokemon = event.pokemon
        if (!pokemon.isPlayerOwned()) return
        val ownerId = pokemon.getOwnerUUID() ?: return

        val info = PlayerTiers.infoFor(ownerId)
        val levelCap = info.levelCap
        val owner = pokemon.getOwnerPlayer()

        if (pokemon.level >= levelCap) {
            event.cancel()
            if (event.source is CandyExperienceSource) {
                owner?.let { notifyCapReached(it, pokemon.getDisplayName(), info) }
            }
            return
        }

        val experienceToCap = pokemon.experienceGroup.getExperience(levelCap) - pokemon.experience
        if (event.experience >= experienceToCap) {
            event.experience = experienceToCap.coerceAtLeast(0)
            owner?.let { notifyCapReached(it, pokemon.getDisplayName(), info) }
        }
    }

    private fun onPokedexChanged(event: PokedexDataChangedEvent.Post) {
        if (event.knowledge != PokedexEntryProgress.OWNED) return

        val speciesId = event.dataSource.pokemon.species.resourceIdentifier
        val addedToPool = SharedPokedex.contribute(event.playerUUID, speciesId)
        val catcher = event.playerUUID.getPlayer()

        if (addedToPool && catcher != null && PlayerTiers.sharesTier(catcher.uuid)) {
            refreshSharedTiers(catcher.server, catcher.name)
            return
        }

        val info = PlayerTiers.infoFor(event.playerUUID)
        val previousTier = PlayerTiers.refreshTier(event.playerUUID) ?: return
        if (info.tier <= previousTier) return

        catcher?.let { notifyTierUp(it, info) }
    }

    /**
     * Recomputes the tiers of every online player who shares tiers after the shared pool grew, telling each one who went
     * up who registered the new species.
     *
     * @param server Running server.
     * @param catcherName Name of the player who registered the species.
     */
    private fun refreshSharedTiers(server: MinecraftServer, catcherName: Component) {
        server.playerList.players
            .filter { PlayerTiers.sharesTier(it.uuid) }
            .forEach { player ->
                val previousTier = PlayerTiers.refreshTier(player.uuid) ?: return@forEach
                val info = PlayerTiers.infoFor(player.uuid)
                if (info.tier > previousTier) notifySharedTierUp(player, catcherName, info)
            }
    }

    /**
     * Tells the owner a Pokémon hit the level cap, with the species still missing for the next tier and the level cap it
     * unlocks.
     */
    private fun notifyCapReached(player: ServerPlayer, pokemonName: Component, info: TierInfo) {
        val nextCaptures = info.nextTierCaptures
        val nextLevelCap = info.nextLevelCap
        val message = if (nextCaptures == null || nextLevelCap == null) {
            Component.translatable("text.caraca_customs.level_cap.reached_max", pokemonName, info.levelCap)
        } else {
            val missing = (nextCaptures - info.caughtSpecies).coerceAtLeast(1)
            Component.translatable("text.caraca_customs.level_cap.reached", pokemonName, info.levelCap, missing, info.tier + 1, nextLevelCap)
        }
        player.sendSystemMessage(message.withStyle(ChatFormatting.YELLOW))
    }

    private fun notifyTierUp(player: ServerPlayer, info: TierInfo) {
        val message = if (DynamicConfig.data.levelCapEnabled) {
            Component.translatable("text.caraca_customs.tier.up", info.tier, info.tierCount, info.levelCap)
        } else {
            Component.translatable("text.caraca_customs.tier.up_no_cap", info.tier, info.tierCount)
        }
        player.sendSystemMessage(message.withStyle(ChatFormatting.GREEN))
    }

    private fun notifySharedTierUp(player: ServerPlayer, catcherName: Component, info: TierInfo) {
        val message = if (DynamicConfig.data.levelCapEnabled) {
            Component.translatable("text.caraca_customs.tier.up_shared", catcherName, info.tier, info.tierCount, info.levelCap)
        } else {
            Component.translatable("text.caraca_customs.tier.up_shared_no_cap", catcherName, info.tier, info.tierCount)
        }
        player.sendSystemMessage(message.withStyle(ChatFormatting.GREEN))
    }
}
