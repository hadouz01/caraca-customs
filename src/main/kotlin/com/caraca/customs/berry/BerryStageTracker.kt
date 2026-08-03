package com.caraca.customs.berry

import net.minecraft.core.BlockPos
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe memory cache tracking the most recent stage growth ticks for berry bush block instances.
 *
 * Used during tick calculations to evaluate conditional bounds based on previous growth performance.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object BerryStageTracker {

    private val lastStageTicksMap = ConcurrentHashMap<BlockPos, Int>()

    /**
     * Retrieves the last calculated ticks for a berry block at the given position.
     *
     * @param pos World coordinate position of the berry block.
     * @return Last tick count generated, or `null` if no prior stage has been tracked.
     */
    fun getLastTicks(pos: BlockPos): Int? = lastStageTicksMap[pos]

    /**
     * Remaps or records the generated tick duration for a specific block position.
     *
     * @param pos World coordinate position of the berry block.
     * @param ticks Calculated growth tick duration.
     */
    fun setLastTicks(pos: BlockPos, ticks: Int) {
        lastStageTicksMap[pos] = ticks
    }

    /**
     * Purges tracked entry for a block position when broken or cleared.
     *
     * @param pos Target block position to remove from tracking.
     */
    fun remove(pos: BlockPos) {
        lastStageTicksMap.remove(pos)
    }
}