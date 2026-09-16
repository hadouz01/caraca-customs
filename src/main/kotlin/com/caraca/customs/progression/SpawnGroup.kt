package com.caraca.customs.progression

import com.caraca.customs.config.SpawnGroupConfigEntry

/**
 * Species groups by potential and evolutionary stage, used by the tier spawn cap.
 *
 * Each group has its own entry in the config (see [SpawnGroupConfigEntry]) and its species list in
 * `data/caraca_customs/spawn_groups/<id>.json`. The defaults below were made for the default 6 tiers.
 *
 * @property id Identifier used in the config file, translations and data files.
 * @property defaultConfig Config entry used when the group is missing from the config file.
 *
 * @author hadouz01
 * @since 1.5.0
 */
enum class SpawnGroup(
    val id: String,
    private val defaultStartTier: Int,
    private val defaultInterval: Int = 0,
    private val defaultEarlyTierRate: Float = SpawnGroupConfigEntry.DEFAULT_EARLY_TIER_SPAWN_RATE
) {
    STAGE_1_LOW("stage_1_low", 1),
    STAGE_2_LOWEST("stage_2_lowest", 2),
    STAGE_2_LOW("stage_2_low", 3),
    STAGE_3_LOWEST("stage_3_lowest", 3),
    STAGE_3_LOW("stage_3_low", 4),

    STAGE_1_MEDIUM("stage_1_medium", 2, 1, 0.5f),
    STAGE_2_MEDIUM("stage_2_medium", 3),
    STAGE_3_MEDIUM("stage_3_medium", 4),

    STAGE_1_HIGH("stage_1_high", 3),
    STAGE_2_HIGH("stage_2_high", 4),
    STAGE_3_HIGH("stage_3_high", 5, 1),

    STARTER_STAGE_1("starter_stage_1", 2),
    STARTER_STAGE_2("starter_stage_2", 3),
    STARTER_STAGE_3("starter_stage_3", 4),

    REGULAR_LEGENDARY("regular_legendary", 5, 1),
    MYTHICAL_LEGENDARY("mythical_legendary", 6, 1);

    val defaultConfig: SpawnGroupConfigEntry
        get() = SpawnGroupConfigEntry(
            startTier = defaultStartTier,
            reducedRateInterval = defaultInterval,
            earlyTierSpawnRate = defaultEarlyTierRate
        )

    companion object {
        private val BY_ID = entries.associateBy { it.id }

        /**
         * Looks up a group by its identifier.
         *
         * @param id Group identifier, e.g. `stage_1_low`.
         * @return The matching group, or `null` if none.
         */
        fun byId(id: String): SpawnGroup? = BY_ID[id.lowercase()]
    }
}
