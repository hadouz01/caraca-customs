package com.caraca.customs.client.gui.caracatron

import java.awt.Color

/**
 * Tier colors of the Caracatron, from yellow green in tier 1 to red in the last one ([BASE]). Past six tiers the colors
 * get stretched and blended in HSB, since an RGB blend gets muddy between blue and magenta.
 *
 * @author hadouz01
 * @since 1.15.0
 */
object CaracatronTierColors {

    private val BASE = intArrayOf(0xAAB900, 0x03A216, 0x02B47F, 0x0094D2, 0xFF00FF, 0xE70036)

    private val BASE_HSB: List<FloatArray> = BASE.map { rgb ->
        Color.RGBtoHSB((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF, null)
    }

    /**
     * Gets a tier's color.
     *
     * @param tier Tier, from 1 to [tierCount].
     * @param tierCount Amount of tiers configured.
     * @return The color as RGB, without alpha.
     */
    fun colorOf(tier: Int, tierCount: Int): Int {
        if (tierCount <= BASE.size) return BASE[tier.coerceIn(1, BASE.size) - 1]

        val position = (tier.coerceIn(1, tierCount) - 1).toFloat() / (tierCount - 1) * BASE.lastIndex
        val index = position.toInt().coerceAtMost(BASE.lastIndex - 1)
        val progress = position - index
        val from = BASE_HSB[index]
        val to = BASE_HSB[index + 1]

        return Color.HSBtoRGB(
            lerp(from[0], to[0], progress),
            lerp(from[1], to[1], progress),
            lerp(from[2], to[2], progress)
        ) and 0xFFFFFF
    }

    private fun lerp(from: Float, to: Float, progress: Float): Float = from + (to - from) * progress
}
