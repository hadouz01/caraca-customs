package com.caraca.customs.mulch

/**
 * Mulch use counter added to Cobblemon's `BerryBlockEntity`, so any mulch lasts [MULCH_HARVEST_USES] yield cycles. The
 * mulch itself is still Cobblemon's `mulchVariant`.
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
