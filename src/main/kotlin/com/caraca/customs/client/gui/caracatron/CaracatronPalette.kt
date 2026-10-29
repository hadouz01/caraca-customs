package com.caraca.customs.client.gui.caracatron

import com.mojang.blaze3d.platform.NativeImage
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import kotlin.math.roundToInt

/**
 * Color map from Cobblemon's Pokédex to a Caracatron screen color, learned from textures both have with the same layout
 * (the screen, its bars, scroll track and type bars): every Pokédex color found in them is mapped to the color most
 * often found at the same pixels of the Caracatron's version.
 *
 * Colors that aren't in the map are taken to the nearest mapped color when it's close enough ([MAX_DISTANCE]), keeping
 * their brightness difference from it, so lighter and darker shades of the Pokédex's cyan follow along. Colors far from
 * every mapped one (type colors, reds, grays) are left as they are.
 *
 * @param pairs Pokédex textures and their Caracatron counterparts.
 *
 * @author hadouz01
 * @since 1.29.0
 */
class CaracatronPalette(pairs: List<Pair<ResourceLocation, ResourceLocation>>) {

    private companion object {
        const val MAX_DISTANCE = 40
        const val MIN_ALPHA = 200
    }

    private val sources: IntArray
    private val targets: IntArray
    private val cache = Int2IntOpenHashMap().apply { defaultReturnValue(-1) }

    init {
        val votes = HashMap<Int, HashMap<Int, Int>>()
        pairs.forEach { (source, target) ->
            val from = read(source) ?: return@forEach
            val to = read(target)
            if (to == null || to.width != from.width || to.height != from.height) {
                from.close()
                to?.close()
                return@forEach
            }
            for (x in 0 until from.width) {
                for (y in 0 until from.height) {
                    val a = from.getPixelRGBA(x, y)
                    val b = to.getPixelRGBA(x, y)
                    if (alpha(a) < MIN_ALPHA || alpha(b) < MIN_ALPHA) continue
                    votes.getOrPut(rgb(a)) { HashMap() }.merge(rgb(b), 1, Int::plus)
                }
            }
            from.close()
            to.close()
        }
        val entries = votes.mapValues { (_, counts) -> counts.maxBy { it.value }.key }.entries.toList()
        sources = IntArray(entries.size) { entries[it].key }
        targets = IntArray(entries.size) { entries[it].value }
    }

    /**
     * Whether the palette learned anything, false when the color's textures were missing or didn't match.
     */
    val isEmpty: Boolean get() = sources.isEmpty()

    /**
     * Maps a Pokédex color to the screen color.
     *
     * @param color RGB color.
     * @return The mapped RGB color, or [color] itself when it isn't close to the Pokédex's colors.
     */
    fun map(color: Int): Int {
        val cached = cache.get(color)
        if (cached >= 0) return cached
        return nearest(color).also { cache.put(color, it) }
    }

    /**
     * Maps a color that isn't cached yet, through the nearest mapped Pokédex color.
     */
    private fun nearest(color: Int): Int {
        var best = -1
        var bestDistance = Int.MAX_VALUE
        for (index in sources.indices) {
            val distance = distance(color, sources[index])
            if (distance < bestDistance) {
                best = index
                bestDistance = distance
            }
            if (distance == 0) break
        }
        if (best < 0 || bestDistance > MAX_DISTANCE * MAX_DISTANCE) return color
        val shift = (luminance(color) - luminance(sources[best])).roundToInt()
        val target = targets[best]
        return (channel(target shr 16, shift) shl 16) or (channel(target shr 8, shift) shl 8) or channel(target, shift)
    }

    /**
     * Recolors an image in place, pixel by pixel, keeping each pixel's alpha.
     *
     * @param image Image in Minecraft's ABGR pixel format.
     * @return Whether any pixel changed.
     */
    fun recolor(image: NativeImage): Boolean {
        var changed = false
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val pixel = image.getPixelRGBA(x, y)
                if (alpha(pixel) == 0) continue
                val color = rgb(pixel)
                val mapped = map(color)
                if (mapped == color) continue
                image.setPixelRGBA(x, y, (pixel and 0xFF000000.toInt()) or abgr(mapped))
                changed = true
            }
        }
        return changed
    }

    private fun read(texture: ResourceLocation): NativeImage? {
        val resource = Minecraft.getInstance().resourceManager.getResource(texture).orElse(null) ?: return null
        return runCatching { resource.open().use { NativeImage.read(it) } }.getOrNull()
    }

    private fun alpha(abgr: Int): Int = abgr ushr 24

    /**
     * Turns an ABGR pixel into an RGB color.
     */
    private fun rgb(abgr: Int): Int = ((abgr and 0xFF) shl 16) or (abgr and 0xFF00) or ((abgr shr 16) and 0xFF)

    /**
     * Turns an RGB color into the BGR part of a pixel.
     */
    private fun abgr(rgb: Int): Int = ((rgb and 0xFF) shl 16) or (rgb and 0xFF00) or ((rgb shr 16) and 0xFF)

    private fun distance(first: Int, second: Int): Int {
        val red = (first shr 16 and 0xFF) - (second shr 16 and 0xFF)
        val green = (first shr 8 and 0xFF) - (second shr 8 and 0xFF)
        val blue = (first and 0xFF) - (second and 0xFF)
        return red * red + green * green + blue * blue
    }

    private fun luminance(rgb: Int): Double {
        return 0.2126 * (rgb shr 16 and 0xFF) + 0.7152 * (rgb shr 8 and 0xFF) + 0.0722 * (rgb and 0xFF)
    }

    private fun channel(value: Int, shift: Int): Int = ((value and 0xFF) + shift).coerceIn(0, 255)
}
