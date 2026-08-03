package com.caraca.customs.mulch

import com.cobblemon.mod.common.api.mulch.MulchVariant

/**
 * Interface contract defining custom mulch harvest tracking capabilities
 * injected into target Cobblemon block entities.
 *
 * @author hadouz01
 * @since 1.0.0
 */
interface MulchTracker {
    /**
     * Retrieves the active [MulchVariant] applied to the block entity.
     *
     * @return Current active mulch variant, or [MulchVariant.NONE].
     */
    val mulch: MulchVariant?

    /**
     * Retrieves the remaining harvest uses allowed by current mulch application.
     *
     * @return Remaining harvest count.
     */
    fun `caraca$getMulchHarvests`(): Int

    /**
     * Updates the active harvest usages counter for applied mulch.
     *
     * @param uses Target remaining uses count.
     */
    fun `caraca$setMulchHarvests`(uses: Int)

    /**
     * Consumes one harvest usage unit for the active mulch layer.
     * Clears mulch state when usages exhaust.
     *
     * @return True if mulch was fully consumed and removed, false otherwise.
     */
    fun `caraca$consumeMulchHarvest`(): Boolean
}