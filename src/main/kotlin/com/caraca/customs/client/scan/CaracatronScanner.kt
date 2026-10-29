package com.caraca.customs.client.scan

import com.caraca.customs.caracatron.Caracatron
import com.caraca.customs.caracatron.CaracatronItem
import com.caraca.customs.config.DynamicConfig
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.pokedex.PokedexType
import com.cobblemon.mod.common.pokedex.scanner.PokedexUsageContext
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import kotlin.math.min

/**
 * Runs Cobblemon's Pokédex scanner for the Caracatron 3000.
 *
 * The scanner (overlay, zoom, focus and registering scanned Pokémon) is Cobblemon's own `PokedexUsageContext`, driven
 * like a red Pokédex, only faster by the configured scan speed. Cobblemon only lets Pokédex items through some of its
 * checks, so a few mixins treat a Caracatron in use as one: `PlayerExtensionsMixin` (the HUD and party keybinds),
 * `PokedexUsageContextMixin` (scan speed) and `CaracatronItemInHandRendererMixin` (hiding the hand while scanning), plus
 * the `cobblemon:pokedex` item tag, which keeps Cobblemon from closing the scanner every tick. On the server,
 * `FinishScanningHandlerMixin` lowers the ticks a scan needs by the same speed. A quick click opens the Caracatron screen
 * instead of Cobblemon's Pokédex, so the scanner is reset without going through `stopUsing`.
 *
 * @author hadouz01
 * @since 1.20.0
 */
object CaracatronScanner : CaracatronItem.ScanHooks {

    private val context: PokedexUsageContext get() = CobblemonClient.pokedexUsageContext

    /**
     * Plugs the scanner into the Caracatron item.
     */
    fun register() {
        CaracatronItem.scanHooks = this
    }

    override fun start() {
        context.type = PokedexType.RED
    }

    override fun tick(user: LivingEntity, ticksInUse: Int) {
        if (user is LocalPlayer) context.useTick(user, ticksInUse, true)
    }

    override fun stop() {
        context.resetState(false)
    }

    /**
     * Checks if a player is using a Caracatron.
     *
     * @param player Checked player.
     */
    @JvmStatic
    fun isUsing(player: Player): Boolean = player.isUsingItem && player.useItem.`is`(Caracatron.ITEM)

    /**
     * Adds the extra scan progress of a Caracatron before Cobblemon's own render update adds the regular one, making it
     * [com.caraca.customs.config.ConfigData.caracatronScanSpeed] times faster (synced from the server, which lowers the
     * ticks a scan needs by the same amount). Only runs once a scan started (progress above 0), since Cobblemon sends the
     * scan start to the server at 0.
     *
     * @param usageContext Cobblemon's scanner state.
     * @param tickCounter Frame timing given to the render update.
     */
    @JvmStatic
    fun speedUp(usageContext: PokedexUsageContext, tickCounter: DeltaTracker) {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        val speed = DynamicConfig.data.caracatronScanSpeed
        if (speed <= 1 || !isUsing(player) || minecraft.isPaused) return
        if (usageContext.scannedSpecies == null || usageContext.scannableEntityInFocus == null || usageContext.scanningProgress <= 0.0f) return

        val updateInterval = tickCounter.realtimeDeltaTicks / 20 * PokedexUsageContext.RENDER_UPDATES_PER_SECOND
        val limit = PokedexUsageContext.MAX_SCAN_PROGRESS + PokedexUsageContext.CENTER_INFO_DISPLAY_INTERVALS
        usageContext.scanningProgress = min(usageContext.scanningProgress + updateInterval * (speed - 1), limit)
    }
}
