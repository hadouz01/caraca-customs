package com.caraca.customs.network

import com.caraca.CaracaCustoms
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/**
 * Client → server packet asking where the item selected on the Caracatron's materials page comes from.
 *
 * @property item Selected item identifier.
 *
 * @author hadouz01
 * @since 1.27.0
 */
data class CaracatronMaterialRequestPayload(val item: ResourceLocation) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<CaracatronMaterialRequestPayload> = TYPE

    companion object {
        /**
         * Unique network identifier for this packet.
         */
        val ID: ResourceLocation = CaracaCustoms.id("caracatron_material_request")

        /**
         * Payload type definition used by the Fabric registry to route incoming bytes.
         */
        val TYPE: CustomPacketPayload.Type<CaracatronMaterialRequestPayload> = CustomPacketPayload.Type(ID)

        /**
         * Codec carrying the item identifier.
         */
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, CaracatronMaterialRequestPayload> = StreamCodec.ofMember(
            { payload, buf -> buf.writeResourceLocation(payload.item) },
            { buf -> CaracatronMaterialRequestPayload(buf.readResourceLocation()) }
        )
    }
}
