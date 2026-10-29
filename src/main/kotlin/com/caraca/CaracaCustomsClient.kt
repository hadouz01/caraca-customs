package com.caraca

import com.caraca.customs.client.CustomBallTooltipHandler
import com.caraca.customs.client.compat.CatchRateDisplayCompat
import com.caraca.customs.client.render.CaracatronItemModels
import com.caraca.customs.client.scan.CaracatronScanner
import com.caraca.customs.network.ClientNetworkHandler
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory

/**
 * Client-side entrypoint for the `caraca_customs` Fabric mod.
 *
 * Implements [ClientModInitializer] to register configuration sync receivers, Poké Ball tooltips, the Caracatron 3000
 * item models and scanner and, when installed, the `catchrate-display` integration.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CaracaCustomsClient : ClientModInitializer {

    private const val CATCHRATE_DISPLAY_ID = "catchrate-display"

    private val LOGGER = LoggerFactory.getLogger(CaracaCustoms.MOD_ID + "_client")

    /**
     * Called by Fabric Loader during client initialization.
     */
    override fun onInitializeClient() {
        ClientNetworkHandler.register()
        CustomBallTooltipHandler.register()
        CaracatronItemModels.register()
        CaracatronScanner.register()

        if (FabricLoader.getInstance().isModLoaded(CATCHRATE_DISPLAY_ID)) {
            CatchRateDisplayCompat.register()
        }

        LOGGER.info("Client networking and tooltips initialized successfully!")
    }
}
