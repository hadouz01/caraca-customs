package com.caraca.customs.client.gui.caracatron

import com.caraca.CaracaCustoms
import com.cobblemon.mod.common.client.CobblemonResources
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence

/**
 * Dresses Cobblemon's Pokédex in the Caracatron's screen color on the Pokédex page, between [begin] and [end].
 *
 * Pokédex textures use, in this order: the color's own `colors/<color>/pokedex/<name>.png`, the matching Caracatron part
 * ([SHARED_PARTS]), a copy recolored with [CaracatronPalette], or Cobblemon's own. Only a few are worked out per frame
 * ([MAX_PICKS_PER_FRAME]), so the page can show some blue for a split second.
 *
 * White texts take the screen text color (with a white shadow when it's dark), the panel's gray turns white on Noir (except in
 * the scroll text box) and the blue accents follow the palette.
 *
 * @author hadouz01
 * @since 1.29.0
 */
object CaracatronPokedexSkin {

    private const val POKEDEX_PREFIX = "textures/gui/pokedex/"
    private const val POKEDEX_TEXT = 0x606B6E
    private const val UNIFORM_SHADOW_OFFSET = 0.5f
    private const val RGB = 0xFFFFFF
    private const val ALPHA = 0xFF000000.toInt()

    /**
     * Most textures looked up (and possibly recolored) per frame. The rest show Cobblemon's texture for a frame or
     * two, so opening the Pokédex page never stalls on recoloring every texture at once.
     */
    private const val MAX_PICKS_PER_FRAME = 6

    /**
     * Caracatron screen parts with the same size and layout as a Pokédex texture, by Pokédex texture name.
     */
    private val SHARED_PARTS = mapOf(
        "pokedex_screen" to "caracatron_screen",
        "pokedex_screen_bar_search" to "caracatron_screen_bar_search",
        "pokedex_screen_bar_category" to "caracatron_screen_bar_category",
        "scrollbar_track" to "scrollbar_track",
        "type_bar" to "type_bar",
        "type_bar_double" to "type_bar_long"
    )

    private var folder: String? = null
    private var drawingText = false
    private var keepingPanelText = false
    private var picksThisFrame = 0

    private val textures = HashMap<String, ResourceLocation>()
    private val generated = mutableListOf<ResourceLocation>()
    private val palettes = HashMap<String, CaracatronPalette>()

    /**
     * Starts dressing the Pokédex, when it's the Caracatron's Pokédex page.
     *
     * @param screen Pokédex screen about to render.
     */
    @JvmStatic
    fun begin(screen: Screen) {
        folder = CaracatronPages.colorFolder(screen)
        picksThisFrame = 0
    }

    /**
     * Stops dressing, before the Caracatron's own overlay is drawn over the Pokédex.
     */
    @JvmStatic
    fun end() {
        folder = null
    }

    /**
     * Keeps the Pokédex's gray panel text as it is while its scroll text box (description, abilities, moves) draws,
     * since that box stays light on every screen color.
     *
     * @param keep Whether the scroll text box is drawing.
     */
    @JvmStatic
    fun keepPanelText(keep: Boolean) {
        keepingPanelText = keep
    }

    /**
     * Picks the texture to bind in place of a Pokédex texture.
     *
     * @param texture Texture Cobblemon is binding.
     * @return The texture to bind, [texture] itself outside the Caracatron's Pokédex page.
     */
    @JvmStatic
    fun texture(texture: ResourceLocation?): ResourceLocation? {
        if (texture == null || texture.namespace != "cobblemon" || !texture.path.startsWith(POKEDEX_PREFIX)) return texture
        val color = activeFolder() ?: return texture
        val name = texture.path.removePrefix(POKEDEX_PREFIX).removeSuffix(".png")
        val key = "$color/$name"
        textures[key]?.let { return it }
        if (picksThisFrame >= MAX_PICKS_PER_FRAME) return texture
        picksThisFrame++
        return pick(color, name, texture).also { textures[key] = it }
    }

