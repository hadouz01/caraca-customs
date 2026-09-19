package com.caraca.customs.follow

import com.caraca.customs.config.Feature
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonMemories
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ai.behavior.EntityTracker
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ai.memory.MemoryStatus
import net.minecraft.world.entity.ai.memory.WalkTarget
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Keeps sent out party Pokémon close to their owners with a follow distance based on their size and a catch-up speed
 * that grows the further behind they are, so they rarely fall far enough to be teleported.
 *
 * | Size | Height | Follow distance |
 * |---|---|---|
 * | Small | under 1 m | 2 to 5 blocks |
 * | Medium | 1 m to 2.5 m | 2 to 5 blocks |
 * | Big | 2.5 m to 5 m | 3 to 6 blocks |
 * | Extra big | over 5 m | 5 to 9 blocks |
 *
 * Height is the species' Pokédex height times the Pokémon's scale, and distances don't count the Pokémon's own width,
 * like Cobblemon's own follow behavior. Only party Pokémon of online players are touched, so wild Pokémon, NPC Pokémon
 * and pastured ones keep Cobblemon's behavior. Battling, busy, ridden and immobile Pokémon are skipped, and Cobblemon
 * still teleports them if they somehow get stuck far away.
 *
 * Does nothing while [Feature.FOLLOW_SPEED] is disabled.
 *
 * @author hadouz01
 * @since 1.8.0
 */
object FollowSpeedHandler {

    /**
     * Follow distance range of a size class, in blocks.
     *
     * @property maxHeight Highest height in meters (exclusive) of this class.
     * @property closest Distance the Pokémon walks up to when catching up.
     * @property farthest Distance after which the Pokémon starts catching up.
     */
    enum class SizeClass(val maxHeight: Float, val closest: Int, val farthest: Int) {
        SMALL(1.0f, 2, 5),
        MEDIUM(2.5f, 2, 5),
        BIG(5.0f, 3, 6),
        EXTRA_BIG(Float.MAX_VALUE, 5, 9);

        companion object {
            /**
             * Finds the size class of a height.
             *
             * @param height Height in meters.
             * @return The matching size class.
             */
            fun of(height: Float): SizeClass = entries.first { height < it.maxHeight }
        }
    }

    private const val TICK_INTERVAL = 5
    private const val COBBLEMON_FOLLOW_DISTANCE = 5.0
    private const val SUPPRESS_COBBLEMON_TICKS = 10L
    private const val BASE_SPEED = 0.4
    private const val REFERENCE_WALK_SPEED = 0.35
    private const val CATCH_UP_PER_BLOCK = 0.15
    private const val MAX_SPEED = 2.0
    private const val DECIMETERS_PER_METER = 10.0f

    /**
     * Registers the server tick listener.
     */
    fun register() {
        ServerTickEvents.END_SERVER_TICK.register(::onServerTick)
    }

    private fun onServerTick(server: MinecraftServer) {
        if (server.tickCount % TICK_INTERVAL != 0 || !Feature.FOLLOW_SPEED.isEnabled) return

        server.playerList.players.forEach { player ->
            if (player.isSpectator) return@forEach
            Cobblemon.storage.getParty(player).forEach { pokemon ->
                val entity = pokemon.entity ?: return@forEach
                if (canFollow(entity, player)) updateFollow(entity, player)
            }
        }
    }

    private fun canFollow(entity: PokemonEntity, owner: ServerPlayer): Boolean {
        if (!entity.isAlive || entity.level() != owner.level()) return false
        if (entity.isBattling || entity.isBusy || entity.isVehicle || entity.isPassenger) return false
        val moving = entity.behaviour.moving
        if (!(moving.walk.canWalk || moving.swim.canSwimInWater || moving.fly.canFly)) return false
        val brain = entity.brain
        return brain.checkMemory(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED)
    }

    private fun updateFollow(entity: PokemonEntity, owner: ServerPlayer) {
        val pokemon = entity.pokemon
        val height = pokemon.form.height / DECIMETERS_PER_METER * pokemon.scaleModifier
        val size = SizeClass.of(height)
        val radius = max(entity.boundingBox.xsize, entity.boundingBox.zsize) / 2
        val distance = entity.distanceTo(owner) - radius
        val brain = entity.brain

        if (distance > size.farthest) {
            brain.setMemory(MemoryModuleType.LOOK_TARGET, EntityTracker(owner, true))
            brain.setMemory(MemoryModuleType.WALK_TARGET, WalkTarget(owner, catchUpSpeed(entity, distance - size.farthest), size.closest + ceil(radius).toInt()))
            return
        }

        val cobblemonWouldPull = distance > COBBLEMON_FOLLOW_DISTANCE
        if (cobblemonWouldPull && brain.checkMemory(CobblemonMemories.PATH_COOLDOWN, MemoryStatus.REGISTERED)) {
            brain.setMemoryWithExpiry(CobblemonMemories.PATH_COOLDOWN, true, SUPPRESS_COBBLEMON_TICKS)
        }
    }

    /**
     * Speed multiplier used to catch up: Cobblemon's own follow speed, raised for slow walkers and growing with every
     * block beyond the follow distance.
     */
    private fun catchUpSpeed(entity: PokemonEntity, blocksBehind: Double): Float {
        val walkSpeed = entity.getAttributeValue(Attributes.MOVEMENT_SPEED).coerceAtLeast(0.01)
        val base = BASE_SPEED * max(1.0, REFERENCE_WALK_SPEED / walkSpeed)
        return min(base * (1 + CATCH_UP_PER_BLOCK * blocksBehind), MAX_SPEED).toFloat()
    }
}
