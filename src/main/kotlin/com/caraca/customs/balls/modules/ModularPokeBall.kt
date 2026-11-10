package com.caraca.customs.balls.modules

import com.caraca.CaracaCustoms
import com.caraca.customs.balls.modifiers.DynamicCatchRateModifier
import com.caraca.customs.balls.modifiers.ElementalBallCatchRateModifier
import com.cobblemon.mod.common.api.pokeball.catching.CatchRateModifier
import com.cobblemon.mod.common.item.PokeBallItem
import com.cobblemon.mod.common.pokeball.PokeBall
import net.minecraft.resources.ResourceLocation

/**
 * A custom Poké Ball put together: its ids, catch rate modifier, Cobblemon [PokeBall] and [PokeBallItem]. Elemental
 * balls use [ElementalBallCatchRateModifier], the rest use [DynamicCatchRateModifier].
 *
 * @property props Properties the ball is built from.
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
     * Catch rate modifier of the ball.
     */
    val modifier: CatchRateModifier =
        if (props.elementalTypes.isEmpty()) {
            DynamicCatchRateModifier(props.nameId)
        } else {
            ElementalBallCatchRateModifier(ballNameId = props.nameId, types = props.elementalTypes)
        }

    /**
     * The ball as Cobblemon sees it.
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
     * The ball's item.
     */
    val item: PokeBallItem by lazy { PokeBallItem(pokeBall) }
}
