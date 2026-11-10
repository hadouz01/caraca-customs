package com.caraca.customs.client.gui.caracatron

import com.caraca.CaracaCustoms
import com.caraca.customs.config.DynamicConfig
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import java.util.Locale

/**
 * Screen colors of the Caracatron. Every folder in `textures/gui/caracatron/colors/` with a `caracatron_screen.png` is a
 * color, so resource packs can add their own. Icons and slots live in `icons/` and are the same for every color.
 *
 * @author hadouz01
 * @since 1.16.0
 */
object CaracatronColors {

    private const val ROOT = "textures/gui/caracatron"
    private const val COLORS = "$ROOT/colors"
    private const val ICONS = "$ROOT/icons"
    private const val MARKER = "caracatron_screen.png"
    private const val NAME_KEY = "option.caraca_customs.caracatron_color"

    /**
     * Lists the color folders of the loaded resources, the default color first and the others in alphabetical order.
     *
     * @return The folder names.
     */
    fun available(): List<String> {
        val folders = Minecraft.getInstance().resourceManager
            .listResources(COLORS) { it.namespace == CaracaCustoms.MOD_ID && it.path.endsWith("/$MARKER") }
            .keys
            .mapNotNull { id -> id.path.removePrefix("$COLORS/").removeSuffix("/$MARKER").takeIf { it.isNotEmpty() && '/' !in it } }
            .distinct()
        return folders.sortedWith(compareBy<String> { it != DynamicConfig.DEFAULT_CARACATRON_COLOR }.thenBy { it })
    }

    /**
     * Picks the color the screen uses: the configured one when its folder exists, otherwise the first available one.
     *
     * @param available Folders from [available].
     * @return The folder name.
     */
    fun current(available: List<String>): String {
        val configured = DynamicConfig.data.caracatronScreenColor
        return if (configured in available) configured else available.firstOrNull() ?: DynamicConfig.DEFAULT_CARACATRON_COLOR
    }

    /**
     * Saves a color as the player's choice.
     *
     * @param folder Folder name of the color.
     */
    fun select(folder: String) {
        DynamicConfig.saveCaracatronScreenColor(folder)
    }

    /**
     * Gets a screen part of a color, like `caracatron_screen` or `type_bar`.
     *
     * @param folder Folder name of the color.
     * @param name Texture name, without the extension.
     */
    fun part(folder: String, name: String): ResourceLocation = CaracaCustoms.id("$COLORS/$folder/$name.png")

    /**
     * Gets an icon shared by every color, like `medal_icon` or `pokemon_slot_t1`.
     *
     * @param name Texture name, without the extension.
     */
    fun icon(name: String): ResourceLocation = CaracaCustoms.id("$ICONS/$name.png")

    /**
     * Gets the shown name of a color: the folder name with capitalized words, the same in every language (`noir` is
     * "Noir"). Folder names can't have accents, so a resource pack can still name its color through
     * `option.caraca_customs.caracatron_color.<folder>`.
     *
     * @param folder Folder name of the color.
     * @return The name from the resource packs, or the folder name with capitalized words.
     */
    fun displayName(folder: String): Component {
        val fallback = folder.split('_', '-')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.ROOT) } }
        return Component.translatableWithFallback("$NAME_KEY.$folder", fallback)
    }
}
