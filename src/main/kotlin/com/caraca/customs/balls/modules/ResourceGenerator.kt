package com.caraca.customs.balls.modules

import com.caraca.CaracaCustoms
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

/**
 * Development-only generator for item models and Cobblemon Poké Ball variation files.
 *
 * Runs only inside a development environment, writing into the project's source tree and, when present,
 * the build resources directory. The project root is resolved as the parent of the game directory (`run/`),
 * so production game and server folders are never written to. Only missing files are created: existing,
 * possibly hand-edited assets are left untouched.
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

    private val projectRoot: Path? by lazy {
        val loader = FabricLoader.getInstance()
        if (loader.isDevelopmentEnvironment) loader.gameDir.toAbsolutePath().parent else null
    }

    /**
     * Generates the item model and Poké Ball variation files for a custom ball in development environments.
     *
     * @param props Configuration properties defining Poké Ball names and references.
     */
    fun generate(props: CustomBallProperties) {
        val root = projectRoot ?: return
        val particleBallName = props.nameId.lowercase().replace("_", "")

        writeAssetFiles(root.resolve("src/main/resources/assets"), props.nameId, particleBallName)

        val buildResourceDir = root.resolve("build/resources/main/assets")
        if (Files.exists(buildResourceDir)) {
            writeAssetFiles(buildResourceDir, props.nameId, particleBallName)
        }
    }

    private fun writeAssetFiles(baseAssetsDir: Path, name: String, particleName: String) {
        val ns = CaracaCustoms.MOD_ID
        val itemDir = baseAssetsDir.resolve("$ns/models/item")
        val variationsDir = baseAssetsDir.resolve("cobblemon/bedrock/poke_balls/variations")

        runCatching {
            Files.createDirectories(itemDir)
            Files.createDirectories(variationsDir)

            writeIfAbsent(itemDir.resolve("$name.json"), ITEM_JSON.format(ns, name))
            writeIfAbsent(itemDir.resolve("${name}_model.json"), ITEM_MODEL_JSON.format(ns, name))

            val casualParticle = "cobblemon:$particleName/casual/ballsendsparkle"
            val battleParticle = "cobblemon:$particleName/battle/ballsparks"

            writeIfAbsent(
                variationsDir.resolve("0_${ns}_${name}_base.json"),
                ENTITY_JSON.format(ns, name, ns, name, casualParticle, battleParticle)
            )
        }
    }

    /**
     * Writes a template file only when it does not exist yet, so hand-edited assets are never overwritten.
     */
    private fun writeIfAbsent(path: Path, content: String) {
        if (Files.notExists(path)) {
            Files.writeString(path, content)
        }
    }
}
