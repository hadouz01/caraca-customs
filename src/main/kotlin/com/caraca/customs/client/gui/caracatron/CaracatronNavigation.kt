package com.caraca.customs.client.gui.caracatron

import com.cobblemon.mod.common.api.gui.blitk
import com.mojang.math.Axis
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

/**
 * Page navigation at the top of the Caracatron 3000, rectangles 29 to 32 of `exemplo_caracatron.png`: the page before the
 * current one on the left, after an arrow pointing left, and the page after it on the right, followed by an arrow
 * pointing right ([CaracatronPage]).
 *
 * While the mouse is over an option (its arrow or its name), the option sways back and forth in its arrow's direction,
 * one whole pixel at a time, for an 8-bit feel, over a translucent box in the text color that shows the whole click area.
 * Positions are relative to the Caracatron's top left corner.
 *
 * @param page Page being shown.
 * @param onNavigate Called with the page picked by a click.
 *
 * @author hadouz01
 * @since 1.25.0
 */
class CaracatronNavigation(private val page: CaracatronPage, private val onNavigate: (CaracatronPage) -> Unit) {

    /**
     * One of the two options.
     *
     * @property target Page the option opens.
     * @property direction `-1` for the left option, `1` for the right one.
     * @property areaX Left edge of the option's click area, which holds its arrow and name with room for the sway.
     */
    private class Option(val target: CaracatronPage, val direction: Int, val areaX: Int) {
        var hoverStart: Long? = null
    }

    companion object {
        private const val TEXT_Y = 18.5
        private const val AREA_TOP = 17
        private const val AREA_HEIGHT = 7
        private const val AREA_WIDTH = 37

        private const val LEFT_ARROW_CENTER_X = 108.5f
        private const val LEFT_TEXT_X = 112
        private const val LEFT_AREA_X = 104
        private const val RIGHT_ARROW_CENTER_X = 236.5f
        private const val RIGHT_TEXT_RIGHT = 233
        private const val RIGHT_AREA_X = 204
        private const val ARROW_CENTER_Y = 20.5f

        private const val ARROW_WIDTH = 12
        private const val ARROW_HEIGHT = 6
        private const val SCALE = 0.5f

        /**
         * Pixel offsets of the hover animation, one per step, in the arrow's direction.
         */
        private val SWAY = intArrayOf(0, 1, 2, 1)
        private const val SWAY_STEP_MILLIS = 220L

        private const val HOVER_FILL_ALPHA = 0x30
        private const val HOVER_OUTLINE_ALPHA = 0x70
    }

    private val options = listOf(
        Option(page.previous, -1, LEFT_AREA_X),
        Option(page.next, 1, RIGHT_AREA_X)
    )

    /**
     * Draws both options.
     *
     * @param context Current GUI graphics.
     * @param originX Caracatron's left edge on screen.
     * @param originY Caracatron's top edge on screen.
     * @param mouseX Mouse x relative to the Caracatron.
     * @param mouseY Mouse y relative to the Caracatron.
     * @param textColors Text colors of the page, for the page names and the hover box.
     */
    fun render(context: GuiGraphics, originX: Int, originY: Int, mouseX: Double, mouseY: Double, textColors: CaracatronTextColors) {
        val font = Minecraft.getInstance().font
        val now = Util.getMillis()
        options.forEach { option ->
            val start = if (isOver(option, mouseX, mouseY)) option.hoverStart ?: now.also { option.hoverStart = it } else null
            option.hoverStart = start
            val offset = if (start == null) 0 else SWAY[((now - start) / SWAY_STEP_MILLIS % SWAY.size).toInt()] * option.direction

            if (start != null) hoverBox(context, originX + option.areaX, originY + AREA_TOP, textColors.screen)

            val label = option.target.label().copy()
            val textX = if (option.direction < 0) LEFT_TEXT_X.toDouble() else RIGHT_TEXT_RIGHT - font.width(label) * SCALE.toDouble()
            textColors.drawScreenText(context, label, originX + textX + offset, originY + TEXT_Y, scale = SCALE)
            val arrowX = if (option.direction < 0) LEFT_ARROW_CENTER_X else RIGHT_ARROW_CENTER_X
            arrow(context, originX + arrowX + offset, originY + ARROW_CENTER_Y, option.direction)
        }
    }

    /**
     * Opens the page of the option under the mouse.
     *
     * @param mouseX Mouse x relative to the Caracatron.
     * @param mouseY Mouse y relative to the Caracatron.
     * @return Whether an option was clicked.
     */
    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        val option = options.firstOrNull { isOver(it, mouseX, mouseY) } ?: return false
        onNavigate(option.target)
        return true
    }

    /**
     * Checks if the mouse is over one of the options.
     *
     * @param mouseX Mouse x relative to the Caracatron.
     * @param mouseY Mouse y relative to the Caracatron.
     */
    fun isOver(mouseX: Double, mouseY: Double): Boolean = options.any { isOver(it, mouseX, mouseY) }

    private fun isOver(option: Option, mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= option.areaX && mouseX < option.areaX + AREA_WIDTH && mouseY >= AREA_TOP && mouseY < AREA_TOP + AREA_HEIGHT
    }

    /**
     * Draws the box behind a hovered option, filling its whole click area with a translucent tint of the text color and
     * outlining it a little stronger, so it reads as selected on light and dark screens alike.
     *
     * @param x Left edge of the click area on screen.
     * @param y Top edge of the click area on screen.
     * @param color RGB text color of the page.
     */
    private fun hoverBox(context: GuiGraphics, x: Int, y: Int, color: Int) {
        val rgb = color and 0xFFFFFF
        context.fill(x, y, x + AREA_WIDTH, y + AREA_HEIGHT, (HOVER_FILL_ALPHA shl 24) or rgb)
        context.renderOutline(x, y, AREA_WIDTH, AREA_HEIGHT, (HOVER_OUTLINE_ALPHA shl 24) or rgb)
    }

    /**
     * Draws the select arrow at half size, turned to point left or right. The texture points down, so it's turned a
     * quarter clockwise to point left and a quarter counterclockwise to point right.
     */
    private fun arrow(context: GuiGraphics, centerX: Float, centerY: Float, direction: Int) {
        val pose = context.pose()
        pose.pushPose()
        pose.translate(centerX, centerY, 0.0f)
        pose.mulPose(Axis.ZP.rotationDegrees(if (direction < 0) 90.0f else -90.0f))
        pose.scale(SCALE, SCALE, 1.0f)
        blitk(
            matrixStack = pose,
            texture = CaracatronColors.icon("select_arrow"),
            x = -ARROW_WIDTH / 2,
            y = -ARROW_HEIGHT / 2,
            width = ARROW_WIDTH,
            height = ARROW_HEIGHT
        )
        pose.popPose()
    }
}
