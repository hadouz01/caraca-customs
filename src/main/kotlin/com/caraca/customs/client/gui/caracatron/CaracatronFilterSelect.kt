package com.caraca.customs.client.gui.caracatron

import com.cobblemon.mod.common.client.render.drawScaledText
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

/**
 * Drawer of the Caracatron's filter bar, like the screen color one. Clicking the filter name opens it upwards with
 * every filter of the page, [VISIBLE_ROWS] at a time. While it's open it takes every click: a row picks that filter,
 * anywhere else closes it.
 *
 * @param onSelect Called with a newly picked filter.
 *
 * @author hadouz01
 * @since 1.32.0
 */
class CaracatronFilterSelect(private val onSelect: (CaracatronFilter) -> Unit) {

    companion object {
        private const val AREA_X = 26
        private const val AREA_Y = 180
        private const val AREA_WIDTH = 125
        private const val AREA_HEIGHT = 11

        private const val DRAWER_X = 24
        private const val DRAWER_WIDTH = 128
        private const val DRAWER_BOTTOM = 179
        private const val ROW_HEIGHT = 7
        private const val VISIBLE_ROWS = 8
        private const val SCALE = 0.5f

        private const val HOVER_COLOR = 0x30FFFFFF
        private const val SELECTED_COLOR = 0x50FFFFFF
        private const val DRAWER_COLOR = 0xF0141E21.toInt()
        private const val DRAWER_BORDER = 0xFF606B6E.toInt()
        private const val DRAWER_Z = 400.0f
    }

    /**
     * Whether the drawer is open.
     */
    var isOpen: Boolean = false
        private set

    private var options: List<CaracatronFilter> = emptyList()

    private val rows: Int get() = options.size.coerceAtMost(VISIBLE_ROWS)
    private val drawerTop: Int get() = DRAWER_BOTTOM - rows * ROW_HEIGHT

    private val scroll = CaracatronScrollBar(
        DRAWER_X + DRAWER_WIDTH - 3,
        DRAWER_BOTTOM - VISIBLE_ROWS * ROW_HEIGHT + 1,
        DRAWER_BOTTOM - 1,
        2,
        1,
        6
    )

    /**
     * Draws the hover box over the filter name and, while open, the drawer over everything else.
     *
     * @param context Current GUI graphics.
     * @param originX Caracatron's left edge on screen.
     * @param originY Caracatron's top edge on screen.
     * @param mouseX Mouse x relative to the Caracatron.
     * @param mouseY Mouse y relative to the Caracatron.
     * @param selected Filter in use.
     * @param scrollTrack Scroll bar track of the screen color.
     */
    fun render(
        context: GuiGraphics,
        originX: Int,
        originY: Int,
        mouseX: Double,
        mouseY: Double,
        selected: CaracatronFilter,
        scrollTrack: ResourceLocation
    ) {
        if (!isOpen) {
            if (isOverArea(mouseX, mouseY)) {
                context.fill(originX + AREA_X, originY + AREA_Y, originX + AREA_X + AREA_WIDTH, originY + AREA_Y + AREA_HEIGHT, HOVER_COLOR)
            }
            return
        }

        val pose = context.pose()
        pose.pushPose()
        pose.translate(0.0f, 0.0f, DRAWER_Z)

        val left = originX + DRAWER_X
        val right = left + DRAWER_WIDTH
        val top = originY + drawerTop
        val bottom = originY + DRAWER_BOTTOM
        context.fill(left, top, right, bottom, DRAWER_COLOR)
        context.renderOutline(left - 1, top - 1, DRAWER_WIDTH + 2, rows * ROW_HEIGHT + 2, DRAWER_BORDER)

        val hovered = rowAt(mouseX, mouseY)
        context.enableScissor(left, top, right, bottom)
        options.forEachIndexed { index, filter ->
            val rowY = top + index * ROW_HEIGHT - scroll.offset.toInt()
            when {
                filter == selected -> context.fill(left, rowY, right, rowY + ROW_HEIGHT, SELECTED_COLOR)
                index == hovered -> context.fill(left, rowY, right, rowY + ROW_HEIGHT, HOVER_COLOR)
            }
            drawScaledText(context = context, text = filter.label(), x = left + 3, y = rowY + 1.5, scale = SCALE, shadow = true)
        }
        context.disableScissor()

        if (options.size > VISIBLE_ROWS) scroll.render(context, originX, originY, CaracatronColors.icon("scrollbar_slide"), scrollTrack)
        pose.popPose()
    }

    /**
     * Handles a left click.
     *
     * @param mouseX Mouse x relative to the Caracatron.
     * @param mouseY Mouse y relative to the Caracatron.
     * @param available Filters of the page, in order.
     * @param selected Filter in use.
     * @return Whether the click was used: always while open, and on the filter name while closed.
     */
    fun mouseClicked(mouseX: Double, mouseY: Double, available: List<CaracatronFilter>, selected: CaracatronFilter): Boolean {
        if (!isOpen) {
            if (!isOverArea(mouseX, mouseY)) return false
            open(available, selected)
            return true
        }

        if (scroll.startDrag(mouseX, mouseY)) return true
        val filter = options.getOrNull(rowAt(mouseX, mouseY))
        if (filter != null && filter != selected) onSelect(filter)
        isOpen = false
        return true
    }

    /**
     * Scrolls the drawer when the mouse is over it.
     *
     * @param mouseX Mouse x relative to the Caracatron.
     * @param mouseY Mouse y relative to the Caracatron.
     * @param amount Wheel movement, positive going up.
     * @return Whether the drawer took the scroll.
     */
    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!isOverDrawer(mouseX, mouseY)) return false
        scroll.scrollBy(-amount * ROW_HEIGHT)
        return true
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
     * Closes the drawer, like when the page changes.
     */
    fun close() {
        isOpen = false
    }

    /**
     * Checks if the mouse is over the open drawer, where nothing under it should react.
     *
     * @param mouseX Mouse x relative to the Caracatron.
     * @param mouseY Mouse y relative to the Caracatron.
     */
    fun isOverDrawer(mouseX: Double, mouseY: Double): Boolean {
        return isOpen && mouseX >= DRAWER_X && mouseX < DRAWER_X + DRAWER_WIDTH && mouseY >= drawerTop && mouseY < DRAWER_BOTTOM
    }

    private fun open(available: List<CaracatronFilter>, selected: CaracatronFilter) {
        options = available
        isOpen = true
        scroll.resize(options.size * ROW_HEIGHT, rows * ROW_HEIGHT)
        scroll.reset()
        scroll.scrollBy(((options.indexOf(selected) - VISIBLE_ROWS + 1).coerceAtLeast(0) * ROW_HEIGHT).toDouble())
    }

    private fun isOverArea(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= AREA_X && mouseX < AREA_X + AREA_WIDTH && mouseY >= AREA_Y && mouseY < AREA_Y + AREA_HEIGHT
    }

    /**
     * Index in the drawer of the row under the mouse, or `-1`. The scroll bar column doesn't count as a row.
     */
    private fun rowAt(mouseX: Double, mouseY: Double): Int {
        if (!isOverDrawer(mouseX, mouseY)) return -1
        if (options.size > VISIBLE_ROWS && mouseX >= DRAWER_X + DRAWER_WIDTH - 4) return -1
        val index = ((mouseY - drawerTop + scroll.offset) / ROW_HEIGHT).toInt()
        return if (index in options.indices) index else -1
    }
}
