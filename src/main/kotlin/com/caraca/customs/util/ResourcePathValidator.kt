package com.caraca.customs.util

import net.minecraft.resources.ResourceLocation
import java.util.Locale

/**
 * Utility helper providing path sanitization and identifier formatting for dynamic asset generation.
 *
 * Ensures asset path references to comply with strict Minecraft [ResourceLocation] specifications.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object ResourcePathValidator {

    /**
     * Sanitizes a raw identifier string into a valid lowercased resource path.
     *
     * Strips non-alphanumeric characters except underscores and hyphens.
     *
     * @param rawInput The unformatted name string.
     * @return A sanitized path string safe for [ResourceLocation] usage.
     */
    fun sanitizePath(rawInput: String): String {
        return rawInput.lowercase(Locale.ROOT)
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_.-]"), "")
    }

    /**
     * Determines whether a given resource relative path points to a valid image asset.
     *
     * Filter utility to prevent non-texture files (e.g., license or documentation files)
     * from being scanned by the client TextureAtlas engine.
     *
     * @param path The relative file path string.
     * @return `true` if the path ends with `.png`; `false` otherwise.
     */
    fun isTextureAsset(path: String): Boolean {
        return path.endsWith(".png", ignoreCase = true)
    }
}