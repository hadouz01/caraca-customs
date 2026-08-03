package com.caraca.customs.network

import com.caraca.customs.config.DynamicConfig
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

/**
 * Client-side network manager responsible for receiving configuration payloads
 * from the server and updating the local in-memory representation.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object ClientNetworkHandler {

    /**
     * Initializes client networking hooks.
     * Binds the global receiver for configuration synchronization packets.
     */
    fun register() {
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE) { payload, context ->
            // Execute on the main client thread to avoid race conditions when writing to memory
            context.client().execute {
                DynamicConfig.loadFromJsonString(payload.configJson)
            }
        }
    }
}