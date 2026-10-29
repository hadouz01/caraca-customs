package com.caraca.customs.client.gui.caracatron

import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.render.drawScaledText
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation

/**
 * Text colors of a Caracatron screen color: white on dark screens and `#191919` on light ones, so the letters stay easy
 * to read on every color.
 *
 * The choice comes from the brightness of the color's own textures, so screen colors added by resource packs get a
 * readable text too. With the mod's colors, Noir, Cerulean and Pink get white text on the screen and Blanc, Canary and
 * Lime get `#191919`. The info panel is light on every color but Noir, so only Noir gets white text there. Text on the
 * screen itself always has a 1 px shadow: Minecraft's dark one under white text, a white one under the dark text.
 *
 * @property screen RGB color of the text drawn on the screen itself: header, search, filter, color select and page
 * navigation.
 * @property panel RGB color of the text drawn on the info panel.
 *
 * @author hadouz01
 * @since 1.25.3
 */
data class CaracatronTextColors(val screen: Int, val panel: Int) {

    /**
     * Writes a text on the screen itself in [screen] color, with its 1 px shadow ([drawShadowed]).
     *
     * @param context Current GUI graphics.
     * @param text Text to write.
     * @param x Left edge, or center when [centered].
     * @param y Top edge.
     * @param font Font of the text, Minecraft's default when `null`.
     * @param scale Scale of the text, the shadow's offset included.
     * @param centered Whether [x] is the text's center.
     */
    fun drawScreenText(
        context: GuiGraphics,
        text: MutableComponent,
        x: Number,
        y: Number,
        font: ResourceLocation? = null,
        scale: Float = 1.0f,
        centered: Boolean = false
    ) {
        drawShadowed(context, text, x, y, screen, font, scale, centered)
    }

    /**
     * RGB color of the drop chances matching the drops page search: neon green on a dark panel, dark green on a light
     * one, so it never blends into green screens.
     */
    val match: Int get() = if (panel == LIGHT) MATCH_ON_DARK else MATCH_ON_LIGHT

    /**
     * RGB color of the labels of the materials page lines (e.g. "Overworld:" or "Altitude:") and of its links: a teal
     * blue on light panels, a light blue on dark ones, apart from the near black or white of the titles.
     */
    val label: Int get() = if (panel == LIGHT) LABEL_ON_DARK else LABEL_ON_LIGHT

