package com.caraca.customs.network

import com.caraca.customs.client.gui.caracatron.CaracatronScreen
import com.caraca.customs.config.DynamicConfig
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

/**
 * Client-side network manager responsible for receiving configuration payloads and the Caracatron 3000 screen data
 * from the server, updating the local in-memory representation and opening or filling the Caracatron screen.
 *
 * The client's own configuration file is reloaded on disconnect so server values
 * never leak into later singleplayer sessions or other servers.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object ClientNetworkHandler {

    /**
     * Binds the configuration sync and Caracatron receivers and the disconnect restore hook.
     */
    fun register() {
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE) { payload, context ->
            context.client().execute {
                DynamicConfig.loadFromJsonString(payload.configJson)
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(CaracatronOpenPayload.TYPE) { payload, context ->
            context.client().execute {
                CaracatronScreen.open(payload)
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(CaracatronSpawnInfoPayload.TYPE) { payload, context ->
            context.client().execute {
                (context.client().screen as? CaracatronScreen)?.receiveSpawnInfo(payload)
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(CaracatronMaterialInfoPayload.TYPE) { payload, context ->
            context.client().execute {
                (context.client().screen as? CaracatronScreen)?.receiveMaterialInfo(payload)
            }
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, client ->
            client.execute {
                DynamicConfig.load()
            }
        }
    }
}
