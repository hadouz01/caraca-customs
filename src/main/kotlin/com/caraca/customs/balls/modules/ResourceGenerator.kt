package com.caraca.customs.balls.modules

import com.caraca.CaracaCustoms
import com.caraca.customs.balls.modules.CustomBallProperties
import java.nio.file.Files
import java.nio.file.Path

/**
 * High-performance runtime generator for item models and Cobblemon entity variations.
 *
 * Ensures files are written directly into both source tree and build resources directories
 * to prevent RecipeManager and Datapack loading failures during Fabric Loom development runs.
 *
 * @author hadouz01
 * @since 1.0.0
 */
object ResourceGenerator {

    private const val ITEM_JSON = """{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "%s:item/poke_balls/%s"
  }
}"""

    private const val ITEM_MODEL_JSON = """{
  "parent": "cobblemon:item/poke_ball_model",
  "textures": {
    "layer0": "%s:textures/poke_balls/%s.png"
  }
}"""

    private const val ENTITY_JSON = """{
  "pokeball": "%s:%s",
  "order": 0,
  "variations": [
    {
      "aspects": [],
      "layers": [],
      "poser": "cobblemon:poke_ball",
      "model": "cobblemon:poke_ball.geo",
      "texture": "%s:textures/poke_balls/%s.png",
      "particles": {
        "casual": "%s",
        "battle": "%s"
      }
    }
  ]
}"""

    /**
     * Generates required JSON structure files and writes them to target output directories.
     *
     * @param props Configuration properties defining Poké Ball names and references.
     */
    fun generate(props: CustomBallProperties) {
        val ns = CaracaCustoms.MOD_ID
        val name = props.nameId
        val particleBallName = name.lowercase().replace("_", "")

        val devResourceDir = Path.of("src/main/resources/assets")
        val buildResourceDir = Path.of("build/resources/main/assets")

        writeAssetFiles(devResourceDir, ns, name, particleBallName)
        if (Files.exists(buildResourceDir)) {
            writeAssetFiles(buildResourceDir, ns, name, particleBallName)
        }
    }

    /**
     * Resolves subdirectories and writes item models and entity variation JSONs to target root path.
     *
     * @param baseAssetsDir Root assets directory path.
     * @param ns Mod namespace string.
     * @param name Target Poké Ball name identifier.
     * @param particleName Formatted string used for particle path mappings.
     */
    private fun writeAssetFiles(baseAssetsDir: Path, ns: String, name: String, particleName: String) {
        val itemDir = baseAssetsDir.resolve("$ns/models/item")
        val entityDirCobblemon = baseAssetsDir.resolve("cobblemon/bedrock/poke_balls/variations")

        runCatching {
            Files.createDirectories(itemDir)
            Files.createDirectories(entityDirCobblemon)

            Files.writeString(itemDir.resolve("$name.json"), ITEM_JSON.format(ns, name))
            Files.writeString(itemDir.resolve("${name}_model.json"), ITEM_MODEL_JSON.format(ns, name))

            val casualParticle = "cobblemon:$particleName/casual/ballsendsparkle"
            val battleParticle = "cobblemon:$particleName/battle/ballsparks"

            Files.writeString(
                entityDirCobblemon.resolve("0_${name}_base.json"),
                ENTITY_JSON.format(ns, name, ns, name, casualParticle, battleParticle)
            )
        }
    }
}