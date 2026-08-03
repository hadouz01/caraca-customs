package com.caraca.customs.command

import com.caraca.customs.berry.BerryGrowthModifier
import com.caraca.customs.config.DynamicConfig
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
     *
     * Hooks directly into [CommandRegistrationCallback] during server startup.
     */
    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerCommandTree(dispatcher)
        }
    }

    /**
     * Constructs and attaches the `/caraca` command hierarchy to the dispatcher.
     *
     * @param dispatcher Target command dispatcher instance.
     */
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
     * Executes the configuration hot-reload logic and notifies the command executor.
     *
     * Reloads [DynamicConfig] from disk and re-applies [BerryGrowthModifier] runtime parameters.
     *
     * @param context Command execution context containing source metadata.
     * @return Execution status code (1 for success).
     */
    private fun executeReload(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source

        runCatching {
            DynamicConfig.load()
            BerryGrowthModifier.applyGrowthConfigurations()
        }.onSuccess {
            source.sendSuccess(
                { Component.translatable("text.caraca_customs.command.reload_success") },
                true
            )
        }.onFailure { throwable ->
            source.sendFailure(
                Component.translatable("text.caraca_customs.command.reload_failure", throwable.localizedMessage)
            )
        }

        return 1
    }
}