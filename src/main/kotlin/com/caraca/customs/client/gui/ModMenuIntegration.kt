package com.caraca.customs.client.gui

import com.caraca.customs.balls.CustomBallRegistry
import com.caraca.customs.berry.BerryGrowthModifier
import com.caraca.customs.config.BerryConfigEntry
import com.caraca.customs.config.DynamicConfig
import com.caraca.customs.config.PokeBallConfigEntry
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * ModMenu API integration bridge generating dynamic Cloth Config screens.
 *
 * Implements [ModMenuApi] to expose configuration screen factories directly to ModMenu.
 *
 * @author hadouz01
 * @since 1.0.0
 */
class ModMenuIntegration : ModMenuApi {

    /**
     * Binds the Cloth Config builder screen factory to ModMenu.
     *
     * @return [ConfigScreenFactory] instance providing GUI screen construction.
     */
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent -> createConfigScreen(parent) }
    }

    companion object {

        /**
         * Builds and returns a screen for managing mod settings via Cloth Config GUI.
         *
         * @param parent Target parent GUI screen to return to upon saving or closing.
         * @return Fully configured [Screen] instance.
         */
        fun createConfigScreen(parent: Screen): Screen {
            val builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.caraca_customs.config"))

            val entryBuilder = builder.entryBuilder()
            val generalCategory = builder.getOrCreateCategory(Component.translatable("category.caraca_customs.general"))
            val berriesCategory = builder.getOrCreateCategory(Component.translatable("category.caraca_customs.berries"))
            val overridesCategory = builder.getOrCreateCategory(Component.translatable("category.caraca_customs.overrides"))
            val debugsCategory = builder
                .getOrCreateCategory(Component.translatable("category.caraca_customs.debugs"))

            appendGeneralSettings(generalCategory, entryBuilder)
            appendBerryOverrideSettings(berriesCategory, entryBuilder)
            appendBallOverrideSettings(overridesCategory, entryBuilder)
            appendGeneralDebugSettings(debugsCategory, entryBuilder)

            builder.setSavingRunnable {
                DynamicConfig.save()
                BerryGrowthModifier.applyGrowthConfigurations()
            }

            return builder.build()
        }

        /**
         * Appends global settings including Hoe Fortune chance, global default Berry configurations,
         * debug options, and nested global default Poké Ball categories to the main General tab.
         *
         * @param category Target configuration category.
         * @param entryBuilder Entry builder utility.
         */
        private fun appendGeneralSettings(category: ConfigCategory, entryBuilder: ConfigEntryBuilder) {
            appendGlobalBerrySettings(category, entryBuilder)
            appendGlobalBallSettings(category, entryBuilder)
        }

        /**
         * Appends the default global berry drawer entry with unique key identifiers to the main category.
         *
         * @param category Target configuration category.
         * @param entryBuilder Entry builder utility.
         */
        private fun appendGlobalBerrySettings(category: ConfigCategory, entryBuilder: ConfigEntryBuilder) {
            val defaultBerryDrawer = entryBuilder.startSubCategory(Component.translatable("option.caraca_customs.default_berry_title"))
            appendBerryFields(defaultBerryDrawer, entryBuilder, DynamicConfig.data.defaultBerryConfig, isDefault = true)
            category.addEntry(defaultBerryDrawer.build())
        }

        /**
         * Appends default category capture rate drawers for regular, ultra, and caraca Poké Balls to the general tab.
         *
         * @param category Target configuration category.
         * @param entryBuilder Entry builder utility.
         */
        private fun appendGlobalBallSettings(category: ConfigCategory, entryBuilder: ConfigEntryBuilder) {
            appendPokeBallCategoryDrawer(category, entryBuilder, "regular_ball_title", "regular_default", DynamicConfig.data.defaultRegularPokeBallConfig, 1.0f, 1.5f)
            appendPokeBallCategoryDrawer(category, entryBuilder, "ultra_ball_title", "ultra_default", DynamicConfig.data.defaultUltraPokeBallConfig, 3.35f, 6.7f)
            appendPokeBallCategoryDrawer(category, entryBuilder, "caraca_ball_title", "caraca_default", DynamicConfig.data.defaultCaracaBallConfig, 6.7f, 13.4f)
        }

        /**
         * Appends individual item berry overrides to the dedicated Berry category.
         *
         * Sorts all berry entries alphabetically by their localized display names to maintain proper GUI sequence.
         *
         * @param category Target configuration category.
         * @param entryBuilder Entry builder utility.
         */
        private fun appendBerryOverrideSettings(category: ConfigCategory, entryBuilder: ConfigEntryBuilder) {
            DynamicConfig.data.berryOverrides.entries
                .map { (berryId, entry) -> Triple(berryId, resolveItemTranslation(berryId, "item"), entry) }
                .sortedBy { (_, component, _) -> component.string }
                .forEach { (_, component, entry) ->
                    val itemDrawer = entryBuilder.startSubCategory(component)
                    appendBerryFields(itemDrawer, entryBuilder, entry, isDefault = false)
                    category.addEntry(itemDrawer.build())
                }
        }

        /**
         * Iterates over all registered modular Poké Balls and displays them inside expandable drawers in the Overrides category.
         *
         * @param category Target configuration category.
         * @param entryBuilder Entry builder utility.
         */
        private fun appendBallOverrideSettings(category: ConfigCategory, entryBuilder: ConfigEntryBuilder) {
            CustomBallRegistry.getRegisteredBalls().forEach { modularBall ->
                val ballId = modularBall.props.nameId
                val configEntry = DynamicConfig.data.pokeBallOverrides.getOrPut(ballId) {
                    PokeBallConfigEntry(
                        defaultCaptureRate = modularBall.props.defaultCatchRate,
                        boostedCaptureRate = modularBall.props.boostedCatchRate
                    )
                }

                val ballComponent = resolveItemTranslation(ballId, "item")
                val itemDrawer = entryBuilder.startSubCategory(ballComponent)
                appendIndividualBallFields(itemDrawer, entryBuilder, configEntry)
                category.addEntry(itemDrawer.build())
            }
        }

        /**
         * Reusable helper to populate berry input fields for both global defaults and individual overrides.
         *
         * @param drawer Target sub-category builder drawer.
         * @param entryBuilder Entry builder utility.
         * @param targetEntry Target [BerryConfigEntry] instance to bind inputs to.
         * @param isDefault Flag indicating whether the target entry represents the global default settings.
         */
        private fun appendBerryFields(
            drawer: SubCategoryBuilder,
            entryBuilder: ConfigEntryBuilder,
            targetEntry: BerryConfigEntry,
            isDefault: Boolean
        ) {
            val keySuffix = if (isDefault) "berry_default" else "berry"

            val minYield = entryBuilder.startIntField(Component.translatable("option.caraca_customs.$keySuffix.min_yield"), targetEntry.minYield)
                .setDefaultValue(2)
                .setTooltip(Component.translatable("tooltip.caraca_customs.$keySuffix.min_yield"))
                .setSaveConsumer { targetEntry.minYield = it }
                .build()

            val maxYield = entryBuilder.startIntField(Component.translatable("option.caraca_customs.$keySuffix.max_yield"), targetEntry.maxYield)
                .setDefaultValue(5)
                .setTooltip(Component.translatable("tooltip.caraca_customs.$keySuffix.max_yield"))
                .setSaveConsumer { targetEntry.maxYield = it }
                .build()

            val minGrowth = entryBuilder.startIntField(Component.translatable("option.caraca_customs.$keySuffix.min_growth_time"), targetEntry.minGrowthTime)
                .setDefaultValue(20)
                .setTooltip(Component.translatable("tooltip.caraca_customs.$keySuffix.min_growth_time"))
                .setSaveConsumer { targetEntry.minGrowthTime = it }
                .build()

            val maxGrowth = entryBuilder.startIntField(Component.translatable("option.caraca_customs.$keySuffix.max_growth_time"), targetEntry.maxGrowthTime)
                .setDefaultValue(60)
                .setTooltip(Component.translatable("tooltip.caraca_customs.$keySuffix.max_growth_time"))
                .setSaveConsumer { targetEntry.maxGrowthTime = it }
                .build()

            drawer.add(minYield)
            drawer.add(maxYield)
            drawer.add(minGrowth)
            drawer.add(maxGrowth)
        }

        /**
         * Appends global settings including Hoe Fortune chance, global default Berry configurations,
         * debug options, and nested global default Poké Ball categories to the main General tab.
         *
         * @param category Target configuration category.
         * @param entryBuilder Entry builder utility.
         */
        private fun appendGeneralDebugSettings(category: ConfigCategory, entryBuilder: ConfigEntryBuilder) {
            appendDebugSettings(category, entryBuilder)
        }

        /**
         * Appends the debug toggle option for berry growth tick logging.
         *
         * Binds the state directly to [DynamicConfig.data] upon confirmation.
         *
         * @param category Target configuration category.
         * @param entryBuilder Entry builder utility.
         */

        private fun appendDebugSettings(category: ConfigCategory, entryBuilder: ConfigEntryBuilder) {
            val debugEntry = entryBuilder.startBooleanToggle(
                Component.translatable("option.caraca_customs.berry.debug_growth_tick"),
                DynamicConfig.data.debugGrowthTick
            )
                .setDefaultValue(false)
                .setTooltip(Component.translatable("tooltip.caraca_customs.berry.debug_growth_tick"))
                .setSaveConsumer { newValue ->
                    DynamicConfig.data.debugGrowthTick = newValue
                }
                .build()

            category.addEntry(debugEntry)
        }

        /**
         * Appends a Poké Ball category fallback drawer containing rate options directly into general settings.
         *
         * @param category Target configuration category.
         * @param entryBuilder Entry builder utility.
         * @param drawerKey Sub-category title translation key.
         * @param rateKeyPrefix Prefix key for default and boosted rate options.
         * @param targetEntry Target [PokeBallConfigEntry] instance.
         * @param defaultRate Fallback base capture rate default.
         * @param boostedRate Fallback boosted capture rate default.
         */
        private fun appendPokeBallCategoryDrawer(
            category: ConfigCategory,
            entryBuilder: ConfigEntryBuilder,
            drawerKey: String,
            rateKeyPrefix: String,
            targetEntry: PokeBallConfigEntry,
            defaultRate: Float,
            boostedRate: Float
        ) {
            val drawer = entryBuilder.startSubCategory(Component.translatable("option.caraca_customs.$drawerKey"))

            val defaultField = entryBuilder.startFloatField(
                Component.translatable("option.caraca_customs.$rateKeyPrefix.default_rate"),
                targetEntry.defaultCaptureRate
            )
                .setDefaultValue(defaultRate)
                .setTooltip(Component.translatable("tooltip.caraca_customs.$rateKeyPrefix.default_rate"))
                .setSaveConsumer { targetEntry.defaultCaptureRate = it }
                .build()

            val boostedField = entryBuilder.startFloatField(
                Component.translatable("option.caraca_customs.$rateKeyPrefix.boosted_rate"),
                targetEntry.boostedCaptureRate
            )
                .setDefaultValue(boostedRate)
                .setTooltip(Component.translatable("tooltip.caraca_customs.$rateKeyPrefix.boosted_rate"))
                .setSaveConsumer { targetEntry.boostedCaptureRate = it }
                .build()

            drawer.add(defaultField)
            drawer.add(boostedField)
            category.addEntry(drawer.build())
        }

        /**
         * Populates an expandable drawer with individual Poké Ball capture rate override fields and tooltips.
         *
         * @param drawer Target sub-category builder drawer.
         * @param entryBuilder Entry builder utility.
         * @param targetEntry Target [PokeBallConfigEntry] instance to bind inputs to.
         */
        private fun appendIndividualBallFields(
            drawer: SubCategoryBuilder,
            entryBuilder: ConfigEntryBuilder,
            targetEntry: PokeBallConfigEntry
        ) {
            val defaultRate = entryBuilder.startFloatField(
                Component.translatable("option.caraca_customs.ball.default_rate"),
                targetEntry.defaultCaptureRate
            )
                .setDefaultValue(1.0f)
                .setTooltip(Component.translatable("tooltip.caraca_customs.ball.default_rate"))
                .setSaveConsumer { targetEntry.defaultCaptureRate = it }
                .build()

            val boostedRate = entryBuilder.startFloatField(
                Component.translatable("option.caraca_customs.ball.boosted_rate"),
                targetEntry.boostedCaptureRate
            )
                .setDefaultValue(1.5f)
                .setTooltip(Component.translatable("tooltip.caraca_customs.ball.boosted_rate"))
                .setSaveConsumer { targetEntry.boostedCaptureRate = it }
                .build()

            drawer.add(defaultRate)
            drawer.add(boostedRate)
        }

        /**
         * Resolves localized component titles for item or block string identifiers.
         *
         * Supports both plain path keys (e.g. "caraca_ball") and namespaced identifiers (e.g. "cobblemon:oran_berry").
         *
         * @param identifier Identifier string.
         * @param prefix Type prefix ("item" or "block").
         * @return Formatted translatable text component.
         */
        private fun resolveItemTranslation(identifier: String, prefix: String): Component {
            return if (identifier.contains(":")) {
                val location = ResourceLocation.parse(identifier)
                Component.translatable("$prefix.${location.namespace}.${location.path}")
            } else {
                Component.translatable("$prefix.caraca_customs.$identifier")
            }
        }
    }
}