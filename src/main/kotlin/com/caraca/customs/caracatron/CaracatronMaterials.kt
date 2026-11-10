package com.caraca.customs.caracatron

import com.caraca.customs.network.CaracatronMaterialBlock
import com.caraca.customs.network.CaracatronMaterialInfoPayload
import com.caraca.customs.network.CaracatronMaterialLoot
import com.caraca.customs.network.CaracatronMaterialRequestPayload
import com.caraca.customs.network.CaracatronMaterialSpot
import com.caraca.customs.network.CaracatronMaterialStructure
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.Tag
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Items
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator
import net.minecraft.world.level.levelgen.SurfaceRules
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.minecraft.world.level.levelgen.placement.PlacementModifier
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager
import net.minecraft.world.level.storage.loot.LootTable
import org.slf4j.LoggerFactory
import java.util.IdentityHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Works out where an item comes from for the Caracatron's materials page. Worldgen isn't sent to the client, so it runs
 * on the server, reading the registries through their own codecs, which covers datapacks and other mods too.
 *
 * It looks at the item's block and every block that drops it, then at placed features (ores keep their altitude and
 * frequency), the noise terrain and ore veins, structure templates ([TEMPLATE_FOLDERS] for the non-jigsaw ones) and the
 * loot tables tied to the structures or features that fill them. Structures built in code (mineshafts, strongholds,
 * temples...) have no templates, so they're skipped.
 *
 * The index is heavy, so it's built once on a low priority thread on the first request and dropped on every datapack
 * reload.
 *
 * @author hadouz01
 * @since 1.27.0
 */
object CaracatronMaterials {

    private const val MIN_TAG_BIOMES = 2
    private const val ARCHAEOLOGY = "archaeology/"
    private const val TEMPLATE_FOLDER = "structure"

    /**
     * Keys whose block states a feature only checks, replaces or places around, never generates as the feature itself.
     */
    private val SKIPPED_KEYS = setOf(
        "target", "predicate", "dirt_provider", "barrier", "replaceable", "input_predicate", "location_predicate", "position_predicate"
    )

    /**
     * Blocks of a structure template that never end up in the world as themselves.
     */
    private val IGNORED_TEMPLATE_BLOCKS = setOf("air", "cave_air", "void_air", "structure_void", "jigsaw", "structure_block")
        .map(ResourceLocation::withDefaultNamespace).toSet()

    /**
     * Template folders of Minecraft's structure types that place templates without template pools, by structure type.
     */
    private val TEMPLATE_FOLDERS = mapOf(
        "minecraft:shipwreck" to "shipwreck",
        "minecraft:igloo" to "igloo",
        "minecraft:ocean_ruin" to "underwater_ruin",
        "minecraft:ruined_portal" to "ruined_portal",
        "minecraft:end_city" to "end_city",
        "minecraft:woodland_mansion" to "woodland_mansion",
        "minecraft:nether_fossil" to "nether_fossils"
    )

    private val LOGGER = LoggerFactory.getLogger("caraca_customs/caracatron_materials")

    private val SURFACE_HEIGHTMAPS = setOf("WORLD_SURFACE_WG", "WORLD_SURFACE", "MOTION_BLOCKING", "MOTION_BLOCKING_NO_LEAVES")
    private val OCEAN_FLOOR_HEIGHTMAPS = setOf("OCEAN_FLOOR_WG", "OCEAN_FLOOR")
    private val DIMENSION_ORDER = listOf("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end")

    /**
     * Minecraft's large ore veins (`OreVeinifier`), generated in noise dimensions with ore veins on.
     */
    private val VEINS = listOf(
        Vein(listOf("copper_ore", "raw_copper_block", "granite"), 0, 50),
        Vein(listOf("deepslate_iron_ore", "raw_iron_block", "tuff"), -60, -8)
    )

    private class Vein(blocks: List<String>, val minY: Int, val maxY: Int) {
        val blocks = blocks.map(ResourceLocation::withDefaultNamespace)
    }

    /**
     * A loaded dimension.
     *
     * @property minY Lowest generated altitude.
     * @property topY Highest generated altitude.
     * @property biomes Every biome its generator can place.
     */
    private class Dimension(val id: ResourceLocation, val minY: Int, val topY: Int, val biomes: Set<ResourceLocation>)

    /**
     * A loot table filled into a block by a structure processor.
     *
     * @property block Block the processor turns into the container, when the rule says it.
     */
    private data class LootLink(val table: ResourceLocation, val block: ResourceLocation?)

    /**
     * A structure or feature filling a loot table into its blocks.
     */
    private class LootPlace(val place: ResourceLocation, val block: ResourceLocation?, val biomes: Set<ResourceLocation>)

    /**
     * Everything about a spot but its biomes, so spots that only differ by biome are merged.
     */
    private data class SpotKey(
        val block: ResourceLocation,
        val kind: String,
        val dimension: ResourceLocation,
        val surface: String,
        val altitude: Altitude?,
        val perChunk: String,
        val rarity: Int,
        val size: Int
    )

    private data class Altitude(val minY: Int?, val maxY: Int?, val peakMin: Int?, val peakMax: Int?, val biasedLow: Boolean)

    private class Placement(val height: JsonElement?, val perChunk: String, val rarity: Int, val surface: String)

