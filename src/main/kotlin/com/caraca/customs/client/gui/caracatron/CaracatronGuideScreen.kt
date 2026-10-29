package com.caraca.customs.client.gui.caracatron

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.client.CobblemonResources
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Guide page of the Caracatron 3000. Not written yet, so it only shows the empty screen of the player's color with an
 * "In construction..." notice, the page navigation to leave it and the screen color select, shared with the Caracatron
 * page ([CaracatronScreen.colorSelect]).
 *
 * @param caracatron Caracatron screen the player opened, the one the navigation goes back to.
 *
 * @author hadouz01
 * @since 1.25.0
 */
class CaracatronGuideScreen(private val caracatron: CaracatronScreen) : Screen(Component.translatable("$KEY.title")) {

    companion object {
        private const val KEY = "text.caraca_customs.caracatron.guide"

        private const val BASE_WIDTH = 345
        private const val BASE_HEIGHT = 207
        private const val NOTICE_Y = 100
    }

    private val navigation = CaracatronNavigation(CaracatronPage.GUIDE) { CaracatronPages.open(it, caracatron) }
    private val colorSelect get() = caracatron.colorSelect

    private val left: Int get() = (width - BASE_WIDTH) / 2
    private val top: Int get() = (height - BASE_HEIGHT) / 2

    override fun isPauseScreen(): Boolean = false

    override fun renderBlurredBackground(delta: Float) {}

    override fun renderMenuBackground(context: GuiGraphics) {}

    override fun renderBackground(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderTransparentBackground(context)
        val pose = context.pose()
        blitk(matrixStack = pose, texture = CaracatronPages.FRAME, x = left, y = top, width = BASE_WIDTH, height = BASE_HEIGHT)
        blitk(matrixStack = pose, texture = CaracatronColors.part(caracatron.folder, "caracatron_screen"), x = left, y = top, width = BASE_WIDTH, height = BASE_HEIGHT)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val colors = CaracatronTextColors.of(caracatron.folder)
        val x = (mouseX - left).toDouble()
        val y = (mouseY - top).toDouble()
        colors.drawScreenText(
            context,
            Component.translatable("$KEY.construction").withStyle(ChatFormatting.BOLD),
            left + BASE_WIDTH / 2,
            top + NOTICE_Y,
            font = CobblemonResources.DEFAULT_LARGE,
            centered = true
        )
        navigation.render(context, left, top, x, y, colors)
        colorSelect.render(context, left, top, x, y, colors)
        if (colorSelect.showsTooltip(x, y)) {
            context.renderTooltip(font, Component.translatable("option.caraca_customs.caracatron_color"), mouseX, mouseY)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            if (colorSelect.mouseClicked(mouseX - left, mouseY - top)) {
                CaracatronPages.playClick()
                return true
            }
            if (navigation.mouseClicked(mouseX - left, mouseY - top)) return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (colorSelect.drag(deltaY)) return true
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        colorSelect.stopDrag()
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (colorSelect.mouseScrolled(mouseX - left, mouseY - top, scrollY)) return true
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }
}
