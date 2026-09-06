package com.caraca.customs.mulch

/**
 * Interface contract injected into Cobblemon's `BerryBlockEntity`, limiting any applied mulch
 * to [MULCH_HARVEST_USES] yield cycles.
 *
 * The active mulch itself is read from Cobblemon's native `BerryBlockEntity.mulchVariant`.
 *
 * @author hadouz01
 * @since 1.0.0
 */
interface MulchTracker {

    /**
     * Consumes one yield cycle from the active mulch, removing it once all cycles are used.
     *
     * @return `true` if the mulch was fully consumed and removed, `false` otherwise.
     */
    fun `caraca$consumeMulchHarvest`(): Boolean

    companion object {
        /**
         * Number of yield cycles a single mulch application lasts.
         */
        const val MULCH_HARVEST_USES: Int = 4
    }
}
