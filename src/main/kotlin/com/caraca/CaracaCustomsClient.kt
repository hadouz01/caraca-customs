package com.caraca

import com.caraca.customs.network.ClientNetworkHandler
import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

/**
 * Client-side entrypoint for the `caraca_customs` Fabric mod.
 *
 * Implements [ClientModInitializer] to hook into Fabric's client-specific lifecycle phase,
 * ensuring network receivers and tooltip handlers are registered early.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CaracaCustomsClient : ClientModInitializer {

    private val LOGGER = LoggerFactory.getLogger(CaracaCustoms.MOD_ID + "_client")

    /**
     * Called by Fabric Loader during client initialization.
     */
    override fun onInitializeClient() {
        ClientNetworkHandler.register()

        // You can also initialize your CustomBallTooltipHandler here
        // CustomBallTooltipHandler.register()

        LOGGER.info("Client networking and tooltips initialized successfully!")
    }
}