package com.caraca.customs.berry

import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.block.entity.BerryBlockEntity
import org.slf4j.LoggerFactory
import java.lang.reflect.Field

/**
 * High-performance reflection bridge managing private state mutations inside Cobblemon's [BerryBlockEntity].
 *
 * Bypasses Mixin bytecode accessor conflicts by reflectively assigning calculated dynamic yield values
 * directly into the target block entity instance.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object BerryYieldInjector {

    private val LOGGER = LoggerFactory.getLogger("caraca_customs/BerryYieldInjector")

    /**
     * Reflective reference to the backing `yield` field in [BerryBlockEntity].
     */
    private val yieldField: Field? by lazy {
        runCatching {
            BerryBlockEntity::class.java.getDeclaredField("yield").apply {
                isAccessible = true
            }
        }.getOrElse {
            // Fallback lookup in case of field name variations in bytecode
            BerryBlockEntity::class.java.declaredFields.firstOrNull { it.type == Int::class.javaPrimitiveType }?.apply {
                isAccessible = true
            }
        }
    }

    /**
     * Computes dynamic yield based on [DynamicConfig] and reflectively updates [BerryBlockEntity].
     *
     * @param entity Target [BerryBlockEntity] instance being processed.
     * @return `true` if yield was successfully calculated and injected, `false` otherwise.
     */
    @JvmStatic
    fun injectCustomYield(entity: BerryBlockEntity): Boolean {
        val currentBerry = entity.berry() ?: return false
        val berryId = currentBerry.identifier.toString()
        val config = DynamicConfig.getConfigFor(berryId)

        val minYield = config.minYield
        val maxYield = config.maxYield

        val generatedYield = if (minYield <= maxYield) {
            (minYield..maxYield).random()
        } else {
            minYield
        }

        return applyYieldValue(entity, generatedYield)
    }

    /**
     * Reflectively sets the target yield integer on a [BerryBlockEntity] instance.
     *
     * @param entity Target block entity.
     * @param value Calculated item count.
     * @return `true` if set operation succeeded.
     */
    private fun applyYieldValue(entity: BerryBlockEntity, value: Int): Boolean {
        val targetField = yieldField ?: return false

        return runCatching {
            targetField.set(entity, value)
            true
        }.getOrElse { exception ->
            LOGGER.error("Failed to inject yield value '$value' into BerryBlockEntity: ${exception.message}")
            false
        }
    }
}