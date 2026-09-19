package com.caraca.customs.config

import com.caraca.CaracaCustoms
import com.mojang.serialization.MapCodec
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions
import net.minecraft.core.HolderLookup
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType

/**
 * Data conditions tied to a [Feature] toggle, both under the id `caraca_customs:feature_enabled`:
 *
 * ```json
 * { "condition": "caraca_customs:feature_enabled", "feature": "custom_balls" }
 * ```
 *
 * - As a Fabric load condition (`fabric:load_conditions`), it's checked when datapacks load, so recipes follow the
 *   toggle after a datapack reload.
 * - As a loot condition, it's checked every time loot is rolled, so loot follows the toggle right away.
 *
 * @author hadouz01
 * @since 1.7.0
 */
object FeatureConditions {

    private val ID = CaracaCustoms.id("feature_enabled")

    private val RESOURCE_TYPE: ResourceConditionType<FeatureResourceCondition> = ResourceConditionType.create(
        ID,
        Feature.CODEC.fieldOf("feature").xmap(::FeatureResourceCondition, FeatureResourceCondition::feature)
    )

    private val LOOT_CODEC: MapCodec<FeatureLootCondition> =
        Feature.CODEC.fieldOf("feature").xmap(::FeatureLootCondition, FeatureLootCondition::feature)

    private val LOOT_TYPE = LootItemConditionType(LOOT_CODEC)

    /**
     * Registers both condition types.
     */
    fun register() {
        ResourceConditions.register(RESOURCE_TYPE)
        Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, ID, LOOT_TYPE)
    }

    /**
     * Builds a loot condition for a feature, used by code-injected loot pools.
     *
     * @param feature Target feature.
     * @return A loot condition passing only while the feature is enabled.
     */
    fun lootCondition(feature: Feature): LootItemCondition.Builder = LootItemCondition.Builder { FeatureLootCondition(feature) }

    private class FeatureResourceCondition(val feature: Feature) : ResourceCondition {
        override fun getType(): ResourceConditionType<*> = RESOURCE_TYPE
        override fun test(registryLookup: HolderLookup.Provider?): Boolean = feature.isEnabled
    }

    private class FeatureLootCondition(val feature: Feature) : LootItemCondition {
        override fun getType(): LootItemConditionType = LOOT_TYPE
        override fun test(context: LootContext): Boolean = feature.isEnabled
    }
}
