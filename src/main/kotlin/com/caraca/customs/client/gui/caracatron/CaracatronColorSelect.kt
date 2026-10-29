package com.caraca.customs.client.gui.caracatron

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.client.render.drawScaledText
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

/**
 * Screen color select of the Caracatron 3000, at the top center of the screen. Clicking it opens a drawer listing every
 * color folder ([CaracatronColors]) with a swatch and its name, up to [VISIBLE_ROWS] at a time, with a scroll bar when
 * there are more.
 *
 * Positions are relative to the Caracatron's top left corner. While open, the drawer takes every click: picking a color
 * selects it, clicking anywhere else closes it.
 *
 * @param colors Color folders to choose from.
 * @param selected Color folder in use.
 * @param onSelect Called with the folder of a newly picked color.
 *
 * @author hadouz01
 * @since 1.16.0
 */
class CaracatronColorSelect(
    private val colors: List<String>,
    selected: String,
    private val onSelect: (String) -> Unit
) {

    companion object {
        private const val BOX_X = 153
        private const val BOX_Y = 18
        private const val BOX_WIDTH = 40
        private const val BOX_HEIGHT = 5

        private const val DRAWER_TOP = 24
        private const val ROW_HEIGHT = 7
        private const val VISIBLE_ROWS = 5
        private const val ROW_SWATCH_SIZE = 5
        private const val BOX_SWATCH_SIZE = 3
        private const val SWATCH_U = 40
        private const val SWATCH_V = 60
        private const val SCREEN_WIDTH = 345
        private const val SCREEN_HEIGHT = 207

        private const val ARROW_X = 186
        private const val ARROW_WIDTH = 12
        private const val ARROW_HEIGHT = 6
        private const val SCALE = 0.5f

        private const val HOVER_COLOR = 0x30FFFFFF
        private const val SELECTED_COLOR = 0x50FFFFFF
        private const val DRAWER_COLOR = 0xF0141E21.toInt()
        private const val DRAWER_BORDER = 0xFF606B6E.toInt()
        private const val DRAWER_Z = 400.0f
    }

    /**
     * Color folder in use.
     */
    var selected: String = selected
        private set

    /**
     * Whether the drawer is open.
     */
    var isOpen: Boolean = false
        private set

    private val rows: Int get() = colors.size.coerceAtMost(VISIBLE_ROWS)
    private val drawerBottom: Int get() = DRAWER_TOP + rows * ROW_HEIGHT

    private val scroll = CaracatronScrollBar(BOX_X + BOX_WIDTH - 3, DRAWER_TOP + 1, DRAWER_TOP + VISIBLE_ROWS * ROW_HEIGHT - 1, 2, 1, 6)

    /**
     * Draws the select and, while open, the drawer over everything else.
     *
     * @param context Current GUI graphics.
     * @param originX Caracatron's left edge on screen.
     * @param originY Caracatron's top edge on screen.
     * @param mouseX Mouse x relative to the Caracatron.
     * @param mouseY Mouse y relative to the Caracatron.
     * @param textColors Text colors of the color in use, for the name on the select. The drawer keeps white text over its
     * own dark background.
     */
    fun render(
        context: GuiGraphics,
        originX: Int,
        originY: Int,
        mouseX: Double,
        mouseY: Double,
        textColors: CaracatronTextColors
    ) {
        if (!isOpen && isOverBox(mouseX, mouseY)) {
            context.fill(originX + BOX_X, originY + BOX_Y, originX + BOX_X + BOX_WIDTH, originY + BOX_Y + BOX_HEIGHT, HOVER_COLOR)
        }
        swatch(context, selected, originX + BOX_X + 1.0f, originY + BOX_Y + 1.0f, BOX_SWATCH_SIZE)
        textColors.drawScreenText(context, CaracatronColors.displayName(selected).copy(), originX + BOX_X + 6, originY + BOX_Y + 0.5, scale = SCALE)
        arrow(context, CaracatronColors.icon("select_arrow"), originX + ARROW_X.toFloat(), originY + BOX_Y + 1.0f, pointingUp = isOpen)

        if (isOpen) renderDrawer(context, originX, originY, mouseX, mouseY)
    }

    private fun renderDrawer(
        context: GuiGraphics,
        originX: Int,
        originY: Int,
        mouseX: Double,
        mouseY: Double
    ) {
        val pose = context.pose()
        pose.pushPose()
        pose.translate(0.0f, 0.0f, DRAWER_Z)

        val left = originX + BOX_X
        val right = left + BOX_WIDTH
        context.fill(left, originY + DRAWER_TOP, right, originY + drawerBottom, DRAWER_COLOR)
        context.renderOutline(left - 1, originY + DRAWER_TOP - 1, BOX_WIDTH + 2, rows * ROW_HEIGHT + 2, DRAWER_BORDER)

        val hovered = rowAt(mouseX, mouseY)
        context.enableScissor(left, originY + DRAWER_TOP, right, originY + drawerBottom)
        colors.forEachIndexed { index, color ->
            val rowY = originY + DRAWER_TOP + index * ROW_HEIGHT - scroll.offset.toInt()
            when {
                color == selected -> context.fill(left, rowY, right, rowY + ROW_HEIGHT, SELECTED_COLOR)
                index == hovered -> context.fill(left, rowY, right, rowY + ROW_HEIGHT, HOVER_COLOR)
            }
            swatch(context, color, left + 2.0f, rowY + 1.0f, ROW_SWATCH_SIZE)
            drawScaledText(
                context = context,
                text = CaracatronColors.displayName(color).copy(),
                x = left + 9,
                y = rowY + 1.5,
                scale = SCALE,
                shadow = true
            )
        }
        context.disableScissor()

        if (colors.size > VISIBLE_ROWS) scroll.render(context, originX, originY, CaracatronColors.icon("scrollbar_slide"), CaracatronColors.part(selected, "scrollbar_track"))
        pose.popPose()
    }

    /**
     * Handles a left click.
     *
     * @param mouseX Mouse x relative to the Caracatron.
     * @param mouseY Mouse y relative to the Caracatron.
     * @return Whether the click was used: always while open, and on the select while closed.
     */
    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!isOpen) {
            if (!isOverBox(mouseX, mouseY)) return false
            open()
            return true
        }

        if (scroll.startDrag(mouseX, mouseY)) return true
        val color = colors.getOrNull(rowAt(mouseX, mouseY))
        if (color != null && color != selected) {
            selected = color
            onSelect(color)
        }
        isOpen = false
        return true
    }

    /**
     * Scrolls the drawer when the mouse is over it. Over the select itself, open or not, the wheel picks the color above
     * (going up) or below (going down) in the list, going round from the first color to the last and back.
     *
     * @param mouseX Mouse x relative to the Caracatron.
     * @param mouseY Mouse y relative to the Caracatron.
     * @param amount Wheel movement, positive going up.
     * @return Whether the select or its drawer took the scroll.
     */
    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (isOverBox(mouseX, mouseY)) {
            cycle(if (amount > 0) -1 else 1)
            return true
        }
        if (!isOverDrawer(mouseX, mouseY)) return false
        scroll.scrollBy(-amount * ROW_HEIGHT)
        return true
    }

    /**
     * Picks the color a number of rows away from the selected one, going round the list.
     *
     * @param step Rows to move, negative going up.
     */
    private fun cycle(step: Int) {
        if (colors.size < 2 || step == 0) return
        val current = colors.indexOf(selected).coerceAtLeast(0)
        val color = colors[Math.floorMod(current + step, colors.size)]
        selected = color
        onSelect(color)
        CaracatronPages.playClick()
    }

    /**
     * Moves the drawer along with its dragged scroll bar.
     *
     * @param deltaY Mouse movement since the last drag event.
     * @return Whether the scroll bar is being dragged.
     */
    fun drag(deltaY: Double): Boolean = isOpen && scroll.drag(deltaY)

    /**
     * Releases the scroll bar.
     */
    fun stopDrag() {
        scroll.stopDrag()
    }

    /**
     * Checks if the mouse is over the select or its open drawer, where nothing under it should react.
     *
     * @param mouseX Mouse x relative to the Caracatron.
     * @param mouseY Mouse y relative to the Caracatron.
     */
    fun isOver(mouseX: Double, mouseY: Double): Boolean = isOverBox(mouseX, mouseY) || isOverDrawer(mouseX, mouseY)

    /**
     * Checks if the mouse is over the closed select, the only place its tooltip shows.
     *
     * @param mouseX Mouse x relative to the Caracatron.
     * @param mouseY Mouse y relative to the Caracatron.
     */
    fun showsTooltip(mouseX: Double, mouseY: Double): Boolean = !isOpen && isOverBox(mouseX, mouseY)

    private fun open() {
        isOpen = true
        scroll.resize(colors.size * ROW_HEIGHT, rows * ROW_HEIGHT)
        scroll.reset()
        scroll.scrollBy(((colors.indexOf(selected) - VISIBLE_ROWS + 1).coerceAtLeast(0) * ROW_HEIGHT).toDouble())
    }

    private fun isOverBox(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= BOX_X && mouseX < BOX_X + BOX_WIDTH && mouseY >= BOX_Y && mouseY < BOX_Y + BOX_HEIGHT
    }

    private fun isOverDrawer(mouseX: Double, mouseY: Double): Boolean {
        return isOpen && mouseX >= BOX_X && mouseX < BOX_X + BOX_WIDTH && mouseY >= DRAWER_TOP && mouseY < drawerBottom
    }

    /**
     * Index in [colors] of the row under the mouse, or `-1`. The scroll bar column doesn't count as a row.
     */
    private fun rowAt(mouseX: Double, mouseY: Double): Int {
        if (!isOverDrawer(mouseX, mouseY)) return -1
        if (colors.size > VISIBLE_ROWS && mouseX >= BOX_X + BOX_WIDTH - 4) return -1
        val index = ((mouseY - DRAWER_TOP + scroll.offset) / ROW_HEIGHT).toInt()
        return if (index in colors.indices) index else -1
    }

    /**
     * Draws a small square cut from the middle of a color's screen, under the Pokémon list, as a sample of the color.
     * Slots are the same for every color, so they can't be the sample anymore.
     */
    private fun swatch(context: GuiGraphics, color: String, x: Float, y: Float, size: Int) {
        blitk(
            matrixStack = context.pose(),
            texture = CaracatronColors.part(color, "caracatron_screen"),
            x = x,
            y = y,
            width = size,
            height = size,
            uOffset = SWATCH_U,
            vOffset = SWATCH_V,
            textureWidth = SCREEN_WIDTH,
            textureHeight = SCREEN_HEIGHT
        )
    }

    /**
     * Draws the select arrow at half size, mirrored when it points up.
     */
    private fun arrow(context: GuiGraphics, texture: ResourceLocation, x: Float, y: Float, pointingUp: Boolean) {
        val pose = context.pose()
        pose.pushPose()
        pose.translate(x, y + if (pointingUp) ARROW_HEIGHT * SCALE else 0.0f, 0.0f)
        pose.scale(SCALE, if (pointingUp) -SCALE else SCALE, 1.0f)
        RenderSystem.disableCull()
        blitk(matrixStack = context.pose(), texture = texture, x = 0, y = 0, width = ARROW_WIDTH, height = ARROW_HEIGHT)
        RenderSystem.enableCull()
        pose.popPose()
    }
}
