package com.caraca.customs.client

import com.caraca.CaracaCustoms
import com.caraca.customs.balls.CustomBallRegistry
import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.world.item.ItemStack

/**
 * Adds the custom Poké Ball tooltips: live catch rates and colored elemental types.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CustomBallTooltipHandler {

    /**
     * Registers the tooltip callback.
     */
    fun register() {
        ItemTooltipCallback.EVENT.register { stack, _, _, lines ->
            appendBallTooltip(stack, lines)
        }
    }

    /**
     * Adds the catch rates and colored elemental types to a custom Poké Ball's tooltip.
     *
     * @param stack Hovered item.
     * @param lines Tooltip lines.
     */
    private fun appendBallTooltip(stack: ItemStack, lines: MutableList<Component>) {
        val matchingBall = CustomBallRegistry.getBallByItem(stack.item) ?: return

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
     * Writes a multiplier without trailing zeros, like `2` or `1.5`.
     *
     * @param rate Catch rate multiplier.
     */
    private fun formatMultiplier(rate: Float): String {
        return if (rate % 1.0f == 0.0f) {
            rate.toInt().toString()
        } else {
            rate.toString()
        }
    }

    /**
     * Turns the ball's types into colored type names.
     *
     * @param types Types of the ball.
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
     * RGB color of a type's name.
     *
     * @param type Elemental type.
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