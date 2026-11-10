package com.caraca.customs.client.gui.caracatron

import com.cobblemon.mod.common.api.berry.Berry
import com.cobblemon.mod.common.block.BerryBlock
import com.cobblemon.mod.common.client.render.atlas.CobblemonAtlases
import com.cobblemon.mod.common.client.render.models.blockbench.repository.BerryModelRepository
import com.cobblemon.mod.common.client.render.models.blockbench.setPosition
import com.cobblemon.mod.common.util.math.geometry.Axis
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.BushBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.IntegerProperty
import com.mojang.math.Axis as RotationAxis

/**
 * Growth stages of a plant planted on the ground (crops, bushes, Cobblemon's berries), drawn side by side on the materials
 * page, one block model per age over a faint square.
 *
 * Berries also get the fruit and flower models Cobblemon draws through their block entity, on every growth point.
 *
 * @property states One block state per stage, youngest first.
 *
 * @author hadouz01
 * @since 1.42.0
 */
class CaracatronGrowthStages(val states: List<BlockState>) {

    /**
     * Height of the stages, in font units.
     */
    val heightUnits: Int get() = rows * STEP - GAP

    private val perRow: Int get() = (PANEL_UNITS + GAP) / STEP

    private val rows: Int get() = (states.size + perRow - 1) / perRow

    /**
     * Draws every stage, centered on the panel, in the panel's scaled space.
     *
     * @param context Current GUI graphics.
     * @param top Top of the stages, in font units.
     * @param background ARGB color of the squares behind the models.
     */
    fun render(context: GuiGraphics, top: Int, background: Int) {
        states.forEachIndexed { index, state ->
            val row = index / perRow
            val inRow = minOf(perRow, states.size - row * perRow)
            val left = (PANEL_UNITS - (inRow * STEP - GAP)) / 2
            val x = left + index % perRow * STEP
            val y = top + row * STEP
            context.fill(x, y, x + CELL, y + CELL, background)
            renderModel(context, state, x, y)
        }
    }

    private fun renderModel(context: GuiGraphics, state: BlockState, x: Int, y: Int) {
        val pose = context.pose()
        pose.pushPose()
        pose.translate(x + CELL / 2.0f, (y + CELL - BASE_OFFSET).toFloat(), MODEL_Z)
        pose.scale(MODEL_SCALE, -MODEL_SCALE, MODEL_SCALE)
        pose.mulPose(RotationAxis.XP.rotationDegrees(TILT))
        pose.mulPose(RotationAxis.YP.rotationDegrees(TURN))
        pose.translate(-0.5f, 0.0f, -0.5f)

        Lighting.setupFor3DItems()
        val buffers = context.bufferSource()
        Minecraft.getInstance().blockRenderer.renderSingleBlock(state, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
        (state.block as? BerryBlock)?.berry()?.let { renderBerryParts(it, state.getValue(BerryBlock.AGE), pose, context) }
        context.flush()
        pose.popPose()
    }

    /**
     * Draws what Cobblemon's berry renderer adds to the block model: the sprouting fruit at the first stage, flowers and
     * fruits on every growth point at the last two.
     */
    private fun renderBerryParts(berry: Berry, age: Int, pose: PoseStack, context: GuiGraphics) {
        val buffer = context.bufferSource().getBuffer(RenderType.entityCutoutNoCull(CobblemonAtlases.BERRY_SPRITE_ATLAS.textureAtlas.location()))
        when {
            age == 0 -> {
                val model = BerryModelRepository.modelOf(berry.fruitModelIdentifier) ?: return
                val positioning = berry.stageOnePositioning
                place(model, positioning.position.x, positioning.position.y, positioning.position.z, positioning.rotation.x, positioning.rotation.y, positioning.rotation.z)
                model.render(pose, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
            }
            age > BerryBlock.MATURE_AGE -> {
                val identifier = if (age == BerryBlock.FLOWER_AGE) berry.flowerModelIdentifier else berry.fruitModelIdentifier
                val model = BerryModelRepository.modelOf(identifier) ?: return
                berry.growthPoints.forEach { point ->
                    place(model, point.position.x, point.position.y, point.position.z, point.rotation.x, point.rotation.y, point.rotation.z)
                    model.render(pose, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                }
            }
        }
    }

    private fun place(model: ModelPart, x: Double, y: Double, z: Double, rotX: Double, rotY: Double, rotZ: Double) {
        model.setRotation(Math.toRadians(180.0 - rotX).toFloat(), Math.toRadians(180.0 + rotY).toFloat(), Math.toRadians(rotZ).toFloat())
        model.setPosition(Axis.X_AXIS.ordinal, x.toFloat())
        model.setPosition(Axis.Y_AXIS.ordinal, y.toFloat())
        model.setPosition(Axis.Z_AXIS.ordinal, z.toFloat())
    }

    companion object {
        /**
         * Size of a stage's square, in font units.
         */
        const val CELL = 28

        private const val STEP = 32
        private const val GAP = STEP - CELL
        private const val PANEL_UNITS = 252
        private const val BASE_OFFSET = 6
        private const val MODEL_SCALE = 10.0f
        private const val MODEL_Z = 150.0f
        private const val TILT = 30.0f
        private const val TURN = 225.0f
        private const val AGE = "age"

        /**
         * Finds the growth stages of an item: the plant it places, or the first plant among the blocks dropping it. Only
         * plants planted on the ground with an age count (crops, bushes and berries).
         *
         * @param item Item shown on the materials page.
         * @param blocks Blocks that are, or drop, the item.
         * @return The stages, or `null` when the item isn't a plant.
         */
        fun of(item: ResourceLocation, blocks: List<ResourceLocation>): CaracatronGrowthStages? {
            val own = (BuiltInRegistries.ITEM.get(item) as? BlockItem)?.block
            val candidates = listOfNotNull(own) + blocks.map(BuiltInRegistries.BLOCK::get)
            return candidates.firstNotNullOfOrNull(::stagesOf)
        }

        private fun stagesOf(block: Block): CaracatronGrowthStages? {
            if (block !is BushBlock && block !is BerryBlock) return null
            val age = block.stateDefinition.properties.firstOrNull { it.name == AGE } as? IntegerProperty ?: return null
            val states = age.possibleValues.sorted().map { block.defaultBlockState().setValue(age, it) }
            return if (states.size > 1) CaracatronGrowthStages(states) else null
        }
    }
}
