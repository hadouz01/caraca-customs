package com.caraca.customs.balls

import com.caraca.CaracaCustoms
import com.caraca.customs.config.DynamicConfig
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagEntry
import net.minecraft.tags.TagLoader

/**
 * Picks the crafting materials of the regular and Ultra Poké Balls from the config, keeping the recipes' shape.
 *
 * Recipes ask for the `tier_1_poke_ball_materials` and `tier_x_poke_ball_materials` tags, so changing a material only
 * swaps what those tags hold when datapacks load. The corner fill adds an iron or gold ingot to the empty corners of every
 * recipe using the Ultra material.
 *
 * @author hadouz01
 * @since 1.42.0
 */
object BallMaterials {

    /**
     * Materials the regular Poké Balls can use, the default (Copper Ingot) first.
     */
    val REGULAR_OPTIONS = listOf(
        "minecraft:copper_ingot",
        "minecraft:raw_copper",
        "minecraft:iron_ingot",
        "minecraft:raw_iron",
        "minecraft:iron_nugget",
        "minecraft:cobblestone"
    )

    /**
     * Materials the Ultra Poké Balls can use, the default (Quartz) first.
     */
    val ULTRA_OPTIONS = listOf(
        "minecraft:quartz",
        "minecraft:gold_ingot",
        "minecraft:raw_gold",
        "minecraft:gold_nugget",
        "minecraft:diamond",
        "minecraft:emerald",
        "minecraft:ender_pearl",
        "minecraft:cobblestone"
    )

    /**
     * Value of the corner fill that leaves the Ultra recipes as they are.
     */
    const val NO_CORNER_FILL = "none"

    /**
     * Items that can fill the empty corners of the Ultra recipes, nothing (the default) first.
     */
    val CORNER_OPTIONS = listOf(NO_CORNER_FILL, "minecraft:iron_ingot", "minecraft:gold_ingot")

    private val REGULAR_TAG = CaracaCustoms.id("tier_1_poke_ball_materials")
    private val ULTRA_TAG = CaracaCustoms.id("tier_x_poke_ball_materials")
    private val ITEM_TAGS = Registries.tagsDirPath(Registries.ITEM)
    private const val CORNER_KEY = "~"

    /**
     * Turns a stored material into one of the options, falling back to the default.
     *
     * @param value Stored item id.
     * @param options Allowed item ids, the default first.
     */
    fun sanitize(value: String?, options: List<String>): String = options.firstOrNull { it == value?.trim()?.lowercase() } ?: options.first()

    /**
     * Names a material option by its item, for the Mod Menu selectors, or "Nothing" for [NO_CORNER_FILL].
     *
     * @param id Item id.
     */
    fun displayName(id: String): Component {
        if (id == NO_CORNER_FILL) return Component.translatable("option.caraca_customs.ultra_ball_corner_fill.none")
        val location = ResourceLocation.tryParse(id) ?: return Component.literal(id)
        return BuiltInRegistries.ITEM.get(location).description
    }

    /**
     * Swaps the content of the material tags for the configured items. Default materials keep the tags as the datapacks
     * wrote them, so packs adding items to them still work.
     *
     * @param directory Tag folder being loaded, only `tags/item` is touched.
     * @param tags Tags read from the datapacks, by id.
     */
    @JvmStatic
    fun overrideTags(directory: String, tags: MutableMap<ResourceLocation, List<TagLoader.EntryWithSource>>) {
        if (directory != ITEM_TAGS) return
        val data = DynamicConfig.data
        replaceTag(tags, REGULAR_TAG, data.regularBallMaterial, REGULAR_OPTIONS)
        replaceTag(tags, ULTRA_TAG, data.ultraBallMaterial, ULTRA_OPTIONS)
    }

    private fun replaceTag(tags: MutableMap<ResourceLocation, List<TagLoader.EntryWithSource>>, tag: ResourceLocation, value: String, options: List<String>) {
        val material = sanitize(value, options)
        if (material == options.first()) return
        val item = ResourceLocation.tryParse(material) ?: return
        tags[tag] = listOf(TagLoader.EntryWithSource(TagEntry.element(item), CaracaCustoms.MOD_ID))
    }

    /**
     * Fills the empty corners of the shaped recipes using the Ultra material with the item picked in the config, unless
     * it's [NO_CORNER_FILL]. Runs on the recipe JSONs before they're parsed.
     *
     * @param recipes Recipe JSONs read from the datapacks, by id.
     */
    @JvmStatic
    fun fillUltraCorners(recipes: Map<ResourceLocation, JsonElement>) {
        val corner = sanitize(DynamicConfig.data.ultraBallCornerFill, CORNER_OPTIONS)
        if (corner == NO_CORNER_FILL) return
        recipes.values.forEach { json ->
            val recipe = json as? JsonObject ?: return@forEach
            if (recipe.get("type")?.asString != "minecraft:crafting_shaped" || !usesUltraTag(recipe)) return@forEach
            val pattern = recipe.getAsJsonArray("pattern") ?: return@forEach
            if (pattern.size() != 3 || pattern.any { it.asString.length != 3 }) return@forEach

            val filled = JsonArray()
            pattern.forEachIndexed { row, line ->
                val chars = line.asString.toCharArray()
                if (row != 1) {
                    if (chars[0] == ' ') chars[0] = CORNER_KEY[0]
                    if (chars[2] == ' ') chars[2] = CORNER_KEY[0]
                }
                filled.add(JsonPrimitive(String(chars)))
            }
            recipe.add("pattern", filled)
            recipe.getAsJsonObject("key").add(CORNER_KEY, JsonObject().apply { addProperty("item", corner) })
        }
    }

    private fun usesUltraTag(recipe: JsonObject): Boolean {
        val key = recipe.getAsJsonObject("key") ?: return false
        return key.entrySet().any { (_, value) -> (value as? JsonObject)?.get("tag")?.asString == ULTRA_TAG.toString() }
    }
}
