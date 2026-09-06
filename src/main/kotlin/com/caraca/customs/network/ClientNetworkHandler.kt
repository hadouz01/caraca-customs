package com.caraca.customs.network

import com.caraca.customs.config.DynamicConfig
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

/**
 * Client-side network manager responsible for receiving configuration payloads
 * from the server and updating the local in-memory representation.
 *
 * The client's own configuration file is reloaded on disconnect so server values
 * never leak into later singleplayer sessions or other servers.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object ClientNetworkHandler {

    /**
     * Binds the configuration sync receiver and the disconnect restore hook.
     */
    fun register() {
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE) { payload, context ->
            context.client().execute {
                DynamicConfig.loadFromJsonString(payload.configJson)
            }
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, client ->
            client.execute {
                DynamicConfig.load()
            }
        }
    }
}
