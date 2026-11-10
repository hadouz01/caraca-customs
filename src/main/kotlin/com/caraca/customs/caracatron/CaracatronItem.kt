package com.caraca.customs.caracatron

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.traceFirstEntityCollision
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level

/**
 * The Caracatron 3000 item. A quick click opens the Caracatron screen, holding the use button opens Cobblemon's scanner,
 * faster than a Pokédex. The scanner runs on the client through [scanHooks], so this class has no client code.
 *
 * @author hadouz01
 * @since 1.12.0
 */
class CaracatronItem(properties: Properties) : Item(properties) {

    /**
     * Client side of the Caracatron's scanner.
     *
     * @author hadouz01
     * @since 1.20.0
     */
    interface ScanHooks {

        /**
         * Called when the player starts using a Caracatron.
         */
        fun start()

        /**
         * Called every tick the Caracatron is held in use.
         *
         * @param user Entity using it.
         * @param ticksInUse Ticks since it started being used.
         */
        fun tick(user: LivingEntity, ticksInUse: Int)

        /**
         * Called when the player stops using it.
         */
        fun stop()
    }

    companion object {
        /**
         * Use ticks below which releasing the Caracatron opens its screen instead of ending a scan, the same as
         * Cobblemon's `PokedexUsageContext.OPEN_SCANNER_BUFFER_TICKS`.
         */
        const val OPEN_SCREEN_TICKS = 5

        private const val USE_DURATION = 72000

        /**
         * Client scanner, `null` on dedicated servers.
         */
        var scanHooks: ScanHooks? = null
    }

    override fun getUseAnimation(stack: ItemStack): UseAnim = UseAnim.TOOT_HORN

    override fun getUseDuration(stack: ItemStack, user: LivingEntity): Int = USE_DURATION

    /**
     * Starts using the Caracatron, unless the player is crouching while looking at one of their own Pokémon, which
     * Cobblemon keeps for interacting with it. Mirrors Cobblemon's Pokédex: the server starts the use, which reaches the
     * client through the player's synced data, and fails the action so no block or entity gets hit.
     */
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)

        if (player.isCrouching) {
            val pokemon = player.traceFirstEntityCollision(
                entityClass = PokemonEntity::class.java,
                maxDistance = player.entityInteractionRange().toFloat()
            )
            if (pokemon != null && pokemon.isOwnedBy(player)) return InteractionResultHolder.pass(stack)
        }

        if (level.isClientSide) scanHooks?.start()
        if (player !is ServerPlayer) return InteractionResultHolder.consume(stack)
        player.startUsingItem(hand)
        return InteractionResultHolder.fail(stack)
    }

    override fun onUseTick(level: Level, user: LivingEntity, stack: ItemStack, remainingUseTicks: Int) {
        if (!level.isClientSide && user is ServerPlayer && user.isChangingDimension) {
            user.stopUsingItem()
            return
        }
        if (level.isClientSide) scanHooks?.tick(user, getUseDuration(stack, user) - remainingUseTicks)
        super.onUseTick(level, user, stack, remainingUseTicks)
    }

    /**
     * Ends the use: the scanner closes on the client, and a quick click opens the Caracatron screen.
     */
    override fun releaseUsing(stack: ItemStack, level: Level, user: LivingEntity, remainingUseTicks: Int) {
        if (level.isClientSide) scanHooks?.stop()
        if (user is ServerPlayer && getUseDuration(stack, user) - remainingUseTicks < OPEN_SCREEN_TICKS) {
            Caracatron.open(user)
        }
        super.releaseUsing(stack, level, user, remainingUseTicks)
    }
}
