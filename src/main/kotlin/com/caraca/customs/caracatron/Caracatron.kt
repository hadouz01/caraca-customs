package com.caraca.customs.caracatron

import com.caraca.CaracaCustoms
import com.caraca.customs.config.DynamicConfig
import com.caraca.customs.config.Feature
import com.caraca.customs.network.CaracatronOpenPayload
import com.caraca.customs.network.CaracatronSnackSeasoning
import com.caraca.customs.network.CaracatronSpawnEntry
import com.caraca.customs.network.CaracatronSpawnInfoPayload
import com.caraca.customs.network.CaracatronSpawnRequestPayload
import com.caraca.customs.progression.PlayerTiers
import com.caraca.customs.progression.SharedPokedex
import com.caraca.customs.progression.SpawnGroupRegistry
import com.caraca.customs.progression.SpawnTierTable
import com.caraca.customs.progression.TierSpawnLevels
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.CobblemonRecipeTypes
import com.cobblemon.mod.common.api.conditional.RegistryLikeCondition
import com.cobblemon.mod.common.api.conditional.RegistryLikeIdentifierCondition
import com.cobblemon.mod.common.api.conditional.RegistryLikeTagCondition
import com.cobblemon.mod.common.api.cooking.Seasonings
import com.cobblemon.mod.common.api.fishing.SpawnBait
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools
import com.cobblemon.mod.common.api.spawning.TimeRange
import com.cobblemon.mod.common.api.spawning.condition.SpawningCondition
import com.cobblemon.mod.common.api.spawning.detail.PokemonHerdSpawnDetail
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail
import com.cobblemon.mod.common.item.crafting.CookingPotRecipeBase
import com.cobblemon.mod.common.item.group.CobblemonItemGroups
import com.cobblemon.mod.common.pokemon.FormData
import com.cobblemon.mod.common.pokemon.Species
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeManager

/**
 * Server side of the Caracatron 3000: registers the item and builds the data its screen shows.
 *
 * Every implemented species is listed. Species with natural spawns go in the first tier they can spawn in (reduced rate
 * included, like the species surplus counts them), the others in their spawn group's tier ([SpawnTierTable.homeTierOf]).
 * With the spawn cap off, everything is tier 1.
 *
 * @author hadouz01
 * @since 1.12.0
 */
object Caracatron {

    private const val SPAWN_BAIT_PROCESSOR = "spawn_bait"

    private val SNACK_EFFECTS = listOf("typing", "egg_group", "ev", "rarity_bucket")

    /**
     * The Caracatron 3000 item.
     */
    val ITEM: Item = CaracatronItem(Item.Properties().stacksTo(1))

    /**
     * Registers the item, adds it to Cobblemon's utility tab and binds the spawn info and material requests.
     */
    fun register() {
        Registry.register(BuiltInRegistries.ITEM, CaracaCustoms.id("caracatron_3000"), ITEM)

        ItemGroupEvents.modifyEntriesEvent(CobblemonItemGroups.UTILITY_ITEMS_KEY).register { entries ->
            entries.accept(ITEM)
        }

        ServerPlayNetworking.registerGlobalReceiver(CaracatronSpawnRequestPayload.TYPE) { payload, context ->
            context.server().execute { sendSpawnInfo(context.player(), payload.species) }
        }
        CaracatronMaterials.register()
        CaracatronItemSources.register()
    }

    /**
     * Sends the player's tier progress, opening the Caracatron screen on their client.
     *
     * @param player Player using the Caracatron.
     */
    fun open(player: ServerPlayer) {
        if (!ServerPlayNetworking.canSend(player, CaracatronOpenPayload.TYPE)) return

        val playerId = player.uuid
        val info = PlayerTiers.infoFor(playerId)
        val speciesTiers = speciesTiers()
        val ownCaught = Cobblemon.playerDataManager.getPokedexData(playerId).speciesRecords
            .filter { (id, record) -> id in speciesTiers && record.getKnowledge() == PokedexEntryProgress.OWNED }
            .keys
        val worldCaught = if (info.isShared) SharedPokedex.countedSpecies().filterTo(HashSet()) { it in speciesTiers } else emptySet()
        val othersCaught = SharedPokedex.caughtByOthers(playerId).filterTo(HashSet()) { it in speciesTiers }

        ServerPlayNetworking.send(
            player,
            CaracatronOpenPayload(info.tier, info.tierCount, info.isShared, speciesTiers, ownCaught, worldCaught, othersCaught, PastureLootCompat.settings())
        )
    }

    /**
     * Ticks of scanning Cobblemon's server needs before registering a scan. A Caracatron scans
     * [com.caraca.customs.config.ConfigData.caracatronScanSpeed] times faster, so it needs that many times fewer ticks
     * (rounded up); any other scan keeps Cobblemon's.
     *
     * @param player Player finishing the scan.
     * @param ticks Ticks Cobblemon requires.
     * @return The ticks the scan needs.
     */
    @JvmStatic
    fun requiredScanTicks(player: ServerPlayer, ticks: Int): Int {
        if (!player.isUsingItem || !player.useItem.`is`(ITEM)) return ticks
        val speed = DynamicConfig.data.caracatronScanSpeed.coerceAtLeast(1)
        return (ticks + speed - 1) / speed
    }

    /**
     * Maps every implemented species, natural spawns or not, to the tier it's listed in: the first tier it can naturally
     * spawn in, reduced rates and early unlocks included, so each tier lists every species that counts towards the
     * species surplus. Species without natural spawns go to their group's tier ([homeTierOf]).
     *
     * @return Species identifiers and their tiers.
     */
    private fun speciesTiers(): Map<ResourceLocation, Int> {
        val spawnable = SpawnTierTable.spawnableSpecies().orEmpty()
        return PokemonSpecies.implemented.associate { species ->
            val id = species.resourceIdentifier
            val firstTier = if (id in spawnable) firstTierOf(id) else null
            id to (firstTier ?: homeTierOf(id))
        }
    }

    /**
     * Tier a species belongs to: its group's tier ([SpawnTierTable.homeTierOf]), or `1` for species outside every group
     * or with the spawn cap off.
     */
    private fun homeTierOf(species: ResourceLocation): Int {
        if (!Feature.SPAWN_CAP.isEnabled) return 1
        val group = SpawnGroupRegistry.groupOf(species) ?: return 1
        return SpawnTierTable.homeTierOf(group)
    }

    /**
     * First tier a species can naturally spawn in.
     *
     * @param species Species identifier.
     * @return The tier, or `null` when no tier lets it spawn.
     */
    private fun firstTierOf(species: ResourceLocation): Int? {
        return (1..PlayerTiers.tierCount).firstOrNull { multiplierAt(species, it) > 0.0f }
    }

    /**
     * Spawn rate multiplier of a species at a tier, `1` for species outside every group or with the spawn cap off.
     */
    private fun multiplierAt(species: ResourceLocation, tier: Int): Float {
        if (!Feature.SPAWN_CAP.isEnabled) return 1.0f
        val group = SpawnGroupRegistry.groupOf(species) ?: return 1.0f
        return SpawnTierTable.multiplierAt(group, tier)
    }

