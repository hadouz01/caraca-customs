package com.caraca.customs.client

import com.caraca.CaracaCustoms
import com.caraca.customs.balls.CustomBallRegistry
import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.world.item.ItemStack

/**
 * Client-side event handler responsible for formatting and appending custom Poké Ball tooltips.
 *
 * Dynamically resolves real-time catch rate multipliers, applies hex-colored elemental types,
 * and formats translatable components without discarding nested style attributes.
 *
 * @author hadouz01
 * @since 1.0.0
 */
class CustomBallTooltipHandler : ClientModInitializer {

    /**
     * Entrypoint callback initializing client listeners.
     */
    override fun onInitializeClient() {
        registerTooltipCallback()
    }

    /**
     * Hooks into Fabric's [ItemTooltipCallback] pipeline.
     */
    private fun registerTooltipCallback() {
        ItemTooltipCallback.EVENT.register { stack, _, _, lines ->
            appendBallTooltip(stack, lines)
        }
    }

    /**
     * Appends dynamic capture rate and colorized elemental type tooltips to custom Poké Ball items.
     *
     * @param stack The targeted inventory item stack.
     * @param lines Mutable list of tooltip components attached to the item.
     */
    private fun appendBallTooltip(stack: ItemStack, lines: MutableList<Component>) {
        val registeredBalls = CustomBallRegistry.getRegisteredBalls()
        val matchingBall = registeredBalls.firstOrNull { it.item == stack.item } ?: return

        val ballId = matchingBall.props.nameId
        val config = DynamicConfig.getPokeBallConfigFor(ballId)
        val tooltipKey = "item.${CaracaCustoms.MOD_ID}.${ballId}.tooltip"

        if (!I18n.exists(tooltipKey)) return

        val formattedBoosted = formatMultiplier(config.boostedCaptureRate)
        val formattedDefault = formatMultiplier(config.defaultCaptureRate)
        val types = matchingBall.props.elementalTypes

        val arguments = buildList {
            add(formattedBoosted)
            addAll(formatElementalTypesAsComponents(types))
            add(formattedDefault)
        }.toTypedArray()

        val formattedComponent = Component.translatable(tooltipKey, *arguments)
            .withStyle(ChatFormatting.GRAY)

        lines.add(formattedComponent)
    }

    /**
     * Formats floating-point multipliers removing trailing decimals when applicable.
     *
     * @param rate Raw catch rate multiplier.
     * @return Clean string representation of the rate.
     */
    private fun formatMultiplier(rate: Float): String {
        return if (rate % 1.0f == 0.0f) {
            rate.toInt().toString()
        } else {
            rate.toString()
        }
    }

    /**
     * Maps each elemental type into individual colorized [Component] instances.
     *
     * @param types Target set of elemental types associated with the Poké Ball.
     * @return List of colorized type text components.
     */
    private fun formatElementalTypesAsComponents(types: Set<ElementalType>): List<Component> {
        return types.map { type ->
            val colorHex = resolveElementalHexColor(type)
            val typeTranslationKey = "type.${CaracaCustoms.MOD_ID}.${type.name.lowercase()}"

            Component.translatable(typeTranslationKey)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorHex)))
        }
    }

    /**
     * Resolves elemental types to precise 24-bit RGB HEX color values.
     *
     * @param type Target elemental type.
     * @return Integer representation of the target RGB color.
     */
    private fun resolveElementalHexColor(type: ElementalType): Int {
        return when (type) {
            ElementalTypes.FIRE -> 0xff7300
            ElementalTypes.WATER -> 0x0099ff
            ElementalTypes.ELECTRIC -> 0xfff700
            ElementalTypes.GRASS -> 0x27BA3B
            ElementalTypes.ICE -> 0x59ffff
            ElementalTypes.FIGHTING -> 0x962A25
            ElementalTypes.POISON -> 0xA33EA1
            ElementalTypes.GROUND -> 0xebbf78
            ElementalTypes.FLYING -> 0xB3BEF0
            ElementalTypes.PSYCHIC -> 0xF95587
            ElementalTypes.BUG -> 0xaac910
            ElementalTypes.ROCK -> 0x827156
            ElementalTypes.GHOST -> 0x735797
            ElementalTypes.DRAGON -> 0x5035fc
            ElementalTypes.DARK -> 0x70424b
            ElementalTypes.STEEL -> 0xe8e8fa
            ElementalTypes.FAIRY -> 0xff96d4
            else -> 0xd4d2a5
        }
    }
}