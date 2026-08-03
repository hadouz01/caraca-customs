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
 * Directs runtime modifications to private growth fields within Cobblemon's internal berry registry
 * to ensure dynamic growth bounds take effect cleanly across world reloads.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object BerryGrowthModifier {

    private val LOGGER = LoggerFactory.getLogger("caraca_customs/BerryGrowthModifier")

    /**
     * Conversion factor mapping standard time minutes to Minecraft engine game ticks.
     */
    private const val TICKS_PER_MINUTE = 1200

    /**
     * Ticks offset subtracted from the calculated lower bound duration.
     */
    private const val LOWER_BOUND_OFFSET = 1000

    /**
     * Reflective reference to private `growthTime` field in Cobblemon's [Berry] class.
     */
    private val growthTimeField: Field by lazy {
        Berry::class.java.getDeclaredField("growthTime").apply { isAccessible = true }
    }

    /**
     * Registers server lifecycle listeners to ensure berry parameters persist across world loads.
     */
    fun register() {
        ServerLifecycleEvents.SERVER_STARTED.register {
            applyGrowthConfigurations()
        }
        applyGrowthConfigurations()
    }

    /**
     * Iterates through all registered berries in [Berries] and updates their internal growth range.
     *
     * Range bounds in ticks:
     * - If `minGrowthTime == maxGrowthTime`: Upper bound is halved (`(time * 1200) / 2`).
     * - Lower bound: `upperBound - 1000` (Coerced to at least 1 tick).
     */
    fun applyGrowthConfigurations() {
        Berries.all().forEach { berry ->
            val berryId = berry.identifier.toString()
            val config = DynamicConfig.getConfigFor(berryId)

            val safeMinGrowthMinutes = config.minGrowthTime.coerceAtLeast(1)
            val safeMaxGrowthMinutes = config.maxGrowthTime.coerceAtLeast(safeMinGrowthMinutes)

            var maxTicks = safeMaxGrowthMinutes * TICKS_PER_MINUTE

            if (safeMinGrowthMinutes == safeMaxGrowthMinutes) {
                maxTicks /= 2
            }

            val minTicks = ((safeMinGrowthMinutes * TICKS_PER_MINUTE) - LOWER_BOUND_OFFSET).coerceAtLeast(1)

            updateGrowthTime(berry, minTicks..maxTicks)
        }
    }

    /**
     * Reflectively modifies the private [Berry.growthTime] property on a target instance.
     *
     * @param berry Target [Berry] instance.
     * @param newRange Validated non-empty [IntRange] in ticks representing randomized growth duration bounds.
     */
    private fun updateGrowthTime(berry: Berry, newRange: IntRange) {
        if (newRange.isEmpty()) return

        runCatching {
            growthTimeField.set(berry, newRange)
        }.onFailure { exception ->
            LOGGER.error("Failed to inject dynamic growth time range for berry '${berry.identifier}': ${exception.message}")
        }
    }
}