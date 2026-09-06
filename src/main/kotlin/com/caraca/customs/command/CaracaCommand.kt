package com.caraca.customs.command

import com.caraca.customs.berry.BerryGrowthModifier
import com.caraca.customs.config.DynamicConfig
import com.caraca.customs.network.ServerNetworkHandler
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

/**
 * Server-side command registry and execution handler for mod management.
 *
 * Provides administrative commands under the `/caraca` node to manipulate configuration state.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object CaracaCommand {

    private const val PERMISSION_LEVEL_OP = 2

    /**
     * Registers all mod command trees with Fabric's command dispatcher.
     */
    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerCommandTree(dispatcher)
        }
    }

    private fun registerCommandTree(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("caraca")
                .requires { source -> source.hasPermission(PERMISSION_LEVEL_OP) }
                .then(
                    Commands.literal("reload")
                        .executes(::executeReload)
                )
        )
    }

    /**
     * Reloads [DynamicConfig] from disk, re-applies [BerryGrowthModifier] parameters
     * and re-syncs the new configuration to every connected client.
     *
     * @param context Command execution context containing source metadata.
     * @return Execution status code (1 for success, 0 for failure).
     */
    private fun executeReload(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source

        return runCatching {
            DynamicConfig.load()
            BerryGrowthModifier.applyGrowthConfigurations()
            ServerNetworkHandler.broadcastConfig(source.server)
        }.fold(
            onSuccess = {
                source.sendSuccess({ Component.translatable("text.caraca_customs.command.reload_success") }, true)
                1
            },
            onFailure = { throwable ->
                source.sendFailure(
                    Component.translatable("text.caraca_customs.command.reload_failure", throwable.localizedMessage)
                )
                0
            }
        )
    }
}
