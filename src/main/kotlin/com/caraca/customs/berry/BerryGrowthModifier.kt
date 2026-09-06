package com.caraca.customs.berry

import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.api.berry.Berries
import com.cobblemon.mod.common.api.berry.Berry
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import org.slf4j.LoggerFactory
import java.lang.reflect.Field

/**
 * Reflection utility updating registered [Berry] growth parameters at startup and runtime.
 *
 * Writes the configured growth range (in minutes, the unit Cobblemon expects) into each berry's
 * `growthTime`, keeping Cobblemon's own timers and the data synced to clients consistent with the
 * configuration. Stage durations themselves are calculated by `BerryBlockEntityGrowthMixin`.
 *
 * Berries are datapack-driven, so the values are re-applied after every server start and datapack reload.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object BerryGrowthModifier {

    private val LOGGER = LoggerFactory.getLogger("caraca_customs/BerryGrowthModifier")

    private val growthTimeField: Field by lazy {
        Berry::class.java.getDeclaredField("growthTime").apply { isAccessible = true }
    }

    /**
     * Registers server lifecycle listeners re-applying berry parameters whenever berries are (re)loaded.
     */
    fun register() {
        ServerLifecycleEvents.SERVER_STARTED.register { applyGrowthConfigurations() }
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { _, _, success ->
            if (success) applyGrowthConfigurations()
        }
    }

    /**
     * Updates the growth range of every registered berry using its resolved configuration.
     */
    fun applyGrowthConfigurations() {
        Berries.all().forEach { berry ->
            val config = DynamicConfig.getConfigFor(berry.identifier.toString())
            val minMinutes = config.minGrowthTime.coerceAtLeast(1)
            val maxMinutes = config.maxGrowthTime.coerceAtLeast(minMinutes)

            updateGrowthTime(berry, minMinutes..maxMinutes)
        }
    }

    private fun updateGrowthTime(berry: Berry, newRange: IntRange) {
        runCatching {
            growthTimeField.set(berry, newRange)
        }.onFailure { exception ->
            LOGGER.error("Failed to inject dynamic growth time range for berry '${berry.identifier}': ${exception.message}")
        }
    }
}
