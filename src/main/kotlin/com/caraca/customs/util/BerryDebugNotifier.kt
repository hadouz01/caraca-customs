package com.caraca.customs.util

import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity

/**
 * Utility helper responsible for broadcasting stage calculation debug messages to nearby players or server chat.
 *
 * Encapsulates chat notification logic to keep Mixin cognitive complexity low.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object BerryDebugNotifier {

    private const val DEBUG_COLOR_CODE = "§a"
    private const val RESET_COLOR_CODE = "§r"

    /**
     * Broadcasts the calculated growth ticks for a berry bush stage to all players in the entity's dimension.
     *
     * @param blockEntity Target block entity instance providing world context.
     * @param berryName Identifier path or display string of the berry.
     * @param ticks Calculated tick duration before modifier cancellations.
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