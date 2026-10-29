package com.caraca.customs.network

import com.caraca.CaracaCustoms
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/**
 * Client → server packet asking for the spawn pool of the species selected on the Caracatron 3000.
 *
 * @property species Selected species identifier.
 *
 * @author hadouz01
 * @since 1.12.0
 */
data class CaracatronSpawnRequestPayload(val species: ResourceLocation) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<CaracatronSpawnRequestPayload> = TYPE

    companion object {
        /**
         * Unique network identifier for this packet.
         */
        val ID: ResourceLocation = CaracaCustoms.id("caracatron_spawn_request")

        /**
         * Payload type definition used by the Fabric registry to route incoming bytes.
         */
        val TYPE: CustomPacketPayload.Type<CaracatronSpawnRequestPayload> = CustomPacketPayload.Type(ID)

        /**
         * Codec carrying the species identifier.
         */
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, CaracatronSpawnRequestPayload> = StreamCodec.ofMember(
            { payload, buf -> buf.writeResourceLocation(payload.species) },
            { buf -> CaracatronSpawnRequestPayload(buf.readResourceLocation()) }
        )
    }
}
