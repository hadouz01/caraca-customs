package com.caraca.customs.client.gui.caracatron

import net.minecraft.network.chat.Component

/**
 * Pages of the Caracatron 3000, in the order the page navigation goes through them. The navigation at the top of the
 * screen points to the page before and after the current one, going round, so the Caracatron page shows "< Guide" and
 * "Pokédex >".
 *
 * @property key Name of the page in `text.caraca_customs.caracatron.page.<key>`.
 *
 * @author hadouz01
 * @since 1.25.0
 */
enum class CaracatronPage(val key: String) {
    CARACA("caraca"),
    POKEDEX("pokedex"),
    DROPS("drops"),
    MATERIALS("materials"),
    GUIDE("guide");

    /**
     * Page before this one, the last one for the first page.
     */
    val previous: CaracatronPage get() = entries[(ordinal - 1 + entries.size) % entries.size]

    /**
     * Page after this one, the first one for the last page.
     */
    val next: CaracatronPage get() = entries[(ordinal + 1) % entries.size]

    /**
     * Shown name of the page.
     */
    fun label(): Component = Component.translatable("text.caraca_customs.caracatron.page.$key")
}
