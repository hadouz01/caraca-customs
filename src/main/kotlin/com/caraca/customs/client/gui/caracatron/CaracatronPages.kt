package com.caraca.customs.client.gui.caracatron

import com.caraca.CaracaCustoms
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUI
import com.cobblemon.mod.common.client.pokedex.PokedexType
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.lwjgl.opengl.GL11
import java.lang.ref.WeakReference

/**
 * Switches between the Caracatron's pages. Caraca, Drops and Materials are the same [CaracatronScreen], the Guide is
 * [CaracatronGuideScreen], and the Pokédex is Cobblemon's own, framed by `PokedexGUIMixin` through [isFramed], [render]
 * and the mouse methods below. A Pokédex opened from the item is never framed.
 *
 * @author hadouz01
 * @since 1.25.0
 */
object CaracatronPages {

    private const val BASE_WIDTH = 345
    private const val BASE_HEIGHT = 207
    private const val OVERLAY_Z = 500.0f

    /**
     * Frame of the Caracatron, drawn in place of the Pokédex's.
     */
    @JvmField
    val FRAME: ResourceLocation = CaracaCustoms.id("textures/gui/caracatron/caracatron_frame.png")

    private var framedPokedex = WeakReference<Screen>(null)
    private var pokedexCaracatron: CaracatronScreen? = null
    private var pokedexNavigation: CaracatronNavigation? = null

    /**
     * Opens a page of the Caracatron and remembers it ([CaracatronState.lastPage]), so the Caracatron opens on it next
     * time.
     *
     * @param page Page to open.
     * @param caracatron Caracatron screen the player opened, the one the Caraca page goes back to.
     * @param click Whether to play the click, off when the Caracatron opens straight on its last page.
     */
    fun open(page: CaracatronPage, caracatron: CaracatronScreen, click: Boolean = true) {
        val minecraft = Minecraft.getInstance()
        if (click) playClick()
        CaracatronState.lastPage = page
        when (page) {
            CaracatronPage.CARACA, CaracatronPage.DROPS, CaracatronPage.MATERIALS -> {
                caracatron.showPage(page)
                if (minecraft.screen !== caracatron) minecraft.setScreen(caracatron)
            }
            CaracatronPage.GUIDE -> minecraft.setScreen(CaracatronGuideScreen(caracatron))
            CaracatronPage.POKEDEX -> openPokedex(caracatron)
        }
    }

