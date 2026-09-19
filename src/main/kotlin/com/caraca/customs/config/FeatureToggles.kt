package com.caraca.customs.config

import com.caraca.CaracaCustoms
import net.minecraft.server.MinecraftServer
import org.slf4j.LoggerFactory

/**
 * Applies [Feature] toggle changes that need more than a config reload.
 *
 * Recipes only follow [Feature.CUSTOM_BALLS] when datapacks load (see [FeatureConditions]), so flipping it reloads the
 * server's datapacks, the same as `/reload`. Every other toggle is read live.
 *
 * @author hadouz01
 * @since 1.7.0
 */
object FeatureToggles {

    private val LOGGER = LoggerFactory.getLogger(CaracaCustoms.MOD_ID)

    /**
     * Snapshot of the toggles that need extra work when changed, taken before the config is reloaded or edited.
     *
     * @property customBallsEnabled Value of [ConfigData.customBallsEnabled] at the time of the snapshot.
     */
    data class Snapshot(val customBallsEnabled: Boolean)

    /**
     * Takes a snapshot of the current toggles.
     *
     * @return The current [Snapshot].
     */
    fun snapshot(): Snapshot = Snapshot(DynamicConfig.data.customBallsEnabled)

    /**
     * Reloads the server's datapacks if the custom Poké Balls toggle changed since the snapshot.
     *
     * @param server The running server instance.
     * @param before Snapshot taken before the change.
     * @return Whether a datapack reload was started.
     */
    fun applyChanges(server: MinecraftServer, before: Snapshot): Boolean {
        if (before.customBallsEnabled == DynamicConfig.data.customBallsEnabled) return false

        LOGGER.info("Custom Poké Balls were turned {}, reloading datapacks to update recipes", if (DynamicConfig.data.customBallsEnabled) "on" else "off")
        server.reloadResources(server.packRepository.selectedIds).exceptionally { throwable ->
            LOGGER.error("Failed to reload datapacks after toggling the custom Poké Balls", throwable)
            null
        }
        return true
    }
}
