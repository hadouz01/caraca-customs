package com.caraca.customs.client.gui.caracatron

import com.cobblemon.mod.common.client.CobblemonResources
import net.minecraft.ChatFormatting
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

/**
 * Search bar text of the Caracatron, drawn in Cobblemon's large bold font like the tier filter, following Cobblemon's
 * Pokédex search. The bar and its icon are drawn by [CaracatronScreen].
 *
 * @param x Left edge of the text.
 * @param y Top edge of the text.
 * @param width Clickable width.
 * @param height Clickable height.
 * @property placeholder Text shown while the search is empty and unfocused, changed when the page searches something else.
 *
 * @author hadouz01
 * @since 1.12.0
 */
class CaracatronSearchBox(x: Int, y: Int, width: Int, height: Int, var placeholder: Component) :
    EditBox(Minecraft.getInstance().font, x, y, width, height, placeholder) {

    private companion object {
        /**
         * Longest search, enough for item ids picked from the drops page (like `cobblemon:black_augurite`). Text past the
         * bar is cut off.
         */
        const val MAX_LENGTH = 64
    }

    private var focusedTime = Util.getMillis()

    /**
     * Text colors of the screen color, for the text and its shadow.
     */
    var screenColors: CaracatronTextColors = CaracatronTextColors.POKEDEX

    init {
        setMaxLength(MAX_LENGTH)
    }

    override fun setFocused(focused: Boolean) {
        if (focused) focusedTime = Util.getMillis()
        super.setFocused(focused)
    }

    override fun renderWidget(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val showCursor = isFocused && (Util.getMillis() - focusedTime) / 300L % 2L == 0L
        val input = when {
            isFocused -> Component.literal(value + if (showCursor && cursorPosition == value.length) "_" else "")
            value.isEmpty() -> placeholder.copy()
            else -> Component.literal(value)
        }

        context.enableScissor(x - 1, y - 1, x + width, y + height)
        screenColors.drawScreenText(context, input.withStyle(ChatFormatting.BOLD), x, y, font = CobblemonResources.DEFAULT_LARGE)
        context.disableScissor()

        if (showCursor && value.isNotEmpty() && cursorPosition != value.length) {
            val beforeCursor = Component.literal(value.substring(0, cursorPosition))
                .withStyle(Style.EMPTY.withBold(true).withFont(CobblemonResources.DEFAULT_LARGE))
            val cursorX = x + Minecraft.getInstance().font.width(beforeCursor)
            context.fill(RenderType.guiTextHighlight(), cursorX - 1, y, cursorX, y + 9, -3092272)
        }
    }
}