    /**
     * Plays the Pokédex's click, the sound of every button of the Caracatron.
     */
    fun playClick() {
        Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.POKEDEX_CLICK, 1.0f))
    }

    /**
     * Builds the tooltip of a part that also changes with the mouse wheel (color select, filter bar): its own text, then a
     * gray line saying the wheel works there too.
     *
     * @param text Tooltip text of the part.
     */
    fun wheelTooltip(text: Component): List<Component> {
        return listOf(text, Component.translatable("text.caraca_customs.caracatron.wheel_hint").withStyle(ChatFormatting.GRAY))
    }

    private fun openPokedex(caracatron: CaracatronScreen) {
        pokedexCaracatron = caracatron
        pokedexNavigation = CaracatronNavigation(CaracatronPage.POKEDEX) { open(it, caracatron) }
        PokedexGUI.open(CobblemonClient.clientPokedexData, PokedexType.RED, caracatron.selectedSpecies)
        framedPokedex = WeakReference(Minecraft.getInstance().screen?.takeIf { it is PokedexGUI })
    }

    /**
     * Checks if a Pokédex screen is the Caracatron's Pokédex page.
     *
     * @param screen Pokédex screen.
     */
    @JvmStatic
    fun isFramed(screen: Screen): Boolean = framedPokedex.get() === screen

    /**
     * Gets the screen color the Caracatron's Pokédex page is dressed in ([CaracatronPokedexSkin]).
     *
     * @param screen Pokédex screen.
     * @return The color's folder, or `null` when the screen isn't the Caracatron's Pokédex page.
     */
    fun colorFolder(screen: Screen): String? = if (isFramed(screen)) pokedexCaracatron?.folder else null

    /**
     * Draws the page navigation and the screen color select over the Caracatron's Pokédex page, in white like on the
     * other pages, and the color select's tooltip.
     *
     * Everything the Pokédex drew is flushed and the depth buffer cleared first, so the color select's drawer always
     * covers the Pokédex's right panel, whatever depth its models and texts were drawn at.
     *
     * @param screen Pokédex screen.
     * @param context Current GUI graphics.
     * @param mouseX Mouse x on screen.
     * @param mouseY Mouse y on screen.
     */
    @JvmStatic
    fun render(screen: Screen, context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (!isFramed(screen)) return
        val navigation = pokedexNavigation ?: return
        val caracatron = pokedexCaracatron ?: return
        val colorSelect = caracatron.colorSelect
        val left = left(screen)
        val top = top(screen)
        val x = (mouseX - left).toDouble()
        val y = (mouseY - top).toDouble()

        context.flush()
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX)
        context.pose().pushPose()
        context.pose().translate(0.0f, 0.0f, OVERLAY_Z)
        navigation.render(context, left, top, x, y)
        colorSelect.render(context, left, top, x, y, CaracatronPage.POKEDEX.label())
        context.pose().popPose()
        context.flush()

        if (colorSelect.showsTooltip(x, y)) {
            context.renderComponentTooltip(Minecraft.getInstance().font, wheelTooltip(Component.translatable("option.caraca_customs.caracatron_color")), mouseX, mouseY)
        }
    }

    /**
     * Handles a click on the Caracatron's Pokédex page: the color select first, which takes every click while open, then
     * the page navigation.
     *
     * @param screen Pokédex screen.
     * @param mouseX Mouse x on screen.
     * @param mouseY Mouse y on screen.
     * @param button Mouse button.
     * @return Whether the click was used, so the Pokédex ignores it.
     */
    @JvmStatic
    fun mouseClicked(screen: Screen, mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0 || !isFramed(screen)) return false
        val x = mouseX - left(screen)
        val y = mouseY - top(screen)
        if (pokedexCaracatron?.colorSelect?.mouseClicked(x, y) == true) {
            playClick()
            return true
        }
        return pokedexNavigation?.mouseClicked(x, y) == true
    }

    /**
     * Handles the mouse wheel on the Caracatron's Pokédex page: over the color select it switches colors, over the page
     * navigation it switches pages.
     *
     * @param screen Pokédex screen.
     * @param mouseX Mouse x on screen.
     * @param mouseY Mouse y on screen.
     * @param amount Wheel movement, positive going up.
     * @return Whether the color select or the navigation took the scroll.
     */
    @JvmStatic
    fun mouseScrolled(screen: Screen, mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!isFramed(screen)) return false
        val x = mouseX - left(screen)
        val y = mouseY - top(screen)
        if (pokedexCaracatron?.colorSelect?.mouseScrolled(x, y, amount) == true) return true
        return pokedexNavigation?.mouseScrolled(x, y, amount) == true
    }

    /**
     * Drags the color select's scroll bar on the Caracatron's Pokédex page.
     *
     * @param screen Pokédex screen.
     * @param deltaY Mouse movement since the last drag event.
     * @return Whether the scroll bar is being dragged.
     */
    @JvmStatic
    fun mouseDragged(screen: Screen, deltaY: Double): Boolean {
        if (!isFramed(screen)) return false
        return pokedexCaracatron?.colorSelect?.drag(deltaY) == true
    }

    /**
     * Releases the color select's scroll bar on the Caracatron's Pokédex page.
     *
     * @param screen Pokédex screen.
     */
    @JvmStatic
    fun mouseReleased(screen: Screen) {
        if (isFramed(screen)) pokedexCaracatron?.colorSelect?.stopDrag()
    }

    private fun left(screen: Screen): Int = (screen.width - BASE_WIDTH) / 2

    private fun top(screen: Screen): Int = (screen.height - BASE_HEIGHT) / 2
}
