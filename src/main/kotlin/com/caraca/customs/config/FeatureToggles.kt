package com.caraca.customs.config

import com.caraca.CaracaCustoms
import net.minecraft.server.MinecraftServer
import org.slf4j.LoggerFactory

/**
 * Applies [Feature] toggle changes that need more than a config reload.
 *
 * Recipes only follow [Feature.CUSTOM_BALLS] and the Poké Ball materials when datapacks load (see [FeatureConditions]
 * and [com.caraca.customs.balls.BallMaterials]), so changing them reloads the server's datapacks, the same as `/reload`.
 * Every other toggle is read live.
 *
 * @author hadouz01
 * @since 1.7.0
 */
object FeatureToggles {

    private val LOGGER = LoggerFactory.getLogger(CaracaCustoms.MOD_ID)

    /**
     * Snapshot of the settings that need extra work when changed, taken before the config is reloaded or edited.
     *
     * @property customBallsEnabled Value of [ConfigData.customBallsEnabled] at the time of the snapshot.
     * @property regularBallMaterial Value of [ConfigData.regularBallMaterial].
     * @property ultraBallMaterial Value of [ConfigData.ultraBallMaterial].
     * @property ultraBallCornerFill Value of [ConfigData.ultraBallCornerFill].
     */
    data class Snapshot(
        val customBallsEnabled: Boolean,
        val regularBallMaterial: String,
        val ultraBallMaterial: String,
        val ultraBallCornerFill: String
    )

    /**
     * Takes a snapshot of the current settings.
     *
     * @return The current [Snapshot].
     */
    fun snapshot(): Snapshot {
        val data = DynamicConfig.data
        return Snapshot(data.customBallsEnabled, data.regularBallMaterial, data.ultraBallMaterial, data.ultraBallCornerFill)
    }

    /**
     * Reloads the server's datapacks if the custom Poké Balls toggle or their materials changed since the snapshot.
     *
     * @param server Running server.
     * @param before Snapshot taken before the change.
     * @return Whether a datapack reload was started.
     */
    fun applyChanges(server: MinecraftServer, before: Snapshot): Boolean {
        if (before == snapshot()) return false

        LOGGER.info("Custom Poké Ball settings changed, reloading datapacks to update recipes")
        server.reloadResources(server.packRepository.selectedIds).exceptionally { throwable ->
            LOGGER.error("Failed to reload datapacks after changing the custom Poké Ball settings", throwable)
            null
        }
        return true
    }
}
