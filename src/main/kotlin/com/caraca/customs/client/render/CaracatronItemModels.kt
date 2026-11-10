package com.caraca.customs.client.render

import com.caraca.CaracaCustoms
import com.caraca.customs.caracatron.Caracatron
import com.caraca.customs.config.DynamicConfig
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.ModelManager
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import java.util.concurrent.CompletableFuture

/**
 * Item models of the Caracatron, like Cobblemon's Pokédex: a flat sprite in GUIs and item frames and a 3D device
 * everywhere else, swapped in by `CaracatronItemRendererMixin`.
 *
 * The 3D screen is on while the Caracatron can be opened and shows the viewer's own screen color
 * (`models/item/caracatrons/screens/<color>.json`), falling back to the default color.
 *
 * @author hadouz01
 * @since 1.18.0
 */
object CaracatronItemModels {

    private const val EXTRA_MODEL_VARIANT = "fabric_resource"
    private const val SCREENS = "models/item/caracatrons/screens"
    private const val SCREEN_MODELS = "item/caracatrons/screens"

    private val HANDHELD: ResourceLocation = CaracaCustoms.id("item/caracatron_3000_model")
    private val HANDHELD_OFF: ResourceLocation = CaracaCustoms.id("item/caracatron_3000_model_off")

    private var screenModels: Map<String, ResourceLocation> = emptyMap()

    /**
     * Loads the 3D models with the other models, listing the screen color models on every resource reload.
     */
    fun register() {
        PreparableModelLoadingPlugin.register({ resources, executor ->
            CompletableFuture.supplyAsync({
                resources.listResources(SCREENS) { it.namespace == CaracaCustoms.MOD_ID && it.path.endsWith(".json") }
                    .keys
                    .associate { id ->
                        val folder = id.path.removePrefix("$SCREENS/").removeSuffix(".json")
                        folder to CaracaCustoms.id("$SCREEN_MODELS/$folder")
                    }
                    .filterKeys { it.isNotEmpty() && '/' !in it }
            }, executor)
        }) { screens: Map<String, ResourceLocation>, context ->
            screenModels = screens
            context.addModels(HANDHELD, HANDHELD_OFF)
            context.addModels(screens.values)
        }
    }

    /**
     * Checks if a stack is a Caracatron.
     *
     * @param stack Rendered stack.
     */
    @JvmStatic
    fun isCaracatron(stack: ItemStack): Boolean = stack.`is`(Caracatron.ITEM)

    /**
     * Checks if a display context shows the flat sprite instead of the 3D model.
     *
     * @param context Display context of the render.
     */
    @JvmStatic
    fun usesSprite(context: ItemDisplayContext): Boolean {
        return context == ItemDisplayContext.GUI || context == ItemDisplayContext.FIXED
    }

    /**
     * Gets the 3D model of a Caracatron, with its screen on, in the player's screen color, when the entity holding it can
     * open it.
     *
     * @param stack Rendered Caracatron.
     * @param entity Entity holding it, if any.
     * @param models Loaded models.
     * @return The 3D model, or `null` if it failed to load, so the sprite is kept.
     */
    @JvmStatic
    fun handheld(stack: ItemStack, entity: LivingEntity?, models: ModelManager): BakedModel? {
        val canOpen = entity != null && (entity.mainHandItem === stack || (entity.offhandItem === stack && !isCaracatron(entity.mainHandItem)))
        val id = if (canOpen) screenModel() else HANDHELD_OFF
        val model = models.getModel(ModelResourceLocation(id, EXTRA_MODEL_VARIANT))
        return model.takeIf { it !== models.missingModel }
    }

    /**
     * Picks the model with the player's screen color, then the default color's, then the base model.
     */
    private fun screenModel(): ResourceLocation {
        return screenModels[DynamicConfig.data.caracatronScreenColor]
            ?: screenModels[DynamicConfig.DEFAULT_CARACATRON_COLOR]
            ?: HANDHELD
    }
}
