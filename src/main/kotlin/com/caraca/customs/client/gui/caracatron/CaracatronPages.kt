package com.caraca.customs.client.gui.caracatron

import com.caraca.CaracaCustoms
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUI
import com.cobblemon.mod.common.client.pokedex.PokedexType
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.lwjgl.opengl.GL11
import java.lang.ref.WeakReference

/**
 * Switches between the pages of the Caracatron 3000 ([CaracatronPage]), all inside the Caracatron's frame.
 *
 * - **Caraca:** the [CaracatronScreen] the player opened, kept as it was left (selection, search, filter and scroll).
 * - **Pokédex:** Cobblemon's own Pokédex, opened on the species selected in the Caracatron. `PokedexGUIMixin` draws the
 *   Caracatron frame in place of the Pokédex's, and the page navigation and the screen color select over it, through
 *   [isFramed], [render], [mouseClicked], [mouseScrolled], [mouseDragged] and [mouseReleased]. A Pokédex opened from a
 *   Pokédex item is never framed.
 * - **Drops** and **Materials:** the same [CaracatronScreen], switched to that page ([CaracatronScreen.showPage]).
 * - **Guide:** [CaracatronGuideScreen].
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
     * Opens a page of the Caracatron.
     *
     * @param page Page to open.
     * @param caracatron Caracatron screen the player opened, the one the Caraca page goes back to.
     */
    fun open(page: CaracatronPage, caracatron: CaracatronScreen) {
        val minecraft = Minecraft.getInstance()
        playClick()
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
     * Draws the page navigation and the screen color select over the Caracatron's Pokédex page, in the screen color's
     * text colors like on the other pages, and the color select's tooltip.
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
        val textColors = CaracatronTextColors.of(caracatron.folder)
        val left = left(screen)
        val top = top(screen)
        val x = (mouseX - left).toDouble()
        val y = (mouseY - top).toDouble()

        context.flush()
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX)
        context.pose().pushPose()
        context.pose().translate(0.0f, 0.0f, OVERLAY_Z)
        navigation.render(context, left, top, x, y, textColors)
        colorSelect.render(context, left, top, x, y, textColors)
        context.pose().popPose()
        context.flush()

        if (colorSelect.showsTooltip(x, y)) {
            context.renderTooltip(Minecraft.getInstance().font, Component.translatable("option.caraca_customs.caracatron_color"), mouseX, mouseY)
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
     * Scrolls the color select's drawer on the Caracatron's Pokédex page.
     *
     * @param screen Pokédex screen.
     * @param mouseX Mouse x on screen.
     * @param mouseY Mouse y on screen.
     * @param amount Wheel movement, positive going up.
     * @return Whether the drawer took the scroll.
     */
    @JvmStatic
    fun mouseScrolled(screen: Screen, mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!isFramed(screen)) return false
        return pokedexCaracatron?.colorSelect?.mouseScrolled(mouseX - left(screen), mouseY - top(screen), amount) == true
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
