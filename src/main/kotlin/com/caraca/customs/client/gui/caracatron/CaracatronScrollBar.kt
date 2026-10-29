package com.caraca.customs.client.gui.caracatron

import com.cobblemon.mod.common.api.gui.blitk
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Vertical scroll bar of a Caracatron panel, scrolled with the mouse wheel or by dragging its slide.
 *
 * Positions are relative to the Caracatron's top left corner. The track can be thinner than the slide, centered under it.
 *
 * @property x Left edge of the slide.
 * @property top Top of the track.
 * @property bottom Bottom of the track, exclusive.
 * @property slideWidth Width of the slide.
 * @property trackWidth Width of the track.
 * @property minSlideHeight Shortest the slide can get with long contents.
 *
 * @author hadouz01
 * @since 1.12.0
 */
class CaracatronScrollBar(
    private val x: Int,
    private val top: Int,
    private val bottom: Int,
    private val slideWidth: Int,
    private val trackWidth: Int,
    private val minSlideHeight: Int
) {

    /**
     * How far the contents are scrolled, in pixels.
     */
    var offset: Double = 0.0
        private set

    private var contentHeight = 0
    private var viewportHeight = 0
    private var dragging = false

    private val trackHeight: Int get() = bottom - top

    private val maxScroll: Int get() = max(0, contentHeight - viewportHeight)

    private val slideHeight: Int
        get() = if (contentHeight <= viewportHeight) trackHeight
        else (trackHeight * viewportHeight / contentHeight).coerceIn(minSlideHeight, trackHeight)

    private val slideTop: Int
        get() = if (maxScroll == 0) top else top + ((trackHeight - slideHeight) * offset / maxScroll).roundToInt()

    /**
     * Updates the scrolled contents, keeping the offset inside the new limits.
     *
     * @param content Total height of the contents.
     * @param viewport Visible height of the panel.
     */
    fun resize(content: Int, viewport: Int) {
        contentHeight = content
        viewportHeight = viewport
        offset = offset.coerceIn(0.0, maxScroll.toDouble())
    }

    /**
     * Scrolls back to the top.
     */
    fun reset() {
        offset = 0.0
    }

    /**
     * Scrolls the contents, clamped to their limits.
     *
     * @param amount Pixels to scroll, positive going down.
     */
    fun scrollBy(amount: Double) {
        offset = (offset + amount).coerceIn(0.0, maxScroll.toDouble())
    }

    /**
     * Draws the track and the slide.
     *
     * @param context Current GUI graphics.
     * @param originX Caracatron's left edge on screen.
     * @param originY Caracatron's top edge on screen.
     * @param slide Slide texture.
     * @param track Track texture.
     * @param trackAlpha Opacity of the track.
     */
    fun render(context: GuiGraphics, originX: Int, originY: Int, slide: ResourceLocation, track: ResourceLocation, trackAlpha: Float = 1.0f) {
        blitk(
            matrixStack = context.pose(),
            texture = track,
            x = originX + x + (slideWidth - trackWidth) / 2,
            y = originY + top,
            width = trackWidth,
            height = trackHeight,
            alpha = trackAlpha
        )
        blitk(
            matrixStack = context.pose(),
            texture = slide,
            x = originX + x,
            y = originY + slideTop,
            width = slideWidth,
            height = slideHeight
        )
    }

    /**
     * Starts dragging when the slide is clicked.
     *
     * @param mouseX Mouse x relative to the Caracatron.
     * @param mouseY Mouse y relative to the Caracatron.
     * @return Whether the slide was grabbed.
     */
    fun startDrag(mouseX: Double, mouseY: Double): Boolean {
        dragging = maxScroll > 0 && mouseX >= x && mouseX < x + slideWidth && mouseY >= slideTop && mouseY < slideTop + slideHeight
        return dragging
    }

    /**
     * Moves the contents along with a dragged slide.
     *
     * @param deltaY Mouse movement since the last drag event.
     * @return Whether the slide is being dragged.
     */
    fun drag(deltaY: Double): Boolean {
        if (!dragging) return false
        val freeSpace = trackHeight - slideHeight
        if (freeSpace > 0) scrollBy(deltaY * maxScroll / freeSpace)
        return true
    }

    /**
     * Releases the slide.
     */
    fun stopDrag() {
        dragging = false
    }
}
