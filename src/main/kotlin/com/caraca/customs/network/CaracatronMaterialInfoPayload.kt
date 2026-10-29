package com.caraca.customs.network

import com.caraca.CaracaCustoms
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/**
 * A place where a block generates in the world.
 *
 * @property kind How it generates: [FEATURE] (ores, plants, trees, rocks...), [TERRAIN] (part of the dimension's ground)
 * or [VEIN] (Minecraft's large copper and iron veins).
 * @property dimension Dimension it generates in.
 * @property biomes Biomes and biome groups (`#` prefixed) it generates in, empty when it generates in every biome of the
 * dimension.
 * @property surface Where a feature sits: [SURFACE], [OCEAN_FLOOR], or empty when it isn't placed on a surface.
 * @property minY Lowest altitude, `null` when unknown.
 * @property maxY Highest altitude, `null` when unknown.
 * @property peakMin Bottom of the most common altitude range, `null` when the altitudes are all as common.
 * @property peakMax Top of the most common altitude range.
 * @property biasedLow Whether lower altitudes are more common.
 * @property perChunk How many times it's tried per chunk, e.g. `16` or `1-3`, empty when unknown.
 * @property rarity Chunks per try (`1 in N chunks`), `0` when it's tried in every chunk.
 * @property size Most blocks in a vein, `0` when it isn't an ore vein.
 *
 * @author hadouz01
 * @since 1.27.0
 */
data class CaracatronMaterialSpot(
    val kind: String,
    val dimension: ResourceLocation,
    val biomes: List<String>,
    val surface: String,
    val minY: Int?,
    val maxY: Int?,
    val peakMin: Int?,
    val peakMax: Int?,
    val biasedLow: Boolean,
    val perChunk: String,
    val rarity: Int,
    val size: Int
) {

    /**
     * Writes this spot to a buffer.
     *
     * @param buf Target buffer.
     */
    fun write(buf: FriendlyByteBuf) {
        buf.writeUtf(kind)
        buf.writeResourceLocation(dimension)
        buf.writeCollection(biomes) { out, biome -> out.writeUtf(biome) }
        buf.writeUtf(surface)
        buf.writeNullable(minY) { out, y -> out.writeVarInt(y) }
        buf.writeNullable(maxY) { out, y -> out.writeVarInt(y) }
        buf.writeNullable(peakMin) { out, y -> out.writeVarInt(y) }
        buf.writeNullable(peakMax) { out, y -> out.writeVarInt(y) }
        buf.writeBoolean(biasedLow)
        buf.writeUtf(perChunk)
        buf.writeVarInt(rarity)
        buf.writeVarInt(size)
    }

    companion object {
        /**
         * Values of [kind] and [surface].
         */
        const val FEATURE = "feature"
        const val TERRAIN = "terrain"
        const val VEIN = "vein"
        const val SURFACE = "surface"
        const val OCEAN_FLOOR = "ocean_floor"

        /**
         * Reads a spot written by [write].
         *
         * @param buf Source buffer.
         * @return The decoded spot.
         */
        fun read(buf: FriendlyByteBuf): CaracatronMaterialSpot = CaracatronMaterialSpot(
            kind = buf.readUtf(),
            dimension = buf.readResourceLocation(),
            biomes = buf.readList { it.readUtf() },
            surface = buf.readUtf(),
            minY = buf.readNullable { it.readVarInt() },
            maxY = buf.readNullable { it.readVarInt() },
            peakMin = buf.readNullable { it.readVarInt() },
            peakMax = buf.readNullable { it.readVarInt() },
            biasedLow = buf.readBoolean(),
            perChunk = buf.readUtf(),
            rarity = buf.readVarInt(),
            size = buf.readVarInt()
        )
    }
}

/**
 * A structure a block is part of.
 *
 * @property structure Structure identifier.
 * @property dimension Dimension the structure generates in, `null` when unknown.
 * @property biomes Biomes and biome groups (`#` prefixed) the structure generates in, empty when it generates in every
 * biome of [dimension] or they're unknown.
 *
 * @author hadouz01
 * @since 1.28.0
 */
data class CaracatronMaterialStructure(val structure: ResourceLocation, val dimension: ResourceLocation?, val biomes: List<String>) {

    /**
     * Writes this structure to a buffer.
     *
     * @param buf Target buffer.
     */
    fun write(buf: FriendlyByteBuf) {
        buf.writeResourceLocation(structure)
        buf.writeNullable(dimension) { out, id -> out.writeResourceLocation(id) }
        buf.writeCollection(biomes) { out, biome -> out.writeUtf(biome) }
    }

    companion object {
        /**
         * Reads a structure written by [write].
         *
         * @param buf Source buffer.
         * @return The decoded structure.
         */
        fun read(buf: FriendlyByteBuf): CaracatronMaterialStructure = CaracatronMaterialStructure(
            structure = buf.readResourceLocation(),
            dimension = buf.readNullable { it.readResourceLocation() },
            biomes = buf.readList { it.readUtf() }
        )
    }
}

/**
 * A block that generates in the world and is, or drops, the searched item.
 *
 * @property block Block identifier.
 * @property dropsItem Whether the block drops the item when mined, instead of being the item itself (e.g. Iron Ore for
 * Raw Iron).
 * @property spots Every place the block generates in naturally.
 * @property structures Every structure the block is part of (since 1.28.0).
 *
 * @author hadouz01
 * @since 1.27.0
 */
