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
 * and blocked entirely once it gets there. Experience given by commands (e.g. `/levelup`) ignores the cap, and setting
 * the level directly (e.g. `/pokeedit`) never goes through experience at all.
 *
 * @author hadouz01
 * @since 1.4.0
 */
object LevelCapHandler {

    /**
     * Subscribes to Cobblemon's experience and Pokédex events and tracks online players' tiers.
     */
    fun register() {
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe(Priority.LOW, ::onExperienceGained)
        CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe(Priority.NORMAL, ::onPokedexChanged)

        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            PlayerTiers.refreshTier(handler.player.uuid)
        }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            PlayerTiers.forgetTier(handler.player.uuid)
        }
    }

    /**
     * Recomputes the tiers of every online player without notifying them, used after the config is reloaded.
     *
     * @param server The running server instance.
     */
    fun refreshTiers(server: MinecraftServer) {
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

        val levelCap = PlayerTiers.infoFor(ownerId).levelCap
        val owner = pokemon.getOwnerPlayer()

        if (pokemon.level >= levelCap) {
            event.cancel()
            if (event.source is CandyExperienceSource) {
                owner?.let { notifyCapReached(it, pokemon.getDisplayName(), levelCap) }
            }
            return
        }

        val experienceToCap = pokemon.experienceGroup.getExperience(levelCap) - pokemon.experience
        if (event.experience >= experienceToCap) {
            event.experience = experienceToCap.coerceAtLeast(0)
            owner?.let { notifyCapReached(it, pokemon.getDisplayName(), levelCap) }
        }
    }

    private fun onPokedexChanged(event: PokedexDataChangedEvent.Post) {
        if (event.knowledge != PokedexEntryProgress.OWNED) return

        val info = PlayerTiers.infoFor(event.playerUUID)
        val previousTier = PlayerTiers.refreshTier(event.playerUUID) ?: return
        if (info.tier <= previousTier) return

        val player = event.playerUUID.getPlayer() ?: return
        notifyTierUp(player, info)
    }

    private fun notifyCapReached(player: ServerPlayer, pokemonName: Component, levelCap: Int) {
        player.sendSystemMessage(
            Component.translatable("text.caraca_customs.level_cap.reached", pokemonName, levelCap)
                .withStyle(ChatFormatting.YELLOW)
        )
    }

    private fun notifyTierUp(player: ServerPlayer, info: TierInfo) {
        val message = if (DynamicConfig.data.levelCapEnabled) {
            Component.translatable("text.caraca_customs.tier.up", info.tier, info.tierCount, info.levelCap)
        } else {
            Component.translatable("text.caraca_customs.tier.up_no_cap", info.tier, info.tierCount)
        }
        player.sendSystemMessage(message.withStyle(ChatFormatting.GREEN))
    }
}