    /**
     * Draws a Pokédex text in the screen color's text colors, in place of `GuiGraphics.drawString`.
     *
     * @return The x after the text, or `null` to let Minecraft draw it as it is.
     */
    @JvmStatic
    fun drawText(context: GuiGraphics, font: Font, text: FormattedCharSequence, x: Int, y: Int, color: Int, shadow: Boolean): Int? {
        if (drawingText) return null
        val folder = activeFolder() ?: return null
        val colors = CaracatronTextColors.of(folder)
        val rgb = color and RGB
        val target = when (rgb) {
            RGB -> colors.screen
            POKEDEX_TEXT -> if (!keepingPanelText && colors.darkPanel) CaracatronTextColors.LIGHT else rgb
            else -> palette(folder).map(rgb)
        }
        val darkShadow = shadow && target == CaracatronTextColors.DARK
        if (target == rgb && !darkShadow) return null

        val alpha = color and ALPHA
        drawingText = true
        try {
            if (!darkShadow) return context.drawString(font, text, x, y, alpha or target, shadow)
            val offset = shadowOffset(text)
            context.pose().pushPose()
            context.pose().translate(offset, offset, 0.0f)
            context.drawString(font, text, x, y, alpha or CaracatronTextColors.DARK_SHADOW, false)
            context.pose().popPose()
            return context.drawString(font, text, x, y, alpha or target, false)
        } finally {
            drawingText = false
        }
    }

    /**
     * Same as the [FormattedCharSequence] version, for plain strings.
     */
    @JvmStatic
    fun drawText(context: GuiGraphics, font: Font, text: String, x: Int, y: Int, color: Int, shadow: Boolean): Int? {
        if (drawingText || activeFolder() == null) return null
        return drawText(context, font, Component.literal(text).visualOrderText, x, y, color, shadow)
    }

    /**
     * Forgets the palettes and recolored textures, so they're made again from the reloaded resources.
     */
    fun clear() {
        val textureManager = Minecraft.getInstance().textureManager
        generated.forEach(textureManager::release)
        generated.clear()
        textures.clear()
        palettes.clear()
    }

    private fun activeFolder(): String? {
        val color = folder ?: return null
        val screen = Minecraft.getInstance().screen ?: return null
        return if (CaracatronPages.isFramed(screen)) color else null
    }

    private fun pick(color: String, name: String, original: ResourceLocation): ResourceLocation {
        val resources = Minecraft.getInstance().resourceManager
        val own = CaracatronColors.part(color, "pokedex/$name")
        if (resources.getResource(own).isPresent) return own

        SHARED_PARTS[name]?.let { CaracatronColors.part(color, it) }?.takeIf { resources.getResource(it).isPresent }?.let { return it }

        val palette = palette(color)
        if (palette.isEmpty) return original
        val image = resources.getResource(original).orElse(null)
            ?.let { resource -> runCatching { resource.open().use { NativeImage.read(it) } }.getOrNull() }
            ?: return original
        if (!palette.recolor(image)) {
            image.close()
            return original
        }
        val id = CaracaCustoms.id("caracatron_pokedex/$color/${name.replace('/', '_')}")
        Minecraft.getInstance().textureManager.register(id, DynamicTexture(image))
        generated += id
        return id
    }

    private fun palette(color: String): CaracatronPalette = palettes.getOrPut(color) {
        CaracatronPalette(SHARED_PARTS.map { (pokedex, part) -> pokedexTexture(pokedex) to CaracatronColors.part(color, part) })
    }

    private fun pokedexTexture(name: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath("cobblemon", "$POKEDEX_PREFIX$name.png")

    /**
     * Distance Minecraft draws a text's shadow from it: half a unit for Cobblemon's large font, one for the others.
     */
    private fun shadowOffset(text: FormattedCharSequence): Float {
        var offset = 1.0f
        text.accept { _, style, _ ->
            if (style.font == CobblemonResources.DEFAULT_LARGE) offset = UNIFORM_SHADOW_OFFSET
            false
        }
        return offset
    }
}
