package com.caraca.customs.network

import com.caraca.CaracaCustoms
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/**
 * Server → client packet opening the Caracatron 3000 screen with the player's tier progress.
 *
 * @property tier Tier shown on the screen: the world's when [isShared], otherwise the player's own.
 * @property tierCount Amount of tiers configured.
 * @property isShared Whether the player progresses through the shared tiers.
 * @property speciesTiers Every implemented species mapped to the tier it belongs to.
 * @property ownCaught Species registered as caught in the player's own Pokédex.
 * @property worldCaught Species in the shared pool, empty when the player doesn't share tiers.
 * @property othersCaught Species caught by any other player, shared tiers or not.
 * @property pastureLoot Pasture Loot's settings, or `null` when the server doesn't have it.
 *
 * @author hadouz01
 * @since 1.12.0
 */
data class CaracatronOpenPayload(
    val tier: Int,
    val tierCount: Int,
    val isShared: Boolean,
    val speciesTiers: Map<ResourceLocation, Int>,
    val ownCaught: Set<ResourceLocation>,
    val worldCaught: Set<ResourceLocation>,
    val othersCaught: Set<ResourceLocation>,
    val pastureLoot: CaracatronPastureLoot?
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<CaracatronOpenPayload> = TYPE

    private fun write(buf: RegistryFriendlyByteBuf) {
        buf.writeVarInt(tier)
        buf.writeVarInt(tierCount)
        buf.writeBoolean(isShared)
        buf.writeMap(speciesTiers, { out, id -> out.writeResourceLocation(id) }, { out, value -> out.writeVarInt(value) })
        buf.writeCollection(ownCaught) { out, id -> out.writeResourceLocation(id) }
        buf.writeCollection(worldCaught) { out, id -> out.writeResourceLocation(id) }
        buf.writeCollection(othersCaught) { out, id -> out.writeResourceLocation(id) }
        buf.writeNullable(pastureLoot) { out, settings ->
            out.writeDouble(settings.chancePerMinute)
            out.writeCollection(settings.blacklist) { inner, id -> inner.writeResourceLocation(id) }
        }
    }

    companion object {
        /**
         * Unique network identifier for this packet.
         */
        val ID: ResourceLocation = CaracaCustoms.id("caracatron_open")

        /**
         * Payload type definition used by the Fabric registry to route incoming bytes.
         */
        val TYPE: CustomPacketPayload.Type<CaracatronOpenPayload> = CustomPacketPayload.Type(ID)

        /**
         * Codec writing the tier, the species tiers, the caught lists and Pasture Loot's settings.
         */
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, CaracatronOpenPayload> = StreamCodec.ofMember(
            CaracatronOpenPayload::write,
            ::read
        )

        private fun read(buf: RegistryFriendlyByteBuf): CaracatronOpenPayload = CaracatronOpenPayload(
            tier = buf.readVarInt(),
            tierCount = buf.readVarInt(),
            isShared = buf.readBoolean(),
            speciesTiers = buf.readMap({ it.readResourceLocation() }, { it.readVarInt() }),
            ownCaught = buf.readCollection(::HashSet) { it.readResourceLocation() },
            worldCaught = buf.readCollection(::HashSet) { it.readResourceLocation() },
            othersCaught = buf.readCollection(::HashSet) { it.readResourceLocation() },
            pastureLoot = buf.readNullable { input ->
                CaracatronPastureLoot(input.readDouble(), input.readCollection(::HashSet) { it.readResourceLocation() })
            }
        )
    }
}

/**
 * Settings of the Pasture Loot mod on the server, for the Caracatron's drops page.
 *
 * @property chancePerMinute Chance (0 to 1) each pastured Pokémon drops an item each minute, or a negative number when
 * unknown.
 * @property blacklist Items pastures never drop.
 *
 * @author hadouz01
 * @since 1.26.0
 */
data class CaracatronPastureLoot(val chancePerMinute: Double, val blacklist: Set<ResourceLocation>)
