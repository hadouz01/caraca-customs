package com.caraca.customs.client.gui.caracatron

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

/**
 * A filter of the Caracatron 3000's category bar, picked with its arrows.
 *
 * @author hadouz01
 * @since 1.15.0
 */
sealed class CaracatronFilter {

    /**
     * Whether the list is sorted by tier before the dex number, instead of only by the dex number.
     */
    open val sortsByTier: Boolean = false

    /**
     * Text shown in the category bar.
     */
    abstract fun label(): MutableComponent

    /**
     * Tooltip of the category bar.
     */
    abstract fun tooltip(): Component

    /**
     * Checks if a Pokémon of a tier is listed.
     *
     * @param tier Tier the Pokémon belongs to.
     */
    abstract fun accepts(tier: Int): Boolean

    /**
     * Every Pokémon, by national dex number.
     */
    data object AllByNumber : CaracatronFilter() {
        override fun label(): MutableComponent = Component.translatable("$KEY.all_number")
        override fun tooltip(): Component = Component.translatable("$KEY.all_number.tooltip")
        override fun accepts(tier: Int): Boolean = true
    }

    /**
     * Every Pokémon, by the tier they belong to, then by national dex number.
     */
    data object AllByTier : CaracatronFilter() {
        override val sortsByTier: Boolean = true
        override fun label(): MutableComponent = Component.translatable("$KEY.all_tier")
        override fun tooltip(): Component = Component.translatable("$KEY.all_tier.tooltip")
        override fun accepts(tier: Int): Boolean = true
    }

    /**
     * Only the Pokémon that belong to one tier.
     *
     * @property tier Listed tier.
     */
    data class Tier(val tier: Int) : CaracatronFilter() {
        override fun label(): MutableComponent = Component.translatable("text.caraca_customs.caracatron.tier", CaracatronScreen.roman(tier))
        override fun tooltip(): Component = Component.translatable(KEY)
        override fun accepts(tier: Int): Boolean = tier == this.tier
    }

    private companion object {
        const val KEY = "text.caraca_customs.caracatron.filter"
    }
}
