package com.caraca.customs.caracatron

import com.caraca.customs.network.CaracatronItemSource
import com.caraca.customs.network.CaracatronRecipe
import com.caraca.customs.network.CaracatronSnackIngredient
import com.cobblemon.mod.common.api.berry.Berries
import com.cobblemon.mod.common.api.berry.Berry
import com.cobblemon.mod.common.api.berry.spawncondition.AllBiomeCondition
import com.cobblemon.mod.common.item.crafting.CookingPotRecipe
import com.cobblemon.mod.common.item.crafting.CookingPotRecipeBase
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.tags.BiomeTags
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.ShapedRecipe

/**
 * Works out how to get the items of the Caracatron's snack tab and materials page. It runs on the server, since
 * Cobblemon doesn't send berry spawn rules to the client.
 * Berries list the Overworld biomes their groves grow in and the berries that mutate into them. Every item also gets
 * every recipe making it, the main one first, from a recipe index rebuilt on every datapack reload.
 * the first recipe making it, crafting table first, from a recipe index rebuilt on every datapack reload.
 *
 * @author hadouz01
 * @since 1.23.0
 */
object CaracatronItemSources {

    private const val GRID_SIZE = 3

    private var recipes: Map<ResourceLocation, List<Recipe<*>>>? = null

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
        return wanted.map { item -> sourceOf(item, recipes[item].orEmpty(), server) }
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

    private fun sourceOf(item: ResourceLocation, recipes: List<Recipe<*>>, server: MinecraftServer): CaracatronItemSource {
        val berry = Berries.getByIdentifier(item)
        val growsEverywhere = berry?.spawnConditions?.any { it is AllBiomeCondition } == true

        return CaracatronItemSource(
            item = item,
            growsEverywhere = growsEverywhere,
            biomes = if (berry == null || growsEverywhere) emptyList() else naturalBiomes(berry, server),
            mutations = berry?.let(::mutationsInto).orEmpty(),
            recipes = recipes.map { recipeOf(it, server.registryAccess()) }
        )
    }

    /**
     * Writes a recipe for the client: its type, counted ingredients, grid and how many items it makes.
     *
     * @param recipe Any recipe.
     * @param registries Registries of the running server, read for the result.
     */
    fun recipeOf(recipe: Recipe<*>, registries: HolderLookup.Provider): CaracatronRecipe {
        val (width, grid) = gridOf(recipe)
        val type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.type)?.toString().orEmpty()
        val count = runCatching { recipe.getResultItem(registries).count }.getOrDefault(1).coerceAtLeast(1)
        return CaracatronRecipe(type, ingredientsOf(recipe), width, grid, count)
    }

    /**
     * Lays a recipe out as a grid. Crafting table and Campfire Pot recipes always fill a 3 × 3 grid: shaped ones keep
     * their layout from the top left, shapeless ones fill it in order. Other recipes list their ingredients in rows of 3,
     * so a furnace recipe is a single slot. Tag ingredients show their first item.
     *
     * @return The grid's width and its slots, row by row.
     */
    private fun gridOf(recipe: Recipe<*>): Pair<Int, List<ResourceLocation?>> {
        val slots = recipe.ingredients.map { ingredient ->
            if (ingredient.isEmpty) null else ingredient.items.firstOrNull()?.let { BuiltInRegistries.ITEM.getKey(it.item) }
        }
        if (recipe !is CraftingRecipe && recipe !is CookingPotRecipeBase) {
            val filled = slots.filterNotNull()
            return filled.size.coerceIn(1, GRID_SIZE) to filled
        }

        val grid = arrayOfNulls<ResourceLocation>(GRID_SIZE * GRID_SIZE)
        val shapedWidth = shapedWidthOf(recipe)
        if (shapedWidth > 0) {
            slots.forEachIndexed { index, slot -> grid[index / shapedWidth * GRID_SIZE + index % shapedWidth] = slot }
        } else {
            slots.filterNotNull().take(grid.size).forEachIndexed { index, slot -> grid[index] = slot }
        }
        return GRID_SIZE to grid.toList()
    }

    private fun shapedWidthOf(recipe: Recipe<*>): Int = when (recipe) {
        is ShapedRecipe -> recipe.width
        is CookingPotRecipe -> recipe.pattern.width()
        else -> 0
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
    private fun recipesFor(items: List<ResourceLocation>, server: MinecraftServer): Map<ResourceLocation, List<Recipe<*>>> {
        val index = recipes ?: buildRecipes(server).also { recipes = it }
        return items.mapNotNull { item -> index[item]?.let { item to it } }.toMap()
    }

    /**
     * Indexes, in a single pass over the recipes, every recipe making each item: crafting table recipes first, shaped
     * before shapeless, then by id. So the custom Poké Balls start with their apricorn and tier material recipe, before
     * the color mixes and the conversions from Cobblemon's balls.
     */
    private fun buildRecipes(server: MinecraftServer): Map<ResourceLocation, List<Recipe<*>>> {
        val registries = server.registryAccess()
        val order = compareBy<RecipeHolder<*>>(
            { if (it.value().type == RecipeType.CRAFTING) 0 else 1 },
            { if (shapedWidthOf(it.value()) > 0) 0 else 1 },
            { it.id().toString() }
        )
        return server.recipeManager.recipes
            .filter { holder -> holder.value().ingredients.any { !it.isEmpty } }
            .mapNotNull { holder ->
                val result = runCatching { holder.value().getResultItem(registries) }.getOrNull()?.takeUnless { it.isEmpty }
                    ?: return@mapNotNull null
                BuiltInRegistries.ITEM.getKey(result.item) to holder
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, holders) -> holders.sortedWith(order).map { it.value() } }
    }
}
