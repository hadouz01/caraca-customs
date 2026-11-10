package com.caraca.customs.network

import com.caraca.customs.config.DynamicConfig
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

/**
 * Server side of the mod's packets: registers them and sends the config to players.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object ServerNetworkHandler {

    /**
     * Registers the config sync and Caracatron payload types and sends the active configuration to every joining player.
     */
    fun register() {
        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(CaracatronOpenPayload.TYPE, CaracatronOpenPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(CaracatronSpawnInfoPayload.TYPE, CaracatronSpawnInfoPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(CaracatronSpawnRequestPayload.TYPE, CaracatronSpawnRequestPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(CaracatronMaterialInfoPayload.TYPE, CaracatronMaterialInfoPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(CaracatronMaterialRequestPayload.TYPE, CaracatronMaterialRequestPayload.CODEC)

        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            sendConfig(handler.player)
        }
    }

    /**
     * Sends the active configuration to every connected player, used after a hot reload.
     *
     * @param server Running server.
     */
    fun broadcastConfig(server: MinecraftServer) {
        val payload = createPayload()
        server.playerList.players
            .filter { ServerPlayNetworking.canSend(it, ConfigSyncPayload.TYPE) }
            .forEach { ServerPlayNetworking.send(it, payload) }
    }

    private fun sendConfig(player: ServerPlayer) {
        if (ServerPlayNetworking.canSend(player, ConfigSyncPayload.TYPE)) {
            ServerPlayNetworking.send(player, createPayload())
        }
    }

    private fun createPayload(): ConfigSyncPayload = ConfigSyncPayload(DynamicConfig.serializeToString())
}
