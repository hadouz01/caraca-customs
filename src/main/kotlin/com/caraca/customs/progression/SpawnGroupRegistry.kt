package com.caraca.customs.progression

import com.caraca.CaracaCustoms
import com.google.gson.JsonParser
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import org.slf4j.LoggerFactory

/**
 * Loads which species belong to each [SpawnGroup] from `data/caraca_customs/spawn_groups/<group id>.json`:
 *
 * ```json
 * { "species": ["bulbasaur", "pikachu", "othermod:custom_mon"] }
 * ```
 *
 * Species without a namespace are read as `cobblemon:`. Datapacks can override any group file, and the lists are
 * reloaded with `/reload`. A species listed in more than one group keeps the first group in [SpawnGroup] order, and
 * species in no group are never affected by the spawn cap.
 *
 * @author hadouz01
 * @since 1.5.0
 */
object SpawnGroupRegistry : SimpleSynchronousResourceReloadListener {

    private const val DIRECTORY = "spawn_groups"
    private const val DEFAULT_NAMESPACE = "cobblemon"

    private val LOGGER = LoggerFactory.getLogger(CaracaCustoms.MOD_ID)

    @Volatile
    private var groupsBySpecies: Map<ResourceLocation, SpawnGroup> = emptyMap()

    /**
     * Registers this loader as a server data reload listener.
     */
    fun register() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(this)
    }

    override fun getFabricId(): ResourceLocation = CaracaCustoms.id(DIRECTORY)

    override fun onResourceManagerReload(manager: ResourceManager) {
        val speciesByGroup = linkedMapOf<SpawnGroup, List<ResourceLocation>>()

        manager.listResources(DIRECTORY) { it.namespace == CaracaCustoms.MOD_ID && it.path.endsWith(".json") }
            .forEach { (location, resource) ->
                val groupId = location.path.removePrefix("$DIRECTORY/").removeSuffix(".json")
                val group = SpawnGroup.byId(groupId)
                if (group == null) {
                    LOGGER.warn("Unknown spawn group file {}", location)
                    return@forEach
                }

                runCatching {
                    resource.openAsReader().use { reader ->
                        JsonParser.parseReader(reader).asJsonObject.getAsJsonArray("species")
                            ?.mapNotNull { element -> parseSpecies(element.asString) }
                            .orEmpty()
                    }
                }.onSuccess { species ->
                    speciesByGroup[group] = species
                }.onFailure { throwable ->
                    LOGGER.error("Failed to read spawn group file {}", location, throwable)
                }
            }

        val mapping = linkedMapOf<ResourceLocation, SpawnGroup>()
        SpawnGroup.entries.forEach { group ->
            speciesByGroup[group].orEmpty().forEach { species ->
                val previous = mapping.putIfAbsent(species, group)
                if (previous != null && previous != group) {
                    LOGGER.warn("Species {} is listed in both {} and {}, keeping {}", species, previous.id, group.id, previous.id)
                }
            }
        }

        groupsBySpecies = mapping
        SpawnTierTable.invalidate()
        LOGGER.info("Loaded {} species into {} spawn groups", mapping.size, speciesByGroup.size)
    }

    /**
     * Finds the group of a species.
     *
     * @param species Species identifier.
     * @return The species' group, or `null` when it isn't in any.
     */
    fun groupOf(species: ResourceLocation): SpawnGroup? = groupsBySpecies[species]

    /**
     * Parses a species name from a group file or a spawn detail, defaulting to the `cobblemon` namespace.
     *
     * @param raw Species name, with or without namespace.
     * @return The species identifier, or `null` if the name is invalid.
     */
    fun parseSpecies(raw: String): ResourceLocation? {
        val name = raw.trim().lowercase()
        if (name.isEmpty()) return null
        val full = if (name.contains(':')) name else "$DEFAULT_NAMESPACE:$name"
        return ResourceLocation.tryParse(full)
    }
}
