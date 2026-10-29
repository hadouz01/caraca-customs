package com.caraca.customs.caracatron

import com.caraca.customs.network.CaracatronItemSource
import com.caraca.customs.network.CaracatronSnackIngredient
import com.cobblemon.mod.common.api.berry.Berries
import com.cobblemon.mod.common.api.berry.Berry
import com.cobblemon.mod.common.api.berry.spawncondition.AllBiomeCondition
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.tags.BiomeTags
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeType

/**
 * Works out how to get the items of the Caracatron's Poké Snack tab, shown when hovering them.
 *
 * Cobblemon doesn't send berry spawn rules to the client, so this runs on the server:
 *
 * - **Berries:** the Overworld biomes their trees grow in naturally (berry groves only generate in `#minecraft:is_overworld`
 *   biomes, and each berry's spawn conditions pick among them), or every Overworld biome for `cobblemon:all_biome`
 *   berries, plus the pairs of berries that mutate into it when planted side by side.
 * - **Everything else:** the first recipe making the item, crafting table recipes first, from an index of every recipe
 *   built on the first request and dropped on every datapack reload.
 *
 * @author hadouz01
 * @since 1.23.0
 */
object CaracatronItemSources {

    private var recipes: Map<ResourceLocation, Recipe<*>>? = null

    /**
     * Drops the recipe index on every datapack reload and server stop, so it's built again from the new recipes.
     */
    fun register() {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { _, _, _ -> recipes = null }
        ServerLifecycleEvents.SERVER_STOPPED.register { recipes = null }
    }

    /**
     * Builds the sources of the given items.
     *
     * @param items Item identifiers, in the order they're sent.
     * @param server Running server, for its biomes and recipes.
     * @return One source per item.
     */
    fun sourcesOf(items: Collection<ResourceLocation>, server: MinecraftServer): List<CaracatronItemSource> {
        val wanted = items.distinct()
        val recipes = recipesFor(wanted, server)
        return wanted.map { item -> sourceOf(item, recipes[item], server) }
    }

    /**
     * Ingredients of a recipe, each one counted by how many slots take it, in the recipe's order. Tag ingredients show
     * their first item.
     *
     * @param recipe Any recipe.
     * @return The ingredients and their counts.
     */
    fun ingredientsOf(recipe: Recipe<*>): List<CaracatronSnackIngredient> {
        val counts = linkedMapOf<ResourceLocation, Int>()
        recipe.ingredients
            .filterNot { it.isEmpty }
            .mapNotNull { ingredient -> ingredient.items.firstOrNull()?.let { BuiltInRegistries.ITEM.getKey(it.item) } }
            .forEach { counts.merge(it, 1, Int::plus) }
        return counts.map { (item, count) -> CaracatronSnackIngredient(item, count) }
    }

    private fun sourceOf(item: ResourceLocation, recipe: Recipe<*>?, server: MinecraftServer): CaracatronItemSource {
        val berry = Berries.getByIdentifier(item)
        val growsEverywhere = berry?.spawnConditions?.any { it is AllBiomeCondition } == true

        return CaracatronItemSource(
            item = item,
            growsEverywhere = growsEverywhere,
            biomes = if (berry == null || growsEverywhere) emptyList() else naturalBiomes(berry, server),
            mutations = berry?.let(::mutationsInto).orEmpty(),
            recipeType = recipe?.let { BuiltInRegistries.RECIPE_TYPE.getKey(it.type)?.toString() }.orEmpty(),
            recipe = recipe?.let(::ingredientsOf).orEmpty()
        )
    }

    /**
     * Overworld biomes where any of the berry's spawn conditions lets its grove generate.
     */
    private fun naturalBiomes(berry: Berry, server: MinecraftServer): List<ResourceLocation> {
        if (berry.spawnConditions.isEmpty()) return emptyList()
        val biomes = server.registryAccess().registryOrThrow(Registries.BIOME)
        return biomes.getTagOrEmpty(BiomeTags.IS_OVERWORLD)
            .filter { holder -> berry.spawnConditions.any { it.canSpawn(berry, holder) } }
            .mapNotNull { holder -> holder.unwrapKey().orElse(null)?.location() }
            .distinct()
    }

    /**
     * Pairs of berries that mutate into the given one, each pair listed once whatever berry holds the mutation.
     */
    private fun mutationsInto(result: Berry): List<Pair<ResourceLocation, ResourceLocation>> {
        return Berries.all()
            .flatMap { berry ->
                berry.mutations.filterValues { it == result.identifier }.keys.map { partner -> itemOf(berry.identifier) to itemOf(partner) }
            }
            .map { (first, second) -> if (first.toString() <= second.toString()) first to second else second to first }
            .distinct()
    }

    private fun itemOf(berry: ResourceLocation): ResourceLocation {
        return Berries.getByIdentifier(berry)?.item()?.let { BuiltInRegistries.ITEM.getKey(it) } ?: berry
    }

    /**
     * Gets the first recipe making each of the given items, from the recipe index.
     *
     * @return Each item that has a recipe, mapped to it.
     */
    private fun recipesFor(items: List<ResourceLocation>, server: MinecraftServer): Map<ResourceLocation, Recipe<*>> {
        val index = recipes ?: buildRecipes(server).also { recipes = it }
        return items.mapNotNull { item -> index[item]?.let { item to it } }.toMap()
    }

    /**
     * Indexes, in a single pass over the recipes, the first recipe making each item, crafting table recipes first.
     */
    private fun buildRecipes(server: MinecraftServer): Map<ResourceLocation, Recipe<*>> {
        val registries = server.registryAccess()
        return server.recipeManager.recipes
            .map { it.value() }
            .filter { recipe -> recipe.ingredients.any { !it.isEmpty } }
            .mapNotNull { recipe ->
                val result = runCatching { recipe.getResultItem(registries) }.getOrNull()?.takeUnless { it.isEmpty } ?: return@mapNotNull null
                BuiltInRegistries.ITEM.getKey(result.item) to recipe
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, recipes) -> recipes.minBy { if (it.type == RecipeType.CRAFTING) 0 else 1 } }
    }
}
