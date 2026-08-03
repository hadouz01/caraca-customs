package com.caraca.customs.network

import com.caraca.customs.config.DynamicConfig
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

/**
 * Server-side network manager responsible for registering packet types
 * and dispatching configuration payloads to players upon joining.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object ServerNetworkHandler {

    /**
     * Initializes server networking hooks.
     * Registers the S2C (Server-To-Client) payload type and binds the join event listener.
     */
    fun register() {
        // Registers the payload type and codec with Fabric's Play S2C registry
        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.CODEC)

        // Dispatches the current server configuration state to the client when they connect
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val jsonString = DynamicConfig.serializeToString()
            val payload = ConfigSyncPayload(jsonString)

            ServerPlayNetworking.send(handler.player, payload)
        }
    }
}