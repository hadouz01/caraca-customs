package com.caraca.customs.network

import com.caraca.CaracaCustoms
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/**
 * One natural spawn of a species, simplified for the Caracatron 3000. Biomes and structures keep their raw identifiers,
 * tags prefixed with `#`, so the client can translate them.
 *
 * @property bucket Spawn bucket: `common`, `uncommon`, `rare` or `ultra-rare`.
 * @property position Spawnable position type, e.g. `grounded`, `submerged` or `fishing`.
 * @property isHerd Whether the species spawns as part of a herd.
 * @property weight Spawn weight inside the bucket.
 * @property biomes Biomes or biome tags the spawn needs, empty for anywhere.
 * @property excludedBiomes Biomes or biome tags the spawn avoids.
 * @property timeRange Named time range, e.g. `day`, or empty for any time.
 * @property weather `rain`, `thunder`, `clear` or empty for any weather.
 * @property canSeeSky Whether it needs (`true`) or avoids (`false`) the open sky, `null` for both.
 * @property minY Lowest spawn height, `null` for none.
 * @property maxY Highest spawn height, `null` for none.
 * @property structures Structures or structure tags the spawn needs.
 * @property levels Level range written in the spawn file, `null` when it has none.
 *
 * @author hadouz01
 * @since 1.12.0
 */
data class CaracatronSpawnEntry(
    val bucket: String,
    val position: String,
    val isHerd: Boolean,
    val weight: Float,
    val biomes: List<String>,
    val excludedBiomes: List<String>,
    val timeRange: String,
    val weather: String,
    val canSeeSky: Boolean?,
    val minY: Int?,
    val maxY: Int?,
    val structures: List<String>,
    val levels: IntRange?
) {
    /**
     * Writes this entry to a buffer.
     *
     * @param buf Target buffer.
     */
    fun write(buf: FriendlyByteBuf) {
        buf.writeUtf(bucket)
        buf.writeUtf(position)
        buf.writeBoolean(isHerd)
        buf.writeFloat(weight)
        buf.writeCollection(biomes, FriendlyByteBuf::writeUtf)
        buf.writeCollection(excludedBiomes, FriendlyByteBuf::writeUtf)
        buf.writeUtf(timeRange)
        buf.writeUtf(weather)
        buf.writeNullable(canSeeSky, FriendlyByteBuf::writeBoolean)
        buf.writeNullable(minY, FriendlyByteBuf::writeVarInt)
        buf.writeNullable(maxY, FriendlyByteBuf::writeVarInt)
        buf.writeCollection(structures, FriendlyByteBuf::writeUtf)
        buf.writeNullable(levels) { out, range ->
            out.writeVarInt(range.first)
            out.writeVarInt(range.last)
        }
    }

    companion object {
        /**
         * Reads an entry written by [write].
         *
         * @param buf Source buffer.
         * @return The decoded entry.
         */
        fun read(buf: FriendlyByteBuf): CaracatronSpawnEntry = CaracatronSpawnEntry(
            bucket = buf.readUtf(),
            position = buf.readUtf(),
            isHerd = buf.readBoolean(),
            weight = buf.readFloat(),
            biomes = buf.readList(FriendlyByteBuf::readUtf),
            excludedBiomes = buf.readList(FriendlyByteBuf::readUtf),
            timeRange = buf.readUtf(),
            weather = buf.readUtf(),
            canSeeSky = buf.readNullable(FriendlyByteBuf::readBoolean),
            minY = buf.readNullable(FriendlyByteBuf::readVarInt),
            maxY = buf.readNullable(FriendlyByteBuf::readVarInt),
            structures = buf.readList(FriendlyByteBuf::readUtf),
            levels = buf.readNullable { it.readVarInt()..it.readVarInt() }
        )
    }
}

/**
 * One ingredient of the Poké Snack recipe, shown on the Caracatron's snack tab.
 *
 * @property item Item identifier, the first item that fits the ingredient.
 * @property count How many slots of the recipe take it.
 *
 * @author hadouz01
 * @since 1.15.0
 */
data class CaracatronSnackIngredient(val item: ResourceLocation, val count: Int) {

    /**
     * Writes this ingredient to a buffer.
     *
     * @param buf Target buffer.
     */
    fun write(buf: FriendlyByteBuf) {
        buf.writeResourceLocation(item)
        buf.writeVarInt(count)
    }

    companion object {
        /**
         * Reads an ingredient written by [write].
         *
         * @param buf Source buffer.
         * @return The decoded ingredient.
         */
        fun read(buf: FriendlyByteBuf): CaracatronSnackIngredient = CaracatronSnackIngredient(
            item = buf.readResourceLocation(),
            count = buf.readVarInt()
        )
    }
}

/**
 * A Poké Snack seasoning that attracts a species, shown on the Caracatron's snack tab.
 *
 * @property item Seasoning item identifier.
 * @property effect Bait effect that matches the species: `typing`, `egg_group`, `ev` or `rarity_bucket`.
 * @property target What the effect matched: the type, egg group or stat name, empty for `rarity_bucket`.
 * @property value Weight multiplier of `typing` and `egg_group` effects, or the lure tier of `rarity_bucket` effects.
 *
 * @author hadouz01
 * @since 1.15.0
 */
data class CaracatronSnackSeasoning(val item: ResourceLocation, val effect: String, val target: String, val value: Float) {

    /**
     * Writes this seasoning to a buffer.
     *
     * @param buf Target buffer.
     */
    fun write(buf: FriendlyByteBuf) {
        buf.writeResourceLocation(item)
        buf.writeUtf(effect)
        buf.writeUtf(target)
        buf.writeFloat(value)
    }