    /**
     * What a structure template holds.
     *
     * @property blocks Blocks it places, jigsaw final states included.
     * @property pools Template pools its jigsaw blocks connect to.
     * @property loot Loot tables of its containers, with the container block.
     */
    private class TemplateContents(val blocks: Set<ResourceLocation>, val pools: Set<ResourceLocation>, val loot: List<LootLink>)

    /**
     * Built index of a server's worldgen and loot.
     *
     * @property dimensions Loaded dimensions, Overworld, Nether and End first.
     * @property biomeTags `is_` biome groups and their biomes.
     * @property spots Generated blocks and their spots, each with the biomes it covers.
     * @property structures Blocks and the structures they're part of, each with its biomes.
     * @property tableItems Items every loot table can give, nested tables included.
     * @property itemBlocks Blocks that are, or drop, each item.
     * @property lootPlaces Structures and features filling each loot table.
     */
    private class Index(
        val dimensions: List<Dimension>,
        val biomeTags: Map<ResourceLocation, Set<ResourceLocation>>,
        val spots: Map<ResourceLocation, Map<SpotKey, Set<ResourceLocation>>>,
        val structures: Map<ResourceLocation, Map<ResourceLocation, Set<ResourceLocation>>>,
        val tableItems: Map<ResourceLocation, Set<ResourceLocation>>,
        val itemBlocks: Map<ResourceLocation, Set<ResourceLocation>>,
        val lootPlaces: Map<ResourceLocation, List<LootPlace>>
    )

    private var index: Index? = null
    private var building: CompletableFuture<Index>? = null

    /**
     * Single daemon thread at the lowest priority building the index, so the game's own threads (and the client's
     * rendering in singleplayer) keep the processor first while it runs.
     */
    private val BUILDER: Executor by lazy {
        Executors.newSingleThreadExecutor { task ->
            Thread(task, "Caracatron materials index").apply {
                isDaemon = true
                priority = Thread.MIN_PRIORITY
            }
        }
    }

    /**
     * Binds the material requests and drops the index on every datapack reload and server stop.
     */
    fun register() {
        ServerPlayNetworking.registerGlobalReceiver(CaracatronMaterialRequestPayload.TYPE) { payload, context ->
            context.server().execute { send(context.player(), payload.item) }
        }
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { _, _, _ -> forget() }
        ServerLifecycleEvents.SERVER_STOPPED.register { forget() }
    }

    private fun forget() {
        index = null
        building = null
    }

    private fun send(player: ServerPlayer, item: ResourceLocation) {
        if (!ServerPlayNetworking.canSend(player, CaracatronMaterialInfoPayload.TYPE)) return
        if (!BuiltInRegistries.ITEM.containsKey(item)) return
        withIndex(player.server) { built ->
            if (!player.hasDisconnected()) ServerPlayNetworking.send(player, infoFor(item, built, player.server))
        }
    }

    /**
     * Runs an action with the index on the server thread, right away when it's built, otherwise once the build started
     * by the first request finishes on a background thread. A build outdated by a datapack reload is thrown away.
     */
    private fun withIndex(server: MinecraftServer, action: (Index) -> Unit) {
        index?.let {
            action(it)
            return
        }
        val future = building ?: CompletableFuture.supplyAsync({ build(server) }, BUILDER).also { building = it }
        future.whenCompleteAsync({ built, error ->
            if (error != null) {
                LOGGER.error("Couldn't index the world generation for the Caracatron materials page", error)
                if (building === future) building = null
                return@whenCompleteAsync
            }
            if (building === future) {
                index = built
                building = null
            }
            action(built)
        }, server)
    }

    /**
     * Builds where an item comes from.
     *
     * @param item Item identifier.
     * @param built Index of the server's worldgen and loot.
     * @param server Running server.
     * @return The generated blocks, the loot holding it and its recipe or berry source.
     */
    private fun infoFor(item: ResourceLocation, built: Index, server: MinecraftServer): CaracatronMaterialInfoPayload {
        val blocks = built.itemBlocks[item].orEmpty()
            .sortedWith(compareBy({ BuiltInRegistries.BLOCK.get(it).asItem() != BuiltInRegistries.ITEM.get(item) }, { it.toString() }))
            .mapNotNull { block ->
                val spots = built.spots[block].orEmpty()
                val structures = built.structures[block].orEmpty()
                if (spots.isEmpty() && structures.isEmpty()) return@mapNotNull null
                val dropsItem = BuiltInRegistries.ITEM.getKey(BuiltInRegistries.BLOCK.get(block).asItem()) != item
                CaracatronMaterialBlock(
                    block,
                    dropsItem,
                    spots.map { (key, biomes) -> spotOf(key, biomes, built) },
                    structures.flatMap { (structure, biomes) -> structuresOf(structure, biomes, built) }
                )
            }

        return CaracatronMaterialInfoPayload(
            item = item,
            blocks = blocks,
            loot = lootOf(item, built),
            source = CaracatronItemSources.sourcesOf(listOf(item), server).first()
        )
    }

