package com.caraca.customs.command

import com.caraca.customs.berry.BerryGrowthModifier
import com.caraca.customs.config.DynamicConfig
import com.caraca.customs.network.ServerNetworkHandler
import com.caraca.customs.progression.LevelCapHandler
import com.caraca.customs.progression.PlayerTiers
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

/**
 * Server-side command registry and execution handler for the `/caraca` node.
 *
 * - `/caraca reload` (operators): reloads the configuration.
 * - `/caraca tier`: shows the caller's progression tier.
 * - `/caraca tier <player>` (operators): shows another player's progression tier.
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
                .then(
                    Commands.literal("reload")
                        .requires { source -> source.hasPermission(PERMISSION_LEVEL_OP) }
                        .executes(::executeReload)
                )
                .then(
                    Commands.literal("tier")
                        .executes { context -> executeTier(context, context.source.playerOrException) }
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .requires { source -> source.hasPermission(PERMISSION_LEVEL_OP) }
                                .executes { context -> executeTier(context, EntityArgument.getPlayer(context, "player")) }
                        )
                )
        )
    }

    /**
     * Reloads [DynamicConfig] from disk, re-applies [BerryGrowthModifier] parameters, refreshes the online players'
     * tiers and re-syncs the new configuration to every connected client.
     *
     * @param context Command execution context containing source metadata.
     * @return Execution status code (1 for success, 0 for failure).
     */
    private fun executeReload(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source

        return runCatching {
            DynamicConfig.load()
            BerryGrowthModifier.applyGrowthConfigurations()
            LevelCapHandler.refreshTiers(source.server)
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

    /**
     * Shows a player's tier, caught species, level cap and what the next tier requires.
     *
     * @param context Command execution context containing source metadata.
     * @param target Player whose tier is shown.
     * @return Execution status code, the target's tier.
     */
    private fun executeTier(context: CommandContext<CommandSourceStack>, target: ServerPlayer): Int {
        val info = PlayerTiers.infoFor(target.uuid)
        val source = context.source

        source.sendSuccess({
            Component.translatable(
                "text.caraca_customs.command.tier.header",
                target.displayName,
                info.tier,
                info.tierCount,
                info.caughtSpecies
            )
        }, false)

        if (DynamicConfig.data.levelCapEnabled) {
            source.sendSuccess({ Component.translatable("text.caraca_customs.command.tier.level_cap", info.levelCap) }, false)
        }

        val nextCaptures = info.nextTierCaptures
        val nextLevelCap = info.nextLevelCap
        val nextLine = when {
            nextCaptures == null || nextLevelCap == null -> Component.translatable("text.caraca_customs.command.tier.max")
            DynamicConfig.data.levelCapEnabled -> Component.translatable("text.caraca_customs.command.tier.next", nextCaptures, nextLevelCap)
            else -> Component.translatable("text.caraca_customs.command.tier.next_no_cap", nextCaptures)
        }
        source.sendSuccess({ nextLine }, false)

        return info.tier
    }
}