    companion object {
        /**
         * Reads a seasoning written by [write].
         *
         * @param buf Source buffer.
         * @return The decoded seasoning.
         */
        fun read(buf: FriendlyByteBuf): CaracatronSnackSeasoning = CaracatronSnackSeasoning(
            item = buf.readResourceLocation(),
            effect = buf.readUtf(),
            target = buf.readUtf(),
            value = buf.readFloat()
        )
    }
}

/**
 * How to get an item of the Poké Snack tab, shown when hovering it.
 *
 * @property item Item identifier.
 * @property growsEverywhere Whether it's a berry growing naturally in every Overworld biome.
 * @property biomes Overworld biomes where the berry grows naturally, empty when it doesn't or grows everywhere.
 * @property mutations Pairs of berries that, planted side by side, can grow it.
 * @property recipeType Type of a recipe making the item, e.g. `minecraft:crafting`, empty when nothing makes it.
 * @property recipe Ingredients of that recipe.
 *
 * @author hadouz01
 * @since 1.23.0
 */
data class CaracatronItemSource(
    val item: ResourceLocation,
    val growsEverywhere: Boolean,
    val biomes: List<ResourceLocation>,
    val mutations: List<Pair<ResourceLocation, ResourceLocation>>,
    val recipeType: String,
    val recipe: List<CaracatronSnackIngredient>
) {

    /**
     * Writes this source to a buffer.
     *
     * @param buf Target buffer.
     */
    fun write(buf: FriendlyByteBuf) {
        buf.writeResourceLocation(item)
        buf.writeBoolean(growsEverywhere)
        buf.writeCollection(biomes) { out, id -> out.writeResourceLocation(id) }
        buf.writeCollection(mutations) { out, (first, second) ->
            out.writeResourceLocation(first)
            out.writeResourceLocation(second)
        }
        buf.writeUtf(recipeType)
        buf.writeCollection(recipe) { out, ingredient -> ingredient.write(out) }
    }

    companion object {
        /**
         * Reads a source written by [write].
         *
         * @param buf Source buffer.
         * @return The decoded source.
         */
        fun read(buf: FriendlyByteBuf): CaracatronItemSource = CaracatronItemSource(
            item = buf.readResourceLocation(),
            growsEverywhere = buf.readBoolean(),
            biomes = buf.readList { it.readResourceLocation() },
            mutations = buf.readList { it.readResourceLocation() to it.readResourceLocation() },
            recipeType = buf.readUtf(),
            recipe = buf.readList(CaracatronSnackIngredient::read)
        )
    }
}

/**
 * Server → client packet answering a [CaracatronSpawnRequestPayload] with the species' spawn pool and how to attract it
 * with a Poké Snack.
 *
 * @property species Species the spawns belong to.
 * @property firstTier First tier the species spawns in, or `0` when no tier lets it spawn.
 * @property tierMultiplier Spawn rate multiplier at the tier shown on the screen.
 * @property tierLevels Levels natural spawns roll at that tier, `null` while tier spawn levels are off.
 * @property entries Every natural spawn of the species.
 * @property snackRecipe Ingredients of the Poké Snack recipe, empty when no recipe was found.
 * @property snackSeasonings Seasonings whose bait effects favor the species.
 * @property itemSources How to get every item of the Poké Snack tab.
 *
 * @author hadouz01
 * @since 1.12.0
 */
data class CaracatronSpawnInfoPayload(
    val species: ResourceLocation,
    val firstTier: Int,
    val tierMultiplier: Float,
    val tierLevels: IntRange?,
    val entries: List<CaracatronSpawnEntry>,
    val snackRecipe: List<CaracatronSnackIngredient>,
    val snackSeasonings: List<CaracatronSnackSeasoning>,
    val itemSources: List<CaracatronItemSource>
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<CaracatronSpawnInfoPayload> = TYPE

    private fun write(buf: RegistryFriendlyByteBuf) {
        buf.writeResourceLocation(species)
        buf.writeVarInt(firstTier)
        buf.writeFloat(tierMultiplier)
        buf.writeNullable(tierLevels) { out, range ->
            out.writeVarInt(range.first)
            out.writeVarInt(range.last)
        }
        buf.writeCollection(entries) { out, entry -> entry.write(out) }
        buf.writeCollection(snackRecipe) { out, ingredient -> ingredient.write(out) }
        buf.writeCollection(snackSeasonings) { out, seasoning -> seasoning.write(out) }
        buf.writeCollection(itemSources) { out, source -> source.write(out) }
    }

    companion object {
        /**
         * Unique network identifier for this packet.
         */
        val ID: ResourceLocation = CaracaCustoms.id("caracatron_spawn_info")

        /**
         * Payload type definition used by the Fabric registry to route incoming bytes.
         */
        val TYPE: CustomPacketPayload.Type<CaracatronSpawnInfoPayload> = CustomPacketPayload.Type(ID)

        /**
         * Codec writing the tier data, every spawn entry and the Poké Snack data.
         */
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, CaracatronSpawnInfoPayload> = StreamCodec.ofMember(
            CaracatronSpawnInfoPayload::write,
            ::read
        )

        private fun read(buf: RegistryFriendlyByteBuf): CaracatronSpawnInfoPayload = CaracatronSpawnInfoPayload(
            species = buf.readResourceLocation(),
            firstTier = buf.readVarInt(),
            tierMultiplier = buf.readFloat(),
            tierLevels = buf.readNullable { it.readVarInt()..it.readVarInt() },
            entries = buf.readList(CaracatronSpawnEntry::read),
            snackRecipe = buf.readList(CaracatronSnackIngredient::read),
            snackSeasonings = buf.readList(CaracatronSnackSeasoning::read),
            itemSources = buf.readList(CaracatronItemSource::read)
        )
    }
}
