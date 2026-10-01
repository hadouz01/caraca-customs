package com.caraca.customs.config

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult

/**
 * Mod mechanics that can be turned on and off from the main config page.
 *
 * @property id Identifier used by the `caraca_customs:feature_enabled` recipe and loot conditions.
 *
 * @author hadouz01
 * @since 1.7.0
 */
enum class Feature(val id: String, private val enabled: (ConfigData) -> Boolean) {
    CUSTOM_BALLS("custom_balls", { it.customBallsEnabled }),
    BERRY_MECHANICS("berry_mechanics", { it.berryMechanicsEnabled }),
    LEVEL_CAP("level_cap", { it.levelCapEnabled }),
    SPAWN_CAP("spawn_cap", { it.spawnCapEnabled }),
    FOLLOW_SPEED("follow_speed", { it.dynamicFollowSpeedEnabled }),
    SPAWN_LEVELS("spawn_levels", { it.tierSpawnLevelsEnabled });

    /**
     * Whether this mechanic is currently enabled in the active config.
     */
    val isEnabled: Boolean get() = enabled(DynamicConfig.data)

    companion object {
        private val BY_ID = entries.associateBy { it.id }

        /**
         * Codec reading a feature by its [id].
         */
        val CODEC: Codec<Feature> = Codec.STRING.comapFlatMap(
            { id -> BY_ID[id]?.let { DataResult.success(it) } ?: DataResult.error { "Unknown Caraca Customs feature: $id" } },
            Feature::id
        )

        /**
         * Shortcut used by Java mixins.
         *
         * @return Whether the berry mechanics are enabled.
         */
        @JvmStatic
        fun berriesEnabled(): Boolean = BERRY_MECHANICS.isEnabled
    }
}
