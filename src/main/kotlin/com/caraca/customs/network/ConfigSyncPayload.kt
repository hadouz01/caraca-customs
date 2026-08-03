package com.caraca.customs.network

import com.caraca.CaracaCustoms
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/**
 * Custom payload packet responsible for transmitting serialized configuration data
 * from the dedicated server to connecting clients.
 *
 * Implements [CustomPacketPayload] to hook into Fabric's modern 1.21.1 networking API.
 *
 * @property configJson The full serialized JSON string representing the server's ConfigData.
 *
 * @author hadouz01
 * @since 1.0.0
 */
data class ConfigSyncPayload(val configJson: String) : CustomPacketPayload {

    /**
     * Resolves the static payload type descriptor for routing.
     */
    override fun type(): CustomPacketPayload.Type<ConfigSyncPayload> = TYPE

    companion object {
        /**
         * Unique network identifier for this specific packet.
         */
        val ID: ResourceLocation = CaracaCustoms.id("config_sync")

        /**
         * Payload type definition used by the Fabric registry to route incoming bytes.
         */
        val TYPE: CustomPacketPayload.Type<ConfigSyncPayload> = CustomPacketPayload.Type(ID)

        /**
         * High-performance stream codec utilizing native UTF-8 string serialization.
         */
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ConfigSyncPayload::configJson,
            ::ConfigSyncPayload
        )
    }
}