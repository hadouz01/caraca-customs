package com.caraca.customs.balls.modules

import com.caraca.CaracaCustoms
import com.caraca.customs.balls.modifiers.DynamicCatchRateModifier
import com.caraca.customs.balls.modifiers.ElementalBallCatchRateModifier
import com.cobblemon.mod.common.api.pokeball.catching.CatchRateModifier
import com.cobblemon.mod.common.item.PokeBallItem
import com.cobblemon.mod.common.pokeball.PokeBall
import net.minecraft.resources.ResourceLocation

/**
 * Wrapper holding the structural definitions and mechanics of a custom Poké Ball:
 * identifiers, catch rate modifier, Cobblemon [PokeBall] model and its [PokeBallItem].
 *
 * Balls with elemental types use [ElementalBallCatchRateModifier]; balls without them use
 * [DynamicCatchRateModifier].
 *
 * @property props Immutable configuration property set defining the Poké Ball.
 *
 * @author hadouz01
 * @since 1.0.0
 */
class ModularPokeBall(val props: CustomBallProperties) {

    /**
     * Unique identifier for this Poké Ball under the mod namespace.
     */
    val id: ResourceLocation = CaracaCustoms.id(props.nameId.lowercase())

    /**
     * 3D model asset path identifier.
     */
    val model3dId: ResourceLocation = CaracaCustoms.id("item/${props.model3dPath}_model")

    /**
     * Evaluated catch rate modifier logic.
     */
    val modifier: CatchRateModifier =
        if (props.elementalTypes.isEmpty()) {
            DynamicCatchRateModifier(props.nameId)
        } else {
            ElementalBallCatchRateModifier(ballNameId = props.nameId, types = props.elementalTypes)
        }

    /**
     * Internal Cobblemon domain model representation.
     */
    val pokeBall: PokeBall = PokeBall(
        name = id,
        catchRateModifier = modifier,
        effects = props.extraCaptureEffects,
        waterDragValue = 1.0f,
        model2d = id,
        model3d = model3dId,
        throwPower = 1.0f,
        ancient = false
    )

    /**
     * Minecraft item representation of this Poké Ball.
     */
    val item: PokeBallItem by lazy { PokeBallItem(pokeBall) }
}