data class CaracatronMaterialBlock(
    val block: ResourceLocation,
    val dropsItem: Boolean,
    val spots: List<CaracatronMaterialSpot>,
    val structures: List<CaracatronMaterialStructure>
) {

    /**
     * Writes this block to a buffer.
     *
     * @param buf Target buffer.
     */
    fun write(buf: FriendlyByteBuf) {
        buf.writeResourceLocation(block)
        buf.writeBoolean(dropsItem)
        buf.writeCollection(spots) { out, spot -> spot.write(out) }
        buf.writeCollection(structures) { out, structure -> structure.write(out) }
    }

    companion object {
        /**
         * Reads a block written by [write].
         *
         * @param buf Source buffer.
         * @return The decoded block.
         */
        fun read(buf: FriendlyByteBuf): CaracatronMaterialBlock = CaracatronMaterialBlock(
            block = buf.readResourceLocation(),
            dropsItem = buf.readBoolean(),
            spots = buf.readList(CaracatronMaterialSpot::read),
            structures = buf.readList(CaracatronMaterialStructure::read)
        )
    }
}

/**
 * A loot table holding the searched item that's found in the world.
 *
 * @property kind [BRUSH] for suspicious blocks dug up with a brush, or [CHEST] for chests and other containers.
 * @property block Block holding the loot (e.g. `minecraft:suspicious_gravel`), `null` when unknown.
 * @property place Structure or feature generating that block, `null` when unknown.
 * @property table Loot table identifier, named on the panel when there's no [place].
 * @property dimension Dimension of [place], `null` when unknown.
 * @property biomes Biomes and biome groups (`#` prefixed) [place] generates in, empty when it generates in every biome of
 * [dimension] or they're unknown.
 *
 * @author hadouz01
 * @since 1.27.0
 */
data class CaracatronMaterialLoot(
    val kind: String,
    val block: ResourceLocation?,
    val place: ResourceLocation?,
    val table: ResourceLocation,
    val dimension: ResourceLocation?,
    val biomes: List<String>
) {

    /**
     * Writes this loot to a buffer.
     *
     * @param buf Target buffer.
     */
    fun write(buf: FriendlyByteBuf) {
        buf.writeUtf(kind)
        buf.writeNullable(block) { out, id -> out.writeResourceLocation(id) }
        buf.writeNullable(place) { out, id -> out.writeResourceLocation(id) }
        buf.writeResourceLocation(table)
        buf.writeNullable(dimension) { out, id -> out.writeResourceLocation(id) }
        buf.writeCollection(biomes) { out, biome -> out.writeUtf(biome) }
    }

    companion object {
        /**
         * Values of [kind].
         */
        const val BRUSH = "brush"
        const val CHEST = "chest"

        /**
         * Reads a loot written by [write].
         *
         * @param buf Source buffer.
         * @return The decoded loot.
         */
        fun read(buf: FriendlyByteBuf): CaracatronMaterialLoot = CaracatronMaterialLoot(
            kind = buf.readUtf(),
            block = buf.readNullable { it.readResourceLocation() },
            place = buf.readNullable { it.readResourceLocation() },
            table = buf.readResourceLocation(),
            dimension = buf.readNullable { it.readResourceLocation() },
            biomes = buf.readList { it.readUtf() }
        )
    }
}

/**
 * Server → client packet answering a [CaracatronMaterialRequestPayload] with where an item comes from.
 *
 * @property item Item the answer belongs to.
 * @property blocks Generated blocks that are, or drop, the item.
 * @property loot Loot found in the world holding the item: suspicious blocks, structure chests.
 * @property source Recipe making the item, and for berries where they grow and their mutations.
 *
 * @author hadouz01
 * @since 1.27.0
 */
data class CaracatronMaterialInfoPayload(
    val item: ResourceLocation,
    val blocks: List<CaracatronMaterialBlock>,
    val loot: List<CaracatronMaterialLoot>,
    val source: CaracatronItemSource
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<CaracatronMaterialInfoPayload> = TYPE

    private fun write(buf: RegistryFriendlyByteBuf) {
        buf.writeResourceLocation(item)
        buf.writeCollection(blocks) { out, block -> block.write(out) }
        buf.writeCollection(loot) { out, entry -> entry.write(out) }
        source.write(buf)
    }

    companion object {
        /**
         * Unique network identifier for this packet.
         */
        val ID: ResourceLocation = CaracaCustoms.id("caracatron_material_info")

        /**
         * Payload type definition used by the Fabric registry to route incoming bytes.
         */
        val TYPE: CustomPacketPayload.Type<CaracatronMaterialInfoPayload> = CustomPacketPayload.Type(ID)

        /**
         * Codec writing the generated blocks, the loot and the item source.
         */
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, CaracatronMaterialInfoPayload> = StreamCodec.ofMember(
            CaracatronMaterialInfoPayload::write,
            ::read
        )

        private fun read(buf: RegistryFriendlyByteBuf): CaracatronMaterialInfoPayload = CaracatronMaterialInfoPayload(
            item = buf.readResourceLocation(),
            blocks = buf.readList(CaracatronMaterialBlock::read),
            loot = buf.readList(CaracatronMaterialLoot::read),
            source = CaracatronItemSource.read(buf)
        )
    }
}
