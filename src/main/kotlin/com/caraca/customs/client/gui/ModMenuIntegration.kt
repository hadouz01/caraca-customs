package com.caraca.customs.client.gui

import com.caraca.customs.balls.CustomBallRegistry
import com.caraca.customs.berry.BerryGrowthModifier
import com.caraca.customs.config.BerryConfigEntry
import com.caraca.customs.config.ConfigData
import com.caraca.customs.config.DynamicConfig
import com.caraca.customs.config.FeatureToggles
import com.caraca.customs.config.PokeBallConfigEntry
import com.caraca.customs.config.SpawnGroupConfigEntry
import com.caraca.customs.progression.LevelCapHandler
import com.caraca.customs.progression.SpawnGroup
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * ModMenu API integration bridge generating dynamic Cloth Config screens.
 *
 * Reset values of global options come from a fresh [ConfigData], keeping a single source of truth for defaults.
 * Individual overrides reset to `0`, which means "use the global value".
 *
 * @author hadouz01
 * @since 1.0.0
 */
class ModMenuIntegration : ModMenuApi {

    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent -> createConfigScreen(parent) }
    }

    companion object {

        private const val OVERRIDE_FALLBACK_INT = 0
        private const val OVERRIDE_FALLBACK_FLOAT = 0.0f

        private val DEFAULTS = ConfigData()

        /**
         * Builds and returns a screen for managing mod settings via Cloth Config GUI.
         *
         * @param parent Target parent GUI screen to return to upon saving or closing.
         * @return Fully configured [Screen] instance.
         */
        fun createConfigScreen(parent: Screen): Screen {
            val togglesBefore = FeatureToggles.snapshot()
            val builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.caraca_customs.config"))

            val entryBuilder = builder.entryBuilder()

            appendGeneralSettings(builder.getOrCreateCategory(Component.translatable("category.caraca_customs.general")), entryBuilder)
            appendProgressionSettings(builder.getOrCreateCategory(Component.translatable("category.caraca_customs.progression")), entryBuilder)
            appendSpawnGroupSettings(builder.getOrCreateCategory(Component.translatable("category.caraca_customs.spawn_groups")), entryBuilder)
            appendBerryOverrideSettings(builder.getOrCreateCategory(Component.translatable("category.caraca_customs.berries")), entryBuilder)
            appendBallOverrideSettings(builder.getOrCreateCategory(Component.translatable("category.caraca_customs.overrides")), entryBuilder)
            appendDebugSettings(builder.getOrCreateCategory(Component.translatable("category.caraca_customs.debugs")), entryBuilder)

            builder.setSavingRunnable {
                DynamicConfig.save()
                BerryGrowthModifier.applyGrowthConfigurations()
                Minecraft.getInstance().singleplayerServer?.let { server ->
                    server.execute {
                        LevelCapHandler.refreshTiers(server)
                        FeatureToggles.applyChanges(server, togglesBefore)
                    }
                }
            }

            return builder.build()
        }

        /**
         * Appends the mechanic toggles, the Hoe Fortune chance, the global default berry drawer and the
         * default Poké Ball category drawers to the General tab.
         */
        private fun appendGeneralSettings(category: ConfigCategory, entryBuilder: ConfigEntryBuilder) {
            val data = DynamicConfig.data

            fun toggle(key: String, value: Boolean, reset: Boolean, save: (Boolean) -> Unit) =
                entryBuilder.startBooleanToggle(Component.translatable("option.caraca_customs.$key"), value)
                    .setDefaultValue(reset)
                    .setTooltip(Component.translatable("tooltip.caraca_customs.$key"))
                    .setSaveConsumer { save(it) }
                    .build()

            category.addEntry(toggle("custom_balls_enabled", data.customBallsEnabled, DEFAULTS.customBallsEnabled) { data.customBallsEnabled = it })
            category.addEntry(toggle("berry_mechanics_enabled", data.berryMechanicsEnabled, DEFAULTS.berryMechanicsEnabled) { data.berryMechanicsEnabled = it })
            category.addEntry(toggle("level_cap_enabled", data.levelCapEnabled, DEFAULTS.levelCapEnabled) { data.levelCapEnabled = it })
            category.addEntry(toggle("spawn_cap_enabled", data.spawnCapEnabled, DEFAULTS.spawnCapEnabled) { data.spawnCapEnabled = it })
            category.addEntry(toggle("follow_speed_enabled", data.dynamicFollowSpeedEnabled, DEFAULTS.dynamicFollowSpeedEnabled) { data.dynamicFollowSpeedEnabled = it })

            category.addEntry(
                entryBuilder.startFloatField(
                    Component.translatable("option.caraca_customs.global_hoe_fortune_chance"),
                    data.globalHoeFortuneChance
                )
                    .setDefaultValue(DEFAULTS.globalHoeFortuneChance)
                    .setMin(0.0f)
                    .setMax(1.0f)
                    .setTooltip(Component.translatable("tooltip.caraca_customs.global_hoe_fortune_chance"))
                    .setSaveConsumer { data.globalHoeFortuneChance = it }
                    .build()
            )

            val defaultBerryDrawer = entryBuilder.startSubCategory(Component.translatable("option.caraca_customs.default_berry_title"))
            appendBerryFields(defaultBerryDrawer, entryBuilder, data.defaultBerryConfig, DEFAULTS.defaultBerryConfig, "berry_default")
            category.addEntry(defaultBerryDrawer.build())

            appendPokeBallCategoryDrawer(category, entryBuilder, "regular_ball_title", "regular_default", data.defaultRegularPokeBallConfig, DEFAULTS.defaultRegularPokeBallConfig)
            appendPokeBallCategoryDrawer(category, entryBuilder, "ultra_ball_title", "ultra_default", data.defaultUltraPokeBallConfig, DEFAULTS.defaultUltraPokeBallConfig)
            appendPokeBallCategoryDrawer(category, entryBuilder, "caraca_ball_title", "caraca_default", data.defaultCaracaBallConfig, DEFAULTS.defaultCaracaBallConfig)
        }

        /**
         * Appends the tier count and the max capture count to the Progression tab.
         */
        private fun appendProgressionSettings(category: ConfigCategory, entryBuilder: ConfigEntryBuilder) {
            val data = DynamicConfig.data

            category.addEntry(
                entryBuilder.startIntSlider(
                    Component.translatable("option.caraca_customs.tier_count"),
                    data.tierCount,
                    DynamicConfig.MIN_TIER_COUNT,
                    DynamicConfig.MAX_TIER_COUNT
                )
                    .setDefaultValue(DEFAULTS.tierCount)
                    .setTooltip(Component.translatable("tooltip.caraca_customs.tier_count"))
                    .setSaveConsumer { data.tierCount = it }
                    .build()
            )

            category.addEntry(
                entryBuilder.startIntField(
                    Component.translatable("option.caraca_customs.max_tier_capture_count"),
                    data.maxTierCaptureCount
                )
                    .setDefaultValue(DEFAULTS.maxTierCaptureCount)
                    .setMin(DynamicConfig.MIN_TIER_CAPTURE_COUNT)
                    .setMax(DynamicConfig.MAX_TIER_CAPTURE_COUNT)
                    .setTooltip(Component.translatable("tooltip.caraca_customs.max_tier_capture_count"))
                    .setSaveConsumer { data.maxTierCaptureCount = it }
                    .build()
            )
        }

        /**
         * Appends the species surplus and a drawer with the spawn rules of every [SpawnGroup] to the Spawn Groups tab.
         */
        private fun appendSpawnGroupSettings(category: ConfigCategory, entryBuilder: ConfigEntryBuilder) {
            val data = DynamicConfig.data

            category.addEntry(
                entryBuilder.startFloatField(
                    Component.translatable("option.caraca_customs.tier_species_surplus"),
                    data.tierSpeciesSurplus
                )
                    .setDefaultValue(DEFAULTS.tierSpeciesSurplus)
                    .setMin(0.0f)
                    .setMax(DynamicConfig.MAX_TIER_SPECIES_SURPLUS)
                    .setTooltip(Component.translatable("tooltip.caraca_customs.tier_species_surplus"))
                    .setSaveConsumer { data.tierSpeciesSurplus = it }
                    .build()
            )

            SpawnGroup.entries.forEach { group ->
                val entry = data.spawnGroups.getOrPut(group.id) { group.defaultConfig }
                val drawer = entryBuilder.startSubCategory(Component.translatable("spawn_group.caraca_customs.${group.id}"))
                appendSpawnGroupFields(drawer, entryBuilder, entry, group.defaultConfig)
                category.addEntry(drawer.build())
            }
        }

        /**
         * Populates the six spawn rule fields of a spawn group.
         *
         * @param drawer Target sub-category builder drawer.
         * @param entryBuilder Entry builder utility.
         * @param targetEntry Entry the fields are bound to.
         * @param resetEntry Entry providing the reset values.
         */
        private fun appendSpawnGroupFields(
            drawer: SubCategoryBuilder,
            entryBuilder: ConfigEntryBuilder,
            targetEntry: SpawnGroupConfigEntry,
            resetEntry: SpawnGroupConfigEntry
        ) {
            fun label(key: String) = Component.translatable("option.caraca_customs.spawn_group.$key")
            fun tooltip(key: String) = Component.translatable("tooltip.caraca_customs.spawn_group.$key")

            drawer.add(
                entryBuilder.startIntField(label("start_tier"), targetEntry.startTier)
                    .setDefaultValue(resetEntry.startTier)
                    .setMin(1)
                    .setMax(DynamicConfig.MAX_TIER_COUNT)
                    .setTooltip(tooltip("start_tier"))
                    .setSaveConsumer { targetEntry.startTier = it }
                    .build()
            )
            drawer.add(
                entryBuilder.startFloatField(label("max_spawn_rate_multiplier"), targetEntry.maxSpawnRateMultiplier)
                    .setDefaultValue(resetEntry.maxSpawnRateMultiplier)
                    .setMin(DynamicConfig.MIN_SPAWN_RATE_MULTIPLIER)
                    .setMax(DynamicConfig.MAX_SPAWN_RATE_MULTIPLIER)
                    .setTooltip(tooltip("max_spawn_rate_multiplier"))
                    .setSaveConsumer { targetEntry.maxSpawnRateMultiplier = it }
                    .build()
            )
            drawer.add(
                entryBuilder.startIntSlider(label("reduced_rate_interval"), targetEntry.reducedRateInterval, 0, DynamicConfig.MAX_REDUCED_RATE_INTERVAL)
                    .setDefaultValue(resetEntry.reducedRateInterval)
                    .setTooltip(tooltip("reduced_rate_interval"))
                    .setSaveConsumer { targetEntry.reducedRateInterval = it }
                    .build()
            )
            drawer.add(
                entryBuilder.startFloatField(label("early_tier_spawn_rate"), targetEntry.earlyTierSpawnRate)
                    .setDefaultValue(resetEntry.earlyTierSpawnRate)
                    .setMin(DynamicConfig.MIN_SPAWN_RATE_MULTIPLIER)
                    .setMax(DynamicConfig.MAX_EARLY_TIER_SPAWN_RATE)
                    .setTooltip(tooltip("early_tier_spawn_rate"))
                    .setSaveConsumer { targetEntry.earlyTierSpawnRate = it }
                    .build()
            )
            drawer.add(
                entryBuilder.startIntField(label("reduce_spawn_rate_tier"), targetEntry.reduceSpawnRateTier)
                    .setDefaultValue(resetEntry.reduceSpawnRateTier)
                    .setMin(0)
                    .setMax(DynamicConfig.MAX_TIER_COUNT)
                    .setTooltip(tooltip("reduce_spawn_rate_tier"))
                    .setSaveConsumer { targetEntry.reduceSpawnRateTier = it }
                    .build()
            )
            drawer.add(
                entryBuilder.startFloatField(label("max_reduced_spawn_rate"), targetEntry.maxReducedSpawnRate)
                    .setDefaultValue(resetEntry.maxReducedSpawnRate)
                    .setMin(0.0f)
                    .setMax(DynamicConfig.MAX_REDUCED_SPAWN_RATE)
                    .setTooltip(tooltip("max_reduced_spawn_rate"))
                    .setSaveConsumer { targetEntry.maxReducedSpawnRate = it }
                    .build()
            )
        }

        /**
         * Appends individual berry overrides, sorted alphabetically by their localized names.
         */
        private fun appendBerryOverrideSettings(category: ConfigCategory, entryBuilder: ConfigEntryBuilder) {
            val fallback = BerryConfigEntry(OVERRIDE_FALLBACK_INT, OVERRIDE_FALLBACK_INT, OVERRIDE_FALLBACK_INT, OVERRIDE_FALLBACK_INT)

            DynamicConfig.data.berryOverrides.entries
                .map { (berryId, entry) -> resolveItemTranslation(berryId) to entry }
                .sortedBy { (component, _) -> component.string }
                .forEach { (component, entry) ->
                    val itemDrawer = entryBuilder.startSubCategory(component)
                    appendBerryFields(itemDrawer, entryBuilder, entry, fallback, "berry")
                    category.addEntry(itemDrawer.build())
                }
        }

        /**
         * Appends an override drawer for every registered custom Poké Ball.
         */
        private fun appendBallOverrideSettings(category: ConfigCategory, entryBuilder: ConfigEntryBuilder) {
            CustomBallRegistry.getRegisteredBalls().forEach { modularBall ->
                val ballId = modularBall.props.nameId
                val configEntry = DynamicConfig.data.pokeBallOverrides.getOrPut(ballId) {
                    PokeBallConfigEntry(OVERRIDE_FALLBACK_FLOAT, OVERRIDE_FALLBACK_FLOAT)
                }

                val itemDrawer = entryBuilder.startSubCategory(resolveItemTranslation(ballId))
                appendRateFields(itemDrawer, entryBuilder, configEntry, PokeBallConfigEntry(OVERRIDE_FALLBACK_FLOAT, OVERRIDE_FALLBACK_FLOAT), "ball")
                category.addEntry(itemDrawer.build())
            }
        }

        /**
         * Appends the berry growth tick debugger toggle.
         */
        private fun appendDebugSettings(category: ConfigCategory, entryBuilder: ConfigEntryBuilder) {
            category.addEntry(
                entryBuilder.startBooleanToggle(
                    Component.translatable("option.caraca_customs.berry.debug_growth_tick"),
                    DynamicConfig.data.debugGrowthTick
                )
                    .setDefaultValue(DEFAULTS.debugGrowthTick)
                    .setTooltip(Component.translatable("tooltip.caraca_customs.berry.debug_growth_tick"))
                    .setSaveConsumer { DynamicConfig.data.debugGrowthTick = it }
                    .build()
            )
        }

        /**
         * Populates berry yield and growth time fields for either the global default or an individual override.
         *
         * @param drawer Target sub-category builder drawer.
         * @param entryBuilder Entry builder utility.
         * @param targetEntry Entry the fields are bound to.
         * @param resetEntry Entry providing the reset values.
         * @param keySuffix Translation key segment (`berry_default` or `berry`).
         */
        private fun appendBerryFields(
            drawer: SubCategoryBuilder,
            entryBuilder: ConfigEntryBuilder,
            targetEntry: BerryConfigEntry,
            resetEntry: BerryConfigEntry,
            keySuffix: String
        ) {
            fun intField(key: String, value: Int, reset: Int, save: (Int) -> Unit) =
                entryBuilder.startIntField(Component.translatable("option.caraca_customs.$keySuffix.$key"), value)
                    .setDefaultValue(reset)
                    .setTooltip(Component.translatable("tooltip.caraca_customs.$keySuffix.$key"))
                    .setSaveConsumer { save(it) }
                    .build()

            drawer.add(intField("min_yield", targetEntry.minYield, resetEntry.minYield) { targetEntry.minYield = it })
            drawer.add(intField("max_yield", targetEntry.maxYield, resetEntry.maxYield) { targetEntry.maxYield = it })
            drawer.add(intField("min_growth_time", targetEntry.minGrowthTime, resetEntry.minGrowthTime) { targetEntry.minGrowthTime = it })
            drawer.add(intField("max_growth_time", targetEntry.maxGrowthTime, resetEntry.maxGrowthTime) { targetEntry.maxGrowthTime = it })
        }

        /**
         * Appends a default Poké Ball category drawer to the General tab.
         */
        private fun appendPokeBallCategoryDrawer(
            category: ConfigCategory,
            entryBuilder: ConfigEntryBuilder,
            drawerKey: String,
            rateKeyPrefix: String,
            targetEntry: PokeBallConfigEntry,
            resetEntry: PokeBallConfigEntry
        ) {
            val drawer = entryBuilder.startSubCategory(Component.translatable("option.caraca_customs.$drawerKey"))
            appendRateFields(drawer, entryBuilder, targetEntry, resetEntry, rateKeyPrefix)
            category.addEntry(drawer.build())
        }

        /**
         * Populates default and boosted catch rate fields.
         *
         * @param drawer Target sub-category builder drawer.
         * @param entryBuilder Entry builder utility.
         * @param targetEntry Entry the fields are bound to.
         * @param resetEntry Entry providing the reset values.
         * @param keyPrefix Translation key segment (e.g. `ball`, `ultra_default`).
         */
        private fun appendRateFields(
            drawer: SubCategoryBuilder,
            entryBuilder: ConfigEntryBuilder,
            targetEntry: PokeBallConfigEntry,
            resetEntry: PokeBallConfigEntry,
            keyPrefix: String
        ) {
            fun floatField(key: String, value: Float, reset: Float, save: (Float) -> Unit) =
                entryBuilder.startFloatField(Component.translatable("option.caraca_customs.$keyPrefix.$key"), value)
                    .setDefaultValue(reset)
                    .setTooltip(Component.translatable("tooltip.caraca_customs.$keyPrefix.$key"))
                    .setSaveConsumer { save(it) }
                    .build()

            drawer.add(floatField("default_rate", targetEntry.defaultCaptureRate, resetEntry.defaultCaptureRate) { targetEntry.defaultCaptureRate = it })
            drawer.add(floatField("boosted_rate", targetEntry.boostedCaptureRate, resetEntry.boostedCaptureRate) { targetEntry.boostedCaptureRate = it })
        }

        /**
         * Resolves a translatable item name for a namespaced id (e.g. `cobblemon:oran_berry`)
         * or a bare `caraca_customs` path (e.g. `caraca_ball`).
         */
        private fun resolveItemTranslation(identifier: String): Component {
            val location = if (identifier.contains(":")) {
                ResourceLocation.parse(identifier)
            } else {
                ResourceLocation.fromNamespaceAndPath("caraca_customs", identifier)
            }
            return Component.translatable("item.${location.namespace}.${location.path}")
        }
    }
}
