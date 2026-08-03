package com.caraca.customs.balls.modules

import com.caraca.CaracaCustoms
import com.caraca.customs.balls.modifiers.DynamicCatchRateModifier
import com.caraca.customs.balls.modifiers.ElementalBallCatchRateModifier
import com.caraca.customs.balls.modifiers.SimpleCatchRateModifier
import com.cobblemon.mod.common.api.pokeball.catching.CaptureEffect
import com.cobblemon.mod.common.api.pokeball.catching.CatchRateModifier
import com.cobblemon.mod.common.item.PokeBallItem
import com.cobblemon.mod.common.pokeball.PokeBall
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity

/**
 * Technical wrapper container holding structural definitions and mechanics for a custom or overridden Poké Ball.
 *
 * Encapsulates identifiers, dynamic modifiers, capture effect chains, and registry instances.
 *
 * @property props Immutable configuration property set defining the Poké Ball.
 * @author hadouz01
 * @since 1.0.0
 */
class ModularPokeBall(val props: CustomBallProperties) {

    /**
     * Unique identifier for this Poké Ball.
     */
    var id: ResourceLocation = resolveIdentifier(props.nameId)

    /**
     * 3D model asset path identifier.
     */
    val model3dId: ResourceLocation = resolveModelIdentifier(props)

    /**
     * Evaluated catch rate modifier logic.
     */
    val modifier: CatchRateModifier = createModifier(props)

    /**
     * Chain of capture effects executed upon successful capture.
     */
    val effects: List<CaptureEffect> = listOf(object : CaptureEffect {
        override fun apply(livingEntity: LivingEntity, pokemon: Pokemon) {}
    }) + props.extraCaptureEffects

    /**
     * Internal Cobblemon domain model representation.
     */
    val pokeBall: PokeBall = PokeBall(
        name = id,
        catchRateModifier = modifier,
        effects = effects,
        waterDragValue = 1.0f,
        model2d = id,
        model3d = model3dId,
        throwPower = 1.0f,
        ancient = false
    )

    /**
     * Lazy-initialized Minecraft item representation, exclusive to newly registered custom balls.
     * Conforms to Cobblemon 1.7.3 single-argument constructor signature.
     */
    val item: PokeBallItem by lazy {
        PokeBallItem(pokeBall)
    }

    /**
     * Resolves the main identifier for the Poké Ball.
     *
     * @param nameId Raw identifier path string.
     * @return Fully qualified [ResourceLocation].
     */
    private fun resolveIdentifier(nameId: String): ResourceLocation {
        val cleanName = nameId.lowercase()
        val namespace = CaracaCustoms.MOD_ID
        return ResourceLocation.fromNamespaceAndPath(namespace, cleanName)
    }

    /**
     * Resolves the 3D item model asset location.
     *
     * @param properties Target Poké Ball properties.
     * @return Formatted model [ResourceLocation].
     */
    private fun resolveModelIdentifier(properties: CustomBallProperties): ResourceLocation {
        val namespace = CaracaCustoms.MOD_ID
        return ResourceLocation.fromNamespaceAndPath(namespace, "item/${properties.model3dPath}_model")
    }

    /**
     * Factory helper creating appropriate catch rate modifier implementations.
     *
     * Resolves modifier dependencies dynamically using the ball identifier.
     *
     * @param properties Target Poké Ball properties.
     * @return Active [CatchRateModifier] instance.
     */
    /**
     * Factory helper creating appropriate catch rate modifier implementations.
     *
     * Resolves modifier dependencies dynamically using the ball identifier.
     *
     * @param properties Target Poké Ball properties.
     * @return Active [CatchRateModifier] instance.
     */
    private fun createModifier(properties: CustomBallProperties): CatchRateModifier {
        if (properties.elementalTypes.isEmpty()) {
            return DynamicCatchRateModifier(properties.nameId)
        }
        return ElementalBallCatchRateModifier(
            ballNameId = properties.nameId,
            types = properties.elementalTypes
        )
    }
}