    /**
     * Answers a spawn info request with every natural spawn of the species and how the player's tier affects it.
     *
     * @param player Player asking for it.
     * @param species Selected species identifier.
     */
    private fun sendSpawnInfo(player: ServerPlayer, species: ResourceLocation) {
        val speciesData = PokemonSpecies.getByIdentifier(species) ?: return
        if (!ServerPlayNetworking.canSend(player, CaracatronSpawnInfoPayload.TYPE)) return

        val tier = PlayerTiers.cachedTier(player.uuid)
        val tierLevels = if (Feature.SPAWN_LEVELS.isEnabled) TierSpawnLevels.windowFor(tier).levels else null
        val details = runCatching { CobblemonSpawnPools.WORLD_SPAWN_POOL.details.toList() }.getOrDefault(emptyList())
        val entries = details.mapNotNull { detail -> entryOf(detail, species) }
        val snackRecipes = baitRecipes(player.server.recipeManager, CobblemonItems.POKE_SNACK)
        val forms = spawnFormsOf(speciesData, details)
        val hasRareSpawns = entries.any { it.bucket != "common" }
        val registries = player.server.registryAccess()
        val snackRecipe = snackRecipes.firstOrNull()?.let { CaracatronItemSources.recipeOf(it, registries) }
        val snackSeasonings = if (entries.isEmpty()) emptyList() else seasoningsFor(snackRecipes, forms, hasRareSpawns)
        val baitRecipe = baitRecipes(player.server.recipeManager, CobblemonItems.POKE_BAIT).firstOrNull()
            ?.let { CaracatronItemSources.recipeOf(it, registries) }
        val snackItems = listOfNotNull(snackRecipe, baitRecipe).flatMap { it.ingredients }.map { it.item } + snackSeasonings.map { it.item }

        ServerPlayNetworking.send(
            player,
            CaracatronSpawnInfoPayload(
                species = species,
                firstTier = firstTierOf(species) ?: 0,
                tierMultiplier = multiplierAt(species, tier),
                tierLevels = tierLevels,
                entries = entries,
                snackRecipe = snackRecipe,
                baitRecipe = baitRecipe,
                snackSeasonings = snackSeasonings,
                itemSources = CaracatronItemSources.sourcesOf(snackItems, player.server)
            )
        )
    }

    /**
     * Every cooking pot recipe that makes an item (a Poké Snack or Poké Bait) with spawn bait seasonings.
     */
    private fun baitRecipes(recipes: RecipeManager, result: Item): List<CookingPotRecipeBase> {
        val all = recipes.getAllRecipesFor(CobblemonRecipeTypes.COOKING_POT_COOKING).map { it.value() } +
            recipes.getAllRecipesFor(CobblemonRecipeTypes.COOKING_POT_SHAPELESS).map { it.value() }
        return all.filter { recipe ->
            recipe.result.`is`(result) && recipe.seasoningProcessors.any { it.type == SPAWN_BAIT_PROCESSOR }
        }
    }

    /**
     * Forms a species can spawn as with a Poké Snack, read from its single spawns (bait effects never touch herds), or
     * its standard form when it has none. Regional forms matter here, since their types and egg groups can differ.
     */
    private fun spawnFormsOf(species: Species, details: List<SpawnDetail>): Set<FormData> {
        val forms = details.asSequence()
            .filterIsInstance<PokemonSpawnDetail>()
            .filter { matches(it.pokemon, species.resourceIdentifier) }
            .map { species.getForm(it.pokemon.aspects) }
            .toSet()
        return forms.ifEmpty { setOf(species.standardForm) }
    }

    /**
     * Finds every seasoning accepted by the Poké Snack recipes whose bait effects favor one of the forms, following
     * Cobblemon's `SpawnBaitInfluence`: matching type and egg group multiply the weight, an EV effect leaves only the
     * species that yield that EV, and rarity effects favor the rarer buckets.
     *
     * @param recipes Poké Snack recipes, their seasoning tags give the accepted items.
     * @param forms Forms the species spawns as.
     * @param hasRareSpawns Whether the species has any spawn outside the common bucket.
     * @return The seasonings, types first, then egg groups, EVs and rarity.
     */
    private fun seasoningsFor(recipes: List<CookingPotRecipeBase>, forms: Set<FormData>, hasRareSpawns: Boolean): List<CaracatronSnackSeasoning> {
        val items = recipes.flatMap { BuiltInRegistries.ITEM.getTagOrEmpty(it.seasoningTag) }.distinct()
        val seasonings = items.flatMap { holder ->
            val item = holder.unwrapKey().map { it.location() }.orElse(null) ?: return@flatMap emptyList()
            Seasonings.getBaitEffectsFromItemStack(ItemStack(holder))
                .mapNotNull { effect -> seasoningOf(item, effect, forms, hasRareSpawns) }
        }
        return seasonings.distinct().sortedBy { SNACK_EFFECTS.indexOf(it.effect) }
    }