    private fun spotOf(key: SpotKey, biomes: Set<ResourceLocation>, index: Index): CaracatronMaterialSpot {
        val dimension = index.dimensions.first { it.id == key.dimension }
        return CaracatronMaterialSpot(
            kind = key.kind,
            dimension = key.dimension,
            biomes = biomeList(biomes, dimension, index.biomeTags),
            surface = key.surface,
            minY = key.altitude?.minY,
            maxY = key.altitude?.maxY,
            peakMin = key.altitude?.peakMin,
            peakMax = key.altitude?.peakMax,
            biasedLow = key.altitude?.biasedLow == true,
            perChunk = key.perChunk,
            rarity = key.rarity,
            size = key.size
        )
    }

    /**
     * Writes a structure a block is part of, once per dimension its biomes are in, or once without a dimension when
     * none of them is loaded.
     */
    private fun structuresOf(structure: ResourceLocation, biomes: Set<ResourceLocation>, index: Index): List<CaracatronMaterialStructure> {
        val dimensions = index.dimensions.filter { dimension -> biomes.any { it in dimension.biomes } }
        if (dimensions.isEmpty()) return listOf(CaracatronMaterialStructure(structure, null, emptyList()))
        return dimensions.map { CaracatronMaterialStructure(structure, it.id, biomeList(biomes, it, index.biomeTags)) }
    }

    /**
     * Lists the loot found in the world holding an item: suspicious blocks and containers tied to the structures and
     * features placing them first, then the other chest tables by name.
     */
    private fun lootOf(item: ResourceLocation, index: Index): List<CaracatronMaterialLoot> {
        val loot = mutableListOf<CaracatronMaterialLoot>()
        val chests = mutableListOf<CaracatronMaterialLoot>()

        index.tableItems.filterValues { item in it }.keys.sortedBy { it.toString() }.forEach { table ->
            val path = table.path
            if (path.startsWith("blocks/") || path.startsWith("entities/") || path.startsWith("injection/")) return@forEach

            val places = index.lootPlaces[table].orEmpty()
            if (places.isEmpty()) {
                val brushed = "archaeology" in path || "fossils" in path
                when {
                    "chests/" in path -> chests += unplacedLoot(CaracatronMaterialLoot.CHEST, table)
                    brushed -> loot += unplacedLoot(CaracatronMaterialLoot.BRUSH, table)
                }
                return@forEach
            }

            places.forEach { place ->
                val brushed = place.block?.path?.startsWith("suspicious_") ?: ("archaeology" in path || "fossils" in path)
                val kind = if (brushed) CaracatronMaterialLoot.BRUSH else CaracatronMaterialLoot.CHEST
                val dimensions = index.dimensions.filter { dimension -> place.biomes.any { it in dimension.biomes } }
                if (dimensions.isEmpty()) {
                    loot += CaracatronMaterialLoot(kind, place.block, place.place, table, null, emptyList())
                }
                dimensions.forEach { dimension ->
                    val biomes = biomeList(place.biomes, dimension, index.biomeTags)
                    loot += CaracatronMaterialLoot(kind, place.block, place.place, table, dimension.id, biomes)
                }
            }
        }

        return loot.distinctBy { listOf(it.kind, it.block, it.place, it.table, it.dimension) } + chests.distinctBy { it.table.path }
    }

    private fun unplacedLoot(kind: String, table: ResourceLocation): CaracatronMaterialLoot {
        return CaracatronMaterialLoot(kind, null, null, table, null, emptyList())
    }

    /**
     * Writes a set of biomes as the biome groups fully inside it, the ones covering the most biomes still left first,
     * followed by the biomes no group covered.
     *
     * @return Biome groups (`#` prefixed) and biomes, or an empty list when they cover the whole dimension.
     */
    private fun biomeList(biomes: Set<ResourceLocation>, dimension: Dimension, tags: Map<ResourceLocation, Set<ResourceLocation>>): List<String> {
        val inside = biomes.filterTo(HashSet()) { it in dimension.biomes }
        if (inside.containsAll(dimension.biomes)) return emptyList()

        val candidates = tags.filterValues { it.size >= MIN_TAG_BIOMES && inside.containsAll(it) }.toMutableMap()
        val left = inside.toMutableSet()
        val groups = mutableListOf<String>()
        val order = compareBy<Map.Entry<ResourceLocation, Set<ResourceLocation>>>(
            { entry -> entry.value.count { it in left } },
            { -it.value.size },
            { it.key.toString() }
        )
        while (candidates.isNotEmpty()) {
            val best = candidates.entries.maxWithOrNull(order) ?: break
            if (best.value.count { it in left } < MIN_TAG_BIOMES) break
            groups += "#${best.key}"
            left -= best.value
            candidates.remove(best.key)
        }
        return groups + left.map { it.toString() }.sorted()
    }