    companion object {
        /**
         * Text colors: white for dark surfaces, `#191919` for light ones, the [match] greens and the [label] blues for
         * light and dark panels.
         */
        const val LIGHT = 0xFFFFFF
        const val DARK = 0x191919
        const val MATCH_ON_LIGHT = 0x0B6B1D
        const val MATCH_ON_DARK = 0x39FF14
        const val LABEL_ON_LIGHT = 0x1E6A86
        const val LABEL_ON_DARK = 0x8FD3FF

        /**
         * Shadow offset of Cobblemon's large font, in font units. It's Minecraft's Unifont (`minecraft:uniform`), whose
         * glyphs cast their shadow half a unit away instead of a whole one.
         */
        private const val UNIFORM_SHADOW_OFFSET = 0.5f

        /**
         * Shadow of the dark text: plain white, while white text keeps Minecraft's own dark shadow.
         */
        const val DARK_SHADOW = 0xFFFFFF

        /**
         * White text colors, like Cobblemon's, used until a screen color is known (the search box's first frame).
         */
        @JvmField
        val POKEDEX = CaracatronTextColors(LIGHT, LIGHT)

        /**
         * Average brightness (0 to 255) under which a surface counts as dark. Cerulean's header sits at 122 and Blanc's at
         * 157, so it splits the colors the way the screens were drawn to be read.
         */
        private const val DARK_SURFACE_BRIGHTNESS = 140
        private const val MIN_ALPHA = 100

        private val SCREEN_SAMPLE = intArrayOf(30, 12, 100, 22)
        private val PANEL_SAMPLE = intArrayOf(4, 26, 135, 150)

        private val cache = mutableMapOf<String, CaracatronTextColors>()

        /**
         * Gets the text colors of a screen color, measured once per folder.
         *
         * @param folder Folder name of the color.
         */
        fun of(folder: String): CaracatronTextColors = cache.getOrPut(folder) {
            CaracatronTextColors(
                screen = textOn(CaracatronColors.part(folder, "caracatron_screen"), SCREEN_SAMPLE),
                panel = textOn(CaracatronColors.part(folder, "caracatron_screen_info"), PANEL_SAMPLE)
            )
        }

        /**
         * Writes a text with a 1 px shadow (scaled with the text, like Minecraft's): Minecraft's own dark shadow for any
         * color but [DARK], which gets the white [DARK_SHADOW] so the shadow can still be told apart from the letters.
         * The dark text's shadow sits as far as Minecraft would put it for that font ([shadowOffset]).
         *
         * @param context Current GUI graphics.
         * @param text Text to write.
         * @param x Left edge, or center when [centered].
         * @param y Top edge.
         * @param color RGB color of the text.
         * @param font Font of the text, Minecraft's default when `null`.
         * @param scale Scale of the text, the shadow's offset included.
         * @param centered Whether [x] is the text's center.
         */
        fun drawShadowed(
            context: GuiGraphics,
            text: MutableComponent,
            x: Number,
            y: Number,
            color: Int,
            font: ResourceLocation? = null,
            scale: Float = 1.0f,
            centered: Boolean = false
        ) {
            if (color != DARK) {
                drawScaledText(context = context, font = font, text = text, x = x, y = y, scale = scale, colour = color, centered = centered, shadow = true)
                return
            }
            val offset = scale * shadowOffset(font)
            drawScaledText(
                context = context,
                font = font,
                text = text.copy(),
                x = x.toDouble() + offset,
                y = y.toDouble() + offset,
                scale = scale,
                colour = DARK_SHADOW,
                centered = centered
            )
            drawScaledText(context = context, font = font, text = text, x = x, y = y, scale = scale, colour = color, centered = centered)
        }

        /**
         * Distance, in font units, Minecraft draws a font's shadow from its letters: half a unit for Cobblemon's large
         * font, one unit for Minecraft's default one.
         *
         * @param font Font of the text, Minecraft's default when `null`.
         */
        private fun shadowOffset(font: ResourceLocation?): Float {
            return if (font == CobblemonResources.DEFAULT_LARGE) UNIFORM_SHADOW_OFFSET else 1.0f
        }

        /**
         * Forgets the measured colors, so they're measured again from the reloaded textures.
         */
        fun clear() {
            cache.clear()
        }

        /**
         * Picks the text color for an area of a texture, white when the texture is missing.
         *
         * @param texture Texture under the text.
         * @param area Left, top, right and bottom edges of the measured area, in texture pixels.
         */
        private fun textOn(texture: ResourceLocation, area: IntArray): Int {
            val brightness = brightness(texture, area) ?: return LIGHT
            return if (brightness < DARK_SURFACE_BRIGHTNESS) LIGHT else DARK
        }

        /**
         * Measures the average perceived brightness of the visible pixels in an area of a texture.
         *
         * @return The brightness from 0 to 255, or `null` when the texture can't be read or the area is see-through.
         */
        private fun brightness(texture: ResourceLocation, area: IntArray): Double? {
            val resource = Minecraft.getInstance().resourceManager.getResource(texture).orElse(null) ?: return null
            val image = runCatching { resource.open().use { NativeImage.read(it) } }.getOrNull() ?: return null
            return image.use {
                var total = 0.0
                var count = 0
                for (x in area[0] until area[2].coerceAtMost(it.width)) {
                    for (y in area[1] until area[3].coerceAtMost(it.height)) {
                        val pixel = it.getPixelRGBA(x, y)
                        if ((pixel ushr 24 and 0xFF) <= MIN_ALPHA) continue
                        val red = pixel and 0xFF
                        val green = pixel shr 8 and 0xFF
                        val blue = pixel shr 16 and 0xFF
                        total += 0.2126 * red + 0.7152 * green + 0.0722 * blue
                        count++
                    }
                }
                if (count == 0) null else total / count
            }
        }
    }
}