    private fun seasoningOf(item: ResourceLocation, effect: SpawnBait.Effect, forms: Set<FormData>, hasRareSpawns: Boolean): CaracatronSnackSeasoning? {
        val target = effect.subcategory?.path.orEmpty()
        val matches = when (effect.type) {
            SpawnBait.Effects.TYPING -> {
                val type = ElementalTypes.get(target)
                type != null && forms.any { type in it.types }
            }
            SpawnBait.Effects.EGG_GROUP -> {
                val eggGroup = EggGroup.fromIdentifier(target)
                eggGroup != null && forms.any { eggGroup in it.eggGroups }
            }
            SpawnBait.Effects.EV -> {
                val stat = Stats.getStat(target)
                return if (forms.any { (it.evYield[stat] ?: 0) > 0 }) {
                    CaracatronSnackSeasoning(item, effect.type.path, stat.showdownId, 0.0f)
                } else null
            }
            SpawnBait.Effects.RARITY_BUCKET -> return if (hasRareSpawns) {
                CaracatronSnackSeasoning(item, effect.type.path, "", effect.value.toFloat())
            } else null
            else -> false
        }
        return if (matches) CaracatronSnackSeasoning(item, effect.type.path, target, effect.value.toFloat()) else null
    }

    /**
     * Simplifies a spawn detail of the species. When a detail has more than one condition, their biomes are joined and
     * the other requirements come from the first one.
     *
     * @return The entry, or `null` when the detail doesn't spawn the species.
     */
    private fun entryOf(detail: SpawnDetail, species: ResourceLocation): CaracatronSpawnEntry? {
        val levels = when (detail) {
            is PokemonSpawnDetail -> {
                if (!matches(detail.pokemon, species)) return null
                detail.levelRange ?: runCatching { detail.pokemon.deriveLevelRange(null) }.getOrNull()
            }
            is PokemonHerdSpawnDetail -> {
                val member = detail.herdablePokemon.firstOrNull { matches(it.pokemon, species) } ?: return null
                member.levelRange ?: detail.levelRange
            }
            else -> return null
        }

        val condition = detail.conditions.firstOrNull()
        return CaracatronSpawnEntry(
            bucket = detail.bucket,
            position = runCatching { detail.spawnablePositionType.name }.getOrDefault(""),
            isHerd = detail is PokemonHerdSpawnDetail,
            weight = detail.weight,
            biomes = detail.conditions.flatMap { it.biomes.orEmpty() }.mapNotNull(::conditionId).distinct(),
            excludedBiomes = detail.anticonditions.flatMap { it.biomes.orEmpty() }.mapNotNull(::conditionId).distinct(),
            timeRange = condition?.timeRange?.let(::timeRangeName).orEmpty(),
            weather = condition?.let(::weatherOf).orEmpty(),
            canSeeSky = condition?.canSeeSky,
            minY = condition?.minY?.toInt(),
            maxY = condition?.maxY?.toInt(),
            structures = condition?.structures.orEmpty().map { either ->
                either.map({ it.toString() }, { "#${it.location}" })
            },
            levels = levels
        )
    }

    private fun matches(properties: PokemonProperties, species: ResourceLocation): Boolean {
        val raw = properties.species ?: return false
        return SpawnGroupRegistry.parseSpecies(raw) == species
    }

    private fun conditionId(condition: RegistryLikeCondition<*>): String? = when (condition) {
        is RegistryLikeTagCondition<*> -> "#${condition.tag.location}"
        is RegistryLikeIdentifierCondition<*> -> condition.identifier.toString()
        else -> null
    }

    private fun timeRangeName(range: TimeRange): String {
        return TimeRange.timeRanges.entries.firstOrNull { it.value == range }?.key.orEmpty()
    }

    private fun weatherOf(condition: SpawningCondition<*>): String = when {
        condition.isThundering == true -> "thunder"
        condition.isRaining == true -> "rain"
        condition.isRaining == false -> "clear"
        else -> ""
    }
}