    private fun build(server: MinecraftServer): Index {
        val registries = server.registryAccess()
        val ops = RegistryOps.create(JsonOps.INSTANCE, registries)
        val dimensions = dimensionsOf(server)
        val biomeRegistry = registries.registryOrThrow(Registries.BIOME)

        val biomeTags = biomeRegistry.tags
            .filter { it.first.location.path.startsWith("is_") }
            .toList()
            .associate { pair -> pair.first.location to pair.second.mapNotNull { it.unwrapKey().orElse(null)?.location() }.toSet() }

        val processorLoot = registries.registryOrThrow(Registries.PROCESSOR_LIST).entrySet().associate { (key, list) ->
            val links = mutableListOf<LootLink>()
            encode(StructureProcessorType.DIRECT_CODEC, list, ops)?.let { collectLoot(it, emptyMap(), links) }
            key.location().toString() to links.toList()
        }.filterValues { it.isNotEmpty() }

        val featureBiomes = IdentityHashMap<PlacedFeature, MutableSet<ResourceLocation>>()
        val usedBiomes = dimensions.flatMapTo(HashSet()) { it.biomes }
        biomeRegistry.holders().forEach { holder ->
            val biome = holder.key().location()
            if (biome !in usedBiomes) return@forEach
            holder.value().generationSettings.features().forEach { step ->
                step.forEach { feature -> featureBiomes.getOrPut(feature.value()) { HashSet() } += biome }
            }
        }

        val spots = HashMap<ResourceLocation, MutableMap<SpotKey, MutableSet<ResourceLocation>>>()
        fun addSpot(key: SpotKey, biomes: Collection<ResourceLocation>) {
            spots.getOrPut(key.block) { LinkedHashMap() }.getOrPut(key) { HashSet() } += biomes
        }

        val lootPlaces = HashMap<ResourceLocation, MutableList<LootPlace>>()
        val placedRegistry = registries.registryOrThrow(Registries.PLACED_FEATURE)

        featureBiomes.forEach { (feature, biomes) ->
            val blocks = LinkedHashSet<ResourceLocation>()
            val links = mutableListOf<LootLink>()
            readFeature(feature, ops, processorLoot, blocks, links)

            placedRegistry.getKey(feature)?.let { id ->
                links.distinct().forEach { link -> lootPlaces.getOrPut(link.table) { mutableListOf() } += LootPlace(id, link.block, biomes) }
            }
            if (blocks.isEmpty()) return@forEach

            val placement = placementOf(feature, ops)
            val size = (encode(ConfiguredFeature.DIRECT_CODEC, feature.feature().value(), ops) as? JsonObject)
                ?.let { it["config"] as? JsonObject }?.number("size") ?: 0

            dimensions.forEach { dimension ->
                val inside = biomes.filter { it in dimension.biomes }
                if (inside.isEmpty()) return@forEach
                val altitude = altitudeOf(placement.height, dimension)
                blocks.forEach { block ->
                    val key = SpotKey(
                        block = block,
                        kind = CaracatronMaterialSpot.FEATURE,
                        dimension = dimension.id,
                        surface = placement.surface,
                        altitude = altitude,
                        perChunk = if (altitude != null) placement.perChunk else "",
                        rarity = if (altitude != null) placement.rarity else 0,
                        size = if (altitude != null) size else 0
                    )
                    addSpot(key, inside)
                }
            }
        }

        addTerrain(server, dimensions, ops, ::addSpot)
        val structures = structureBlocks(server, ops, processorLoot, lootPlaces)
        addNamedArchaeology(server, featureBiomes, lootPlaces)

        val tableItems = lootTableItems(server, ops)
        val itemBlocks = HashMap<ResourceLocation, MutableSet<ResourceLocation>>()
        BuiltInRegistries.BLOCK.entrySet().forEach { (key, block) ->
            val id = key.location()
            val ownItem = block.asItem()
            if (ownItem != Items.AIR) itemBlocks.getOrPut(BuiltInRegistries.ITEM.getKey(ownItem)) { HashSet() } += id
            tableItems[block.lootTable.location()]?.forEach { item -> itemBlocks.getOrPut(item) { HashSet() } += id }
        }

        val ordered = dimensions.sortedBy { dimension ->
            DIMENSION_ORDER.indexOf(dimension.id.toString()).takeIf { it >= 0 } ?: DIMENSION_ORDER.size
        }
        val dimensionOrder = ordered.map { it.id }
        val sortedSpots = spots.mapValues { (_, blockSpots) ->
            blockSpots.entries
                .sortedWith(compareBy({ dimensionOrder.indexOf(it.key.dimension) }, { it.key.kind }, { it.key.altitude?.minY ?: Int.MIN_VALUE }))
                .associate { it.key to it.value.toSet() }
        }

        return Index(ordered, biomeTags, sortedSpots, structures, tableItems, itemBlocks, lootPlaces)
    }

    /**
     * Reads the blocks a placed feature places and the loot its structure processors fill, through every configured
     * feature it holds.
     */
    private fun readFeature(
        feature: PlacedFeature,
        ops: RegistryOps<JsonElement>,
        processorLoot: Map<String, List<LootLink>>,
        blocks: MutableSet<ResourceLocation>,
        links: MutableList<LootLink>
    ) {
        feature.getFeatures().forEach { configured ->
            encode(ConfiguredFeature.DIRECT_CODEC, configured, ops)?.let { json ->
                collectBlocks(json, blocks)
                collectLoot(json, processorLoot, links)
            }
        }
    }

    private fun dimensionsOf(server: MinecraftServer): List<Dimension> {
        return server.allLevels.map { level ->
            val generator = level.chunkSource.generator
            val biomes = generator.biomeSource.possibleBiomes().mapNotNullTo(HashSet()) { it.unwrapKey().orElse(null)?.location() }
            Dimension(level.dimension().location(), generator.minY, generator.minY + generator.genDepth - 1, biomes)
        }
    }

    /**
     * Adds the terrain of every noise dimension: its default block everywhere, its surface rule blocks in the biomes
     * their rules are limited to, and the large ore veins.
     */
    private fun addTerrain(
        server: MinecraftServer,
        dimensions: List<Dimension>,
        ops: RegistryOps<JsonElement>,
        addSpot: (SpotKey, Collection<ResourceLocation>) -> Unit
    ) {
        server.allLevels.forEach { level ->
            val generator = level.chunkSource.generator as? NoiseBasedChunkGenerator ?: return@forEach
            val dimension = dimensions.first { it.id == level.dimension().location() }
            val settings = generator.generatorSettings().value()

            fun terrain(block: ResourceLocation, biomes: Collection<ResourceLocation>) {
                addSpot(SpotKey(block, CaracatronMaterialSpot.TERRAIN, dimension.id, "", null, "", 0, 0), biomes)
            }

            terrain(BuiltInRegistries.BLOCK.getKey(settings.defaultBlock().block), dimension.biomes)

            val surface = LinkedHashMap<ResourceLocation, MutableSet<ResourceLocation>?>()
            encode(SurfaceRules.RuleSource.CODEC, settings.surfaceRule(), ops)?.let { collectSurface(it, null, surface) }
            surface.forEach { (block, biomes) -> terrain(block, biomes ?: dimension.biomes) }

            if (settings.oreVeinsEnabled()) {
                VEINS.forEach { vein ->
                    val altitude = Altitude(vein.minY, vein.maxY, null, null, false)
                    vein.blocks.forEach { block ->
                        addSpot(SpotKey(block, CaracatronMaterialSpot.VEIN, dimension.id, "", altitude, "", 0, 0), dimension.biomes)
                    }
                }
            }
        }
    }

    /**
     * Reads the blocks of every structure, tying each block to the structures it's part of and every container loot
     * (processor `append_loot` rules and template chests) to the structure holding it.
     *
     * @return Each block and the structures it's part of, with their biomes.
     */
    private fun structureBlocks(
        server: MinecraftServer,
        ops: RegistryOps<JsonElement>,
        processorLoot: Map<String, List<LootLink>>,
        lootPlaces: MutableMap<ResourceLocation, MutableList<LootPlace>>
    ): Map<ResourceLocation, Map<ResourceLocation, Set<ResourceLocation>>> {
        val registries = server.registryAccess()
        val pools = registries.registryOrThrow(Registries.TEMPLATE_POOL)
        val placed = registries.registryOrThrow(Registries.PLACED_FEATURE)
        val manager = server.structureManager
        val resources = server.resourceManager

        val processorBlocks = registries.registryOrThrow(Registries.PROCESSOR_LIST).entrySet().associate { (key, list) ->
            val blocks = HashSet<ResourceLocation>()
            encode(StructureProcessorType.DIRECT_CODEC, list, ops)?.let { collectOutputs(it, blocks) }
            key.location().toString() to blocks.toSet()
        }.filterValues { it.isNotEmpty() }

        val templates = HashMap<ResourceLocation, TemplateContents>()
        val featureBlocks = HashMap<ResourceLocation, Set<ResourceLocation>>()
        val listedTemplates by lazy { manager.listTemplates().toList() }
        val result = HashMap<ResourceLocation, MutableMap<ResourceLocation, Set<ResourceLocation>>>()

        registries.registryOrThrow(Registries.STRUCTURE).entrySet().forEach { (key, structure) ->
            val structureId = key.location()
            val json = encode(Structure.DIRECT_CODEC, structure, ops) ?: return@forEach
            val blocks = HashSet<ResourceLocation>()
            val links = mutableListOf<LootLink>()
            val templateIds = LinkedHashSet<ResourceLocation>()

            val queue = ArrayDeque<ResourceLocation>()
            forEachString(json) { _, text -> ResourceLocation.tryParse(text)?.takeIf(pools::containsKey)?.let(queue::add) }

            if (queue.isEmpty()) {
                val type = BuiltInRegistries.STRUCTURE_TYPE.getKey(structure.type())?.toString()
                TEMPLATE_FOLDERS[type]?.let { folder ->
                    templateIds += listedTemplates.filter { it.namespace == ResourceLocation.DEFAULT_NAMESPACE && it.path.startsWith("$folder/") }
                }
            }

            val visitedPools = HashSet<ResourceLocation>()
            val visitedTemplates = HashSet<ResourceLocation>()
            fun readTemplates() {
                templateIds.filter(visitedTemplates::add).forEach { template ->
                    val contents = templates.getOrPut(template) { templateContents(manager, resources, template) }
                    blocks += contents.blocks
                    links += contents.loot
                    queue += contents.pools
                }
                templateIds.clear()
            }

            readTemplates()
            while (queue.isNotEmpty()) {
                val poolId = queue.removeFirst()
                if (!visitedPools.add(poolId)) continue
                val poolJson = pools.get(poolId)?.let { encode(StructureTemplatePool.DIRECT_CODEC, it, ops) } ?: continue

                collectOutputs(poolJson, blocks)
                collectLoot(poolJson, processorLoot, links)
                forEachString(poolJson) { stringKey, text ->
                    val id = ResourceLocation.tryParse(text) ?: return@forEachString
                    when {
                        stringKey == "location" -> templateIds += id
                        stringKey == "feature" -> blocks += featureBlocks.getOrPut(id) {
                            val placedBlocks = HashSet<ResourceLocation>()
                            placed.get(id)?.let { readFeature(it, ops, emptyMap(), placedBlocks, mutableListOf()) }
                            placedBlocks
                        }
                        pools.containsKey(id) -> queue += id
                        else -> processorBlocks[text]?.let { blocks += it }
                    }
                }
                readTemplates()
            }

            blocks -= IGNORED_TEMPLATE_BLOCKS
            if (blocks.isEmpty() && links.isEmpty()) return@forEach
            val biomes = structure.biomes().mapNotNullTo(HashSet()) { it.unwrapKey().orElse(null)?.location() }
            blocks.forEach { block -> result.getOrPut(block) { LinkedHashMap() }[structureId] = biomes }
            links.distinct().forEach { link -> lootPlaces.getOrPut(link.table) { mutableListOf() } += LootPlace(structureId, link.block, biomes) }
        }
        return result
    }

    /**
     * Reads a structure template from its saved form: the blocks of every palette, and the final state, pool and loot
     * table of its block entities.
     */
    private fun templateContents(manager: StructureTemplateManager, resources: ResourceManager, id: ResourceLocation): TemplateContents {
        val tag = templateTag(manager, resources, id) ?: return TemplateContents(emptySet(), emptySet(), emptyList())
        val palettes = if (tag.contains("palettes")) {
            tag.getList("palettes", Tag.TAG_LIST.toInt()).map { it as ListTag }
        } else {
            listOf(tag.getList("palette", Tag.TAG_COMPOUND.toInt()))
        }
        val names = palettes.firstOrNull()?.map(::stateName).orEmpty()

        val blocks = HashSet<ResourceLocation>()
        palettes.forEach { palette -> palette.mapNotNullTo(blocks, ::stateName) }

        val pools = HashSet<ResourceLocation>()
        val loot = mutableListOf<LootLink>()
        tag.getList("blocks", Tag.TAG_COMPOUND.toInt()).forEach { entry ->
            val block = entry as CompoundTag
            if (!block.contains("nbt")) return@forEach
            val nbt = block.getCompound("nbt")
            if (nbt.contains("pool")) ResourceLocation.tryParse(nbt.getString("pool"))?.let { pools += it }
            if (nbt.contains("final_state")) {
                val finalState = nbt.getString("final_state").substringBefore('[').substringBefore('{')
                ResourceLocation.tryParse(finalState)?.let { blocks += it }
            }
            if (nbt.contains("LootTable")) {
                val container = names.getOrNull(block.getInt("state"))
                ResourceLocation.tryParse(nbt.getString("LootTable"))?.let { loot += LootLink(it, container) }
            }
        }
        return TemplateContents(blocks.filterTo(HashSet(), BuiltInRegistries.BLOCK::containsKey), pools, loot)
    }

    private fun stateName(state: Tag): ResourceLocation? = ResourceLocation.tryParse((state as CompoundTag).getString("Name"))

    /**
     * Reads a template's NBT straight from its `structure/<path>.nbt` resource, without building a `StructureTemplate`
     * or keeping it in the server's template cache, so indexing every structure stays light. Templates that aren't
     * resources (saved in the world's `generated` folder) go through the template manager.
     */
    private fun templateTag(manager: StructureTemplateManager, resources: ResourceManager, id: ResourceLocation): CompoundTag? {
        val file = ResourceLocation.fromNamespaceAndPath(id.namespace, "$TEMPLATE_FOLDER/${id.path}.nbt")
        val resource = resources.getResource(file).orElse(null)
        if (resource != null) return runCatching { resource.open().use { NbtIo.readCompressed(it, NbtAccounter.unlimitedHeap()) } }.getOrNull()
        return runCatching { manager.get(id).orElse(null)?.save(CompoundTag()) }.getOrNull()
    }

    /**
     * Collects the blocks structure processors output (`output_state` of their rules).
     */
    private fun collectOutputs(element: JsonElement, into: MutableSet<ResourceLocation>) {
        when {
            element.isJsonArray -> element.asJsonArray.forEach { collectOutputs(it, into) }
            element.isJsonObject -> {
                val obj = element.asJsonObject
                (obj["output_state"] as? JsonObject)?.string("Name")?.let(ResourceLocation::tryParse)
                    ?.takeIf(BuiltInRegistries.BLOCK::containsKey)?.let { into += it }
                obj.entrySet().forEach { (_, value) -> collectOutputs(value, into) }
            }
        }
    }

    /**
     * Calls an action with every text value of a JSON tree and the key holding it (`null` inside arrays at the root).
     */
    private fun forEachString(element: JsonElement, key: String? = null, action: (String?, String) -> Unit) {
        when {
            element.isJsonArray -> element.asJsonArray.forEach { forEachString(it, key, action) }
            element.isJsonObject -> element.asJsonObject.entrySet().forEach { (childKey, value) -> forEachString(value, childKey, action) }
            element is JsonPrimitive && element.isString -> action(key, element.asString)
        }
    }

    /**
     * Ties `archaeology/<name>` loot tables to the structure or placed feature named `<name>`, like Minecraft's desert
     * pyramid, desert well and ocean ruins, whose suspicious blocks are placed in code.
     */
    private fun addNamedArchaeology(
        server: MinecraftServer,
        featureBiomes: Map<PlacedFeature, Set<ResourceLocation>>,
        lootPlaces: MutableMap<ResourceLocation, MutableList<LootPlace>>
    ) {
        val registries = server.registryAccess()
        val structures = registries.registryOrThrow(Registries.STRUCTURE)
        val features = registries.registryOrThrow(Registries.PLACED_FEATURE)

        server.reloadableRegistries().getKeys(Registries.LOOT_TABLE)
            .filter { it.path.startsWith(ARCHAEOLOGY) && it !in lootPlaces }
            .forEach { table ->
                val name = ResourceLocation.fromNamespaceAndPath(table.namespace, table.path.removePrefix(ARCHAEOLOGY))
                val structure = structures.get(name)
                val biomes = structure?.biomes()?.mapNotNullTo(HashSet()) { it.unwrapKey().orElse(null)?.location() }
                    ?: features.get(name)?.let { featureBiomes[it] }
                    ?: return@forEach
                lootPlaces.getOrPut(table) { mutableListOf() } += LootPlace(name, null, biomes)
            }
    }

