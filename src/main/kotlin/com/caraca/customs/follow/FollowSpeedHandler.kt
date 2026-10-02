package com.caraca.customs.follow

import com.caraca.CaracaCustoms
import com.caraca.customs.config.Feature
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonMemories
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.mainThreadRuntime
import com.cobblemon.mod.common.util.resolveFloat
import com.bedrockk.molang.Expression
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.behavior.EntityTracker
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ai.memory.MemoryStatus
import net.minecraft.world.entity.ai.memory.WalkTarget
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Keeps sent out party Pokémon close to their owners with a follow distance based on their size and a boosted speed,
 * so they keep up with a walking or sprinting owner instead of falling behind and teleporting.
 *
 * The boost is a transient multiplier on the entity's movement speed attribute, so it applies to every path the
 * Pokémon walks. Cobblemon's move control turns the attribute, the
 * walk target's speed modifier, the species' walk, swim or fly speed and the Pokémon's scale into the speed it moves
 * at, and slow species like Chikorita end up far slower than a walking player. The multiplier brings that speed up to
 * [CRUISE_SPEED] while the Pokémon is in range, and to [CATCH_UP_SPEED] (plus [CATCH_UP_PER_BLOCK] for every block
 * further behind, up to [MAX_SPEED]) while it's catching up. Fast species are never slowed down.
 *
 * Height is the species' Pokédex height times the Pokémon's scale, and distances don't count the Pokémon's own width,
 * like Cobblemon's own follow behavior. Only party Pokémon of online players are touched, so wild Pokémon, NPC Pokémon
 * and pastured ones keep Cobblemon's behavior. Battling, busy, ridden and immobile Pokémon lose the boost, and
 * Cobblemon still teleports them if they somehow get stuck far away.
 *
 * Does nothing while [Feature.FOLLOW_SPEED] is disabled, and removes every boost it applied when it gets disabled.
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
    private const val FOLLOW_SPEED_MODIFIER = 0.4f
    private const val COBBLEMON_MEDIUM_SPEED_SCALE = 2.5f
    private const val DEFAULT_MEDIUM_SPEED = 0.35f
    private const val DECIMETERS_PER_METER = 10.0f
    private const val MIN_MULTIPLIER_CHANGE = 0.01

    /**
     * Move control speed of a Pokémon with Cobblemon's default walk speed following its owner.
     */
    private const val CRUISE_SPEED = 0.245

    /**
     * Move control speed used as soon as a Pokémon starts catching up, a bit faster than a walking player.
     */
    private const val CATCH_UP_SPEED = 0.45

    /**
     * Extra move control speed for every block beyond the follow distance, so it can outrun a sprinting player.
     */
    private const val CATCH_UP_PER_BLOCK = 0.05

    /**
     * Highest move control speed the boost aims for.
     */
    private const val MAX_SPEED = 0.6

    /**
     * Highest multiplier applied to the movement speed attribute.
     */
    private const val MAX_MULTIPLIER = 8.0

    private val MODIFIER_ID = CaracaCustoms.id("follow_speed")

    private val boosted: MutableSet<PokemonEntity> = Collections.newSetFromMap(WeakHashMap())

    /**
     * Registers the server tick listener.
     */
    fun register() {
        ServerTickEvents.END_SERVER_TICK.register(::onServerTick)
    }

    private fun onServerTick(server: MinecraftServer) {
        if (server.tickCount % TICK_INTERVAL != 0) return
        if (!Feature.FOLLOW_SPEED.isEnabled) {
            if (boosted.isNotEmpty()) {
                boosted.toList().forEach(::removeBoost)
                boosted.clear()
            }
            return
        }

        val updated = HashSet<PokemonEntity>()
        server.playerList.players.forEach { player ->
            if (player.isSpectator) return@forEach
            Cobblemon.storage.getParty(player).forEach { pokemon ->
                val entity = pokemon.entity ?: return@forEach
                if (canFollow(entity, player)) {
                    updateFollow(entity, player)
                    updated += entity
                }
            }
        }

        boosted.filter { it !in updated }.forEach(::removeBoost)
        boosted.retainAll(updated)
        boosted.addAll(updated)
    }

    private fun canFollow(entity: PokemonEntity, owner: ServerPlayer): Boolean {
        if (!entity.isAlive || entity.level() != owner.level()) return false
        if (entity.isBattling || entity.isBusy || entity.isVehicle || entity.isPassenger) return false
        val moving = entity.behaviour.moving
        if (!(moving.walk.canWalk || moving.swim.canSwimInWater || moving.fly.canFly)) return false
        return entity.brain.checkMemory(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED)
    }

    private fun updateFollow(entity: PokemonEntity, owner: ServerPlayer) {
        val pokemon = entity.pokemon
        val height = pokemon.form.height / DECIMETERS_PER_METER * pokemon.scaleModifier
        val size = SizeClass.of(height)
        val radius = max(entity.boundingBox.xsize, entity.boundingBox.zsize) / 2
        val distance = entity.distanceTo(owner) - radius
        val brain = entity.brain

        if (distance > size.farthest) {
            val blocksBehind = distance - size.farthest
            applyBoost(entity, min(CATCH_UP_SPEED + CATCH_UP_PER_BLOCK * blocksBehind, MAX_SPEED))
            brain.setMemory(MemoryModuleType.LOOK_TARGET, EntityTracker(owner, true))
            brain.setMemory(MemoryModuleType.WALK_TARGET, WalkTarget(owner, FOLLOW_SPEED_MODIFIER, size.closest + ceil(radius).toInt()))
            return
        }

        applyBoost(entity, CRUISE_SPEED)
        val cobblemonWouldPull = distance > COBBLEMON_FOLLOW_DISTANCE
        if (cobblemonWouldPull && brain.checkMemory(CobblemonMemories.PATH_COOLDOWN, MemoryStatus.REGISTERED)) {
            brain.setMemoryWithExpiry(CobblemonMemories.PATH_COOLDOWN, true, SUPPRESS_COBBLEMON_TICKS)
        }
    }

    /**
     * Sets the movement speed multiplier that makes the Pokémon move at [targetSpeed] while following its owner,
     * removing it if the Pokémon is already that fast.
     */
    private fun applyBoost(entity: PokemonEntity, targetSpeed: Double) {
        val attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED) ?: return
        val currentAmount = attribute.getModifier(MODIFIER_ID)?.amount ?: 0.0
        val unboostedAttribute = attribute.value / (1 + currentAmount)
        val unboostedSpeed = unboostedAttribute * FOLLOW_SPEED_MODIFIER * mediumSpeed(entity) *
            COBBLEMON_MEDIUM_SPEED_SCALE * sqrt(entity.pokemon.effectiveScale)
        val multiplier = if (unboostedSpeed <= 0) MAX_MULTIPLIER else (targetSpeed / unboostedSpeed).coerceIn(1.0, MAX_MULTIPLIER)
        val amount = multiplier - 1

        if (amount < MIN_MULTIPLIER_CHANGE) {
            if (currentAmount != 0.0) attribute.removeModifier(MODIFIER_ID)
            return
        }
        if (abs(amount - currentAmount) < MIN_MULTIPLIER_CHANGE) return
        attribute.addOrUpdateTransientModifier(AttributeModifier(MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))
    }

    private fun removeBoost(entity: PokemonEntity) {
        entity.getAttribute(Attributes.MOVEMENT_SPEED)?.removeModifier(MODIFIER_ID)
    }

    /**
     * Species speed of the medium the Pokémon is moving through, picked the same way as Cobblemon's move control.
     */
    private fun mediumSpeed(entity: PokemonEntity): Float {
        val moving = entity.behaviour.moving
        val expression = when {
            entity.getCurrentPoseType() in setOf(PoseType.FLY, PoseType.HOVER) -> moving.fly.flySpeedHorizontal
            entity.isEyeInFluid(FluidTags.WATER) || entity.isEyeInFluid(FluidTags.LAVA) -> moving.swim.swimSpeed
            else -> moving.walk.walkSpeed
        }
        return resolve(expression)
    }

    private fun resolve(expression: Expression): Float =
        runCatching { mainThreadRuntime.resolveFloat(expression) }.getOrDefault(DEFAULT_MEDIUM_SPEED)
}
