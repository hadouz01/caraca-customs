package com.caraca.customs.network

import com.caraca.CaracaCustoms
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/**
 * Custom payload packet responsible for transmitting serialized configuration data
 * from the server to connecting clients.
 *
 * @property configJson The full serialized JSON string representing the server's ConfigData.
 *
 * @author hadouz01
 * @since 1.0.0
 */
data class ConfigSyncPayload(val configJson: String) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<ConfigSyncPayload> = TYPE

    companion object {
        /**
         * Maximum accepted JSON length, well above the vanilla 32767 string limit.
         */
        private const val MAX_JSON_LENGTH = 262144

        /**
         * Unique network identifier for this specific packet.
         */
        val ID: ResourceLocation = CaracaCustoms.id("config_sync")

        /**
         * Payload type definition used by the Fabric registry to route incoming bytes.
         */
        val TYPE: CustomPacketPayload.Type<ConfigSyncPayload> = CustomPacketPayload.Type(ID)

        /**
         * UTF-8 string codec carrying the configuration JSON.
         */
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_JSON_LENGTH),
            ConfigSyncPayload::configJson,
            ::ConfigSyncPayload
        )
    }
}