    /**
     * Reads the items of every loot table, following nested loot tables and expanding item tags.
     */
    private fun lootTableItems(server: MinecraftServer, ops: RegistryOps<JsonElement>): Map<ResourceLocation, Set<ResourceLocation>> {
        val reloadable = server.reloadableRegistries()
        val direct = HashMap<ResourceLocation, Set<ResourceLocation>>()
        val nested = HashMap<ResourceLocation, Set<ResourceLocation>>()

        reloadable.getKeys(Registries.LOOT_TABLE).forEach { id ->
            val table = reloadable.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id))
            val json = encode(LootTable.DIRECT_CODEC, table, ops) ?: return@forEach
            val items = HashSet<ResourceLocation>()
            val references = HashSet<ResourceLocation>()
            collectLootEntries(json, items, references)
            direct[id] = items
            nested[id] = references
        }

        val resolved = HashMap<ResourceLocation, Set<ResourceLocation>>()
        fun resolve(id: ResourceLocation, visiting: MutableSet<ResourceLocation>): Set<ResourceLocation> {
            resolved[id]?.let { return it }
            if (!visiting.add(id)) return emptySet()
            val items = HashSet(direct[id].orEmpty())
            nested[id].orEmpty().forEach { items += resolve(it, visiting) }
            visiting.remove(id)
            return items.also { resolved[id] = it }
        }
        direct.keys.forEach { resolve(it, HashSet()) }
        return resolved
    }

    private fun collectLootEntries(element: JsonElement, items: MutableSet<ResourceLocation>, references: MutableSet<ResourceLocation>) {
        when {
            element.isJsonArray -> element.asJsonArray.forEach { collectLootEntries(it, items, references) }
            element.isJsonObject -> {
                val obj = element.asJsonObject
                when (obj.string("type")) {
                    "minecraft:item" -> obj.string("name")?.let(ResourceLocation::tryParse)?.let { items += it }
                    "minecraft:tag" -> obj.string("name")?.let(ResourceLocation::tryParse)?.let { tag ->
                        BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tag)).ifPresent { holders ->
                            holders.forEach { holder -> holder.unwrapKey().ifPresent { items += it.location() } }
                        }
                    }
                    "minecraft:loot_table" -> obj.string("value")?.let(ResourceLocation::tryParse)?.let { references += it }
                }
                obj.entrySet().forEach { (_, value) -> collectLootEntries(value, items, references) }
            }
        }
    }

    /**
     * Collects the blocks of every block state in a feature's configuration, skipping [SKIPPED_KEYS].
     */
    private fun collectBlocks(element: JsonElement, into: MutableSet<ResourceLocation>) {
        when {
            element.isJsonArray -> element.asJsonArray.forEach { collectBlocks(it, into) }
            element.isJsonObject -> {
                val obj = element.asJsonObject
                obj.string("Name")?.let(ResourceLocation::tryParse)?.takeIf(BuiltInRegistries.BLOCK::containsKey)?.let { into += it }
                obj.entrySet().forEach { (key, value) -> if (key !in SKIPPED_KEYS) collectBlocks(value, into) }
            }
        }
    }

    /**
     * Collects the loot tables a structure processor fills into blocks (`append_loot` rules, with the block the rule
     * outputs), directly or through the processor lists a text names.
     *
     * @param processorLoot Loot of every processor list, by identifier.
     */
    private fun collectLoot(element: JsonElement, processorLoot: Map<String, List<LootLink>>, into: MutableCollection<LootLink>) {
        when {
            element.isJsonArray -> element.asJsonArray.forEach { collectLoot(it, processorLoot, into) }
            element.isJsonObject -> {
                val obj = element.asJsonObject
                val modifier = obj["block_entity_modifier"] as? JsonObject
                if (modifier?.string("type") == "minecraft:append_loot") {
                    modifier.string("loot_table")?.let(ResourceLocation::tryParse)?.let { table ->
                        val block = (obj["output_state"] as? JsonObject)?.string("Name")?.let(ResourceLocation::tryParse)
                        into += LootLink(table, block)
                    }
                }
                obj.entrySet().forEach { (_, value) -> collectLoot(value, processorLoot, into) }
            }
            element is JsonPrimitive && element.isString -> processorLoot[element.asString]?.let { into += it }
        }
    }

    /**
     * Collects the blocks of a surface rule, each with the biomes the conditions above it are limited to (`null` when
     * it can show up in any biome).
     */
    private fun collectSurface(element: JsonElement, biomes: List<ResourceLocation>?, into: MutableMap<ResourceLocation, MutableSet<ResourceLocation>?>) {
        when {
            element.isJsonArray -> element.asJsonArray.forEach { collectSurface(it, biomes, into) }
            element.isJsonObject -> {
                val obj = element.asJsonObject
                when (obj.string("type")) {
                    "minecraft:condition" -> {
                        val condition = obj["if_true"] as? JsonObject
                        val limited = if (condition?.string("type") == "minecraft:biome") {
                            condition["biome_is"]?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { ResourceLocation.tryParse(it.asString) }
                        } else {
                            null
                        }
                        obj["then_run"]?.let { collectSurface(it, limited ?: biomes, into) }
                    }
                    "minecraft:block" -> {
                        val block = (obj["result_state"] as? JsonObject)?.string("Name")?.let(ResourceLocation::tryParse) ?: return
                        if (biomes == null) {
                            into[block] = null
                        } else if (!into.containsKey(block)) {
                            into[block] = biomes.toMutableSet()
                        } else {
                            into[block]?.addAll(biomes)
                        }
                    }
                    else -> obj.entrySet().forEach { (_, value) -> collectSurface(value, biomes, into) }
                }
            }
        }
    }

    /**
     * Reads the placement of a feature: its height range, tries per chunk, rarity and the surface it sits on.
     */
    private fun placementOf(feature: PlacedFeature, ops: RegistryOps<JsonElement>): Placement {
        var height: JsonElement? = null
        var perChunk = ""
        var rarity = 0
        var surface = ""
        val json = encode(PlacementModifier.CODEC.listOf(), feature.placement(), ops)
        if (json != null && json.isJsonArray) {
            for (element in json.asJsonArray) {
                val modifier = element as? JsonObject ?: continue
                when (modifier.string("type")) {
                    "minecraft:height_range" -> if (height == null) height = modifier["height"]
                    "minecraft:count", "minecraft:count_on_every_layer" -> if (perChunk.isEmpty()) perChunk = intText(modifier["count"])
                    "minecraft:rarity_filter" -> if (rarity == 0) rarity = modifier.number("chance") ?: 0
                    "minecraft:heightmap" -> surface = when (modifier.string("heightmap")) {
                        in SURFACE_HEIGHTMAPS -> CaracatronMaterialSpot.SURFACE
                        in OCEAN_FLOOR_HEIGHTMAPS -> CaracatronMaterialSpot.OCEAN_FLOOR
                        else -> surface
                    }
                }
            }
        }
        return Placement(height, perChunk, rarity, surface)
    }

    /**
     * Reads a height provider in a dimension: a constant anchor, or a range with its most common altitudes when it's a
     * trapezoid (the middle, widened by its plateau) or biased to the bottom.
     */
    private fun altitudeOf(height: JsonElement?, dimension: Dimension): Altitude? {
        val obj = height as? JsonObject ?: return null
        anchorOf(obj, dimension)?.let { return Altitude(it, it, null, null, false) }
        val min = anchorOf(obj["min_inclusive"], dimension)
        val max = anchorOf(obj["max_inclusive"], dimension)
        return when (obj.string("type")) {
            "minecraft:trapezoid" -> {
                if (min == null || max == null) return Altitude(min, max, null, null, false)
                val middle = Math.floorDiv(min + max, 2)
                val plateau = (obj.number("plateau") ?: 0) / 2
                Altitude(min, max, middle - plateau, middle + plateau, false)
            }
            "minecraft:biased_to_bottom", "minecraft:very_biased_to_bottom" -> Altitude(min, max, null, null, true)
            else -> Altitude(min, max, null, null, false)
        }
    }

    private fun anchorOf(element: JsonElement?, dimension: Dimension): Int? {
        val obj = element as? JsonObject ?: return null
        obj.number("absolute")?.let { return it }
        obj.number("above_bottom")?.let { return dimension.minY + it }
        obj.number("below_top")?.let { return dimension.topY - it }
        return null
    }

    /**
     * Writes an int provider as a number or a range, like `16` or `1-3`, empty when it can't be told.
     */
    private fun intText(element: JsonElement?): String {
        if (element is JsonPrimitive && element.isNumber) return element.asInt.toString()
        val obj = element as? JsonObject ?: return ""
        obj.number("value")?.let { return it.toString() }
        obj["source"]?.let { return intText(it) }
        val min = obj.number("min_inclusive")
        val max = obj.number("max_inclusive")
        if (min == null || max == null) return ""
        return if (min == max) min.toString() else "$min-$max"
    }

    private fun <T> encode(codec: Codec<T>, value: T, ops: RegistryOps<JsonElement>): JsonElement? {
        return runCatching { codec.encodeStart(ops, value).result().orElse(null) }.getOrNull()
    }

    private fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.asString

    private fun JsonObject.number(key: String): Int? = (get(key) as? JsonPrimitive)?.takeIf { it.isNumber }?.asInt
}
