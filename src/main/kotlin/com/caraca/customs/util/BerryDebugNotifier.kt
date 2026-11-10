package com.caraca.customs.util

import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity

/**
 * Debug chat messages for berry growth, kept out of the mixin so it stays small.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object BerryDebugNotifier {

    private const val DEBUG_COLOR_CODE = "§a"
    private const val RESET_COLOR_CODE = "§r"

    /**
     * Tells every player in the berry's dimension how many ticks its next stage takes.
     *
     * @param blockEntity Berry block entity.
     * @param berryName Name of the berry.
     * @param ticks Ticks the stage takes.
     * @param hasRichMulch Whether the berry has Rich Mulch.
     * @param hasGrowthMulch Whether the berry has Growth Mulch.
     */
    fun notifyStageTicksCalculated(blockEntity: BlockEntity, berryName: String, ticks: Int, hasRichMulch: Boolean,
                                   hasGrowthMulch: Boolean) {
        val world: Level = blockEntity.level ?: return
        if (world.isClientSide) return

        val server = world.server ?: return
        val durationSeconds = ticks / 20.0

        val richMulchState: String = if (hasRichMulch) "§atrue§r" else "§cfalse§r"
        val growthMulchState: String = if (hasGrowthMulch) "§atrue§r" else "§cfalse§r"

        val messageText = Component.literal(
            "§7[Caraca Customs]§r §dBerry:§r $berryName §f|§f §eNext stage in:§r $ticks ticks (${String.format("%.1f", 
                durationSeconds)}s) §f|§f §7Rich Mulch:§r $richMulchState §f|§f §7Growth Mulch:§r $growthMulchState"
        )

        server.playerList.broadcastSystemMessage(messageText, false)
    }
}