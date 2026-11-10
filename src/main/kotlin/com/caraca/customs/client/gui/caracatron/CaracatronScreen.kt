package com.caraca.customs.client.gui.caracatron

import com.caraca.CaracaCustoms
import com.caraca.customs.network.CaracatronMaterialInfoPayload
import com.caraca.customs.network.CaracatronMaterialRequestPayload
import com.caraca.customs.network.CaracatronOpenPayload
import com.caraca.customs.network.CaracatronSpawnInfoPayload
import com.caraca.customs.network.CaracatronSpawnRequestPayload
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.gui.TypeIcon
import com.cobblemon.mod.common.client.gui.drawProfilePokemon
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.pokemon.RenderablePokemon
import com.cobblemon.mod.common.pokemon.Species
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import java.text.Normalizer
import kotlin.math.ceil

/**
 * Screen of the Caracatron 3000, laid out like Cobblemon's Pokédex, following `exemplo_caracatron.png`.
 *
 * The header shows the tier and the available and caught counts, the left panel lists species (search, filter and scroll
 * bar) and the right panel shows the selected species' spawns by rarity, or how to attract it with a snack.
 *
 * The same screen also runs the Drops page (search by item or species, drops on the right) and the Materials page
 * (search any item, its sources on the right in four tabs), through [page] and [showPage]. The other pages go through
 * [CaracatronPages]. Textures and text colors follow the picked screen color ([CaracatronColorSelect],
 * [CaracatronTextColors]).
 *
 * @param data Player progress sent by the server.
 *
 * @author hadouz01
 * @since 1.12.0
 */
class CaracatronScreen(private val data: CaracatronOpenPayload) : Screen(Component.translatable("$KEY.title")) {

    /**
     * A species listed on the Caracatron.
     *
     * @property species Cobblemon species.
     * @property tier Tier the species belongs to.
     * @property searchName Translated name, lowercase and without accents.
     * @property color RGB color of the species' tier.
     * @property index Position of the species in the full list, by dex number, used by the [CaracatronDropIndex].
     */
    private data class Entry(val species: Species, val tier: Int, val searchName: String, val color: Int, val index: Int)

    companion object {
        private const val KEY = "text.caraca_customs.caracatron"

        private const val BASE_WIDTH = 345
        private const val BASE_HEIGHT = 207
        private const val SCALE = 0.5f

        private const val BAR_X = 24
        private const val BAR_WIDTH = 139
        private const val BAR_HEIGHT = 11
        private const val SEARCH_BAR_Y = 28
        private const val CATEGORY_BAR_Y = 180

        private const val LIST_X = 24
        private const val LIST_TOP = 39
        private const val LIST_BOTTOM = 180
        private const val LIST_RIGHT = 158
        private const val COLUMNS = 5
        private const val SLOT_SIZE = 25
        private const val SLOT_STEP = 27
        private const val SLOT_TOP_PADDING = 2

        private const val INFO_X = 182
        private const val INFO_Y = 28
        private const val INFO_WIDTH = 139
        private const val INFO_HEIGHT = 163
        private const val INFO_TEXT_X = 186
        private const val INFO_TEXT_TOP = 55
        private const val INFO_TEXT_WIDTH = 126
        private const val INFO_VIEW_TOP = 53
        private const val INFO_VIEW_BOTTOM = 176
        private const val INFO_LINE_HEIGHT = 5

        private const val ICON_SIZE = 14
        private const val ARROW_WIDTH = 12
        private const val ARROW_HEIGHT = 6
        private const val INFO_TAB_Y = 182
        private const val INFO_TAB_ARROW_Y = 177
        private val INFO_TAB_X = intArrayOf(188, 218, 248, 278, 308)
        private val INFO_TAB_ICONS = listOf("common_icon", "uncommon_icon", "rare_icon", "ultra_rare_icon", "snack_icon")
        private val MATERIAL_TAB_ICONS = listOf("materials_world_icon", "materials_structure_icon", "materials_recipe_icon", "materials_brush_icon")

        /**
         * Left edges of the materials tab icons, spread "space around" over the info panel: the same gap between the icons
         * and half of it at both ends.
         */
        private val MATERIAL_TAB_X = MATERIAL_TAB_ICONS.indices.map { index ->
            val gap = (INFO_WIDTH - MATERIAL_TAB_ICONS.size * ICON_SIZE * SCALE) / MATERIAL_TAB_ICONS.size
            INFO_X + gap / 2 + index * (ICON_SIZE * SCALE + gap)
        }
        private const val SNACK_TAB = "snack"
        private val INFO_TABS = CaracatronSpawnText.BUCKETS + SNACK_TAB

        private const val CATEGORY_ARROW_X = 153
        private const val CATEGORY_UP_Y = 181
        private const val CATEGORY_DOWN_Y = 186
        private const val GLOBE_X = 152
        private const val GLOBE_Y = 30

        private const val NAME_MAX_WIDTH = 70
        private const val MATERIAL_NAME_X = 212
        private const val MATERIAL_NAME_MAX_WIDTH = 104
        private const val MATERIAL_ICON_X = 186
        private const val MATERIAL_ICON_Y = 32
        private const val ITEM_SIZE = 16
        private const val SLOT_ITEM_OFFSET = 4
        private const val INFO_ICON_OFFSET = 1
        private const val GRID_CELL_ON_LIGHT = 0x28000000
        private const val ARROW_ON_LIGHT = 0x60000000
        private const val ARROW_ON_DARK = 0x60FFFFFF
        private const val STAGE_ON_LIGHT = 0x40000000
        private const val STAGE_ON_DARK = 0x40FFFFFF
        private const val FLAME_SIZE = 14
        private val FLAME: ResourceLocation = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress")

        /**
         * Stands for the flame slot of a furnace recipe when looking up what's under the mouse.
         */
        private val FUEL_SLOT: ResourceLocation = CaracaCustoms.id("fuel")

        /**
         * Plain text color of the materials page on dark panels (Noir), a bit lighter than the other pages.
         */
        private const val MATERIALS_DARK_PANEL_TEXT = 0x909090
        private const val INFO_UNITS = (INFO_TEXT_WIDTH / SCALE).toInt()

        /**
         * Dex number color of the slots without the dark filter on light screens (Blanc, Canary, Lime).
         */
        private const val SLOT_NUMBER_DARK = 0x000000
        private const val GRID_CELL_ON_DARK = 0x30FFFFFF

        /**
         * Recipe grid squares of the materials page on dark panels (Noir), 15% fainter than the snack tab's.
         */
        private const val MATERIALS_GRID_CELL_ON_DARK = 0x29FFFFFF

        /**
         * How far the dex number and name move right for species with two types, clearing the longer type bar. The name
         * gets that much narrower, so it still ends before the caught icon, which never moves.
         */
        private const val DUAL_TYPE_SHIFT = 9
        private const val TOOLTIP_LINE_HEIGHT = 10
        private const val TOOLTIP_MARGIN = 16
        private const val INFO_CAUGHT_X = 309.5f
        private const val INFO_CAUGHT_Y = 41.5f
        private const val SLOT_CAUGHT_X = 18.0f
        private const val SLOT_CAUGHT_Y = 1.5f
        private const val SMALL_ICON_SIZE = 11
        private const val TIER_LABEL_RIGHT = 318
        private const val TIER_LABEL_Y = 29
        private const val TIER_LABEL_OUTLINE = 0xFFFFFF
        private const val DIMMED_ALPHA = 0.35f

        private val SLOT_ROTATION = Vector3f(13.0f, 35.0f, 0.0f)

        private const val UNCAUGHT_FILTER = 0xBF0D363A.toInt()
        private const val OUTLINE_ALPHA = 0xFF000000.toInt()
        private const val FILTER_Z = -1000.0f

        /**
         * Opens the screen with the progress sent by the server, on the page, searches and selections the Caracatron was
         * last closed with ([CaracatronState]).
         *
         * @param data Player progress.
         */
        fun open(data: CaracatronOpenPayload) {
            CaracatronTextColors.clear()
            CaracatronPokedexSkin.clear()
            CaracatronFuels.clear()
            val screen = CaracatronScreen(data)
            screen.restore()
            Minecraft.getInstance().setScreen(screen)
            val last = CaracatronState.lastPage
            if (last != CaracatronPage.CARACA) CaracatronPages.open(last, screen, click = false)
        }

        /**
         * Writes a tier as a Roman numeral, so 4 becomes `IV`.
         *
         * @param number Number from 1 up.
         * @return The Roman numeral, or the plain number past 3999.
         */
        fun roman(number: Int): String {
            if (number !in 1..3999) return number.toString()
            val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
            val symbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
            var rest = number
            return buildString {
                values.forEachIndexed { index, value ->
                    while (rest >= value) {
                        append(symbols[index])
                        rest -= value
                    }
                }
            }
        }

        private val ACCENTS = "\\p{M}+".toRegex()

        /**
         * Lowercases a text and strips its accents, so "Flabébé" matches "flabebe". Plain ASCII texts, most item ids and
         * names, skip the accent pass.
         */
        private fun normalize(text: String): String {
            if (text.all { it.code < 128 }) return text.lowercase()
            return ACCENTS.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase()
        }
    }

    private val colors = CaracatronColors.available()

    /**
     * Folder of the screen color in use.
     */
    var folder: String = CaracatronColors.current(colors)
        private set

    /**
     * Species selected on the list, if any.
     */
    val selectedSpecies: ResourceLocation? get() = selected?.species?.resourceIdentifier

    /**
     * Screen color select, shared with the other pages of the Caracatron so they show it in the same place.
     */
    val colorSelect = CaracatronColorSelect(colors, folder) { picked ->
        folder = picked
        CaracatronColors.select(picked)
        if (showsDrops || showsMaterials) rebuildInfo()
    }

    private val screenTexture get() = part("caracatron_screen")
    private val searchBar get() = part("caracatron_screen_bar_search")
    private val categoryBar get() = part("caracatron_screen_bar_category")
    private val infoOverlay get() = part("caracatron_screen_info")
    private val typeBar get() = part("type_bar")
    private val typeBarLong get() = part("type_bar_long")
    private val slotTexture = CaracatronColors.icon("pokemon_slot")
    private val slotSelect = CaracatronColors.icon("slot_select")
    private val caughtSeenIcon = CaracatronColors.icon("caught_seen_icon")
    private val caughtSlotIcon = CaracatronColors.icon("caught_seen_icon_slot")
    private val caughtSlotSmallIcon = CaracatronColors.icon("caught_seen_icon_slot_small")
    private val medalIcon = CaracatronColors.icon("medal_icon")
    private val searchIcon = CaracatronColors.icon("search_icon")
    private val globeIcon = CaracatronColors.icon("globe_icon")
    private val categoryIcon = CaracatronColors.icon("category_icon")
    private val selectArrow = CaracatronColors.icon("select_arrow")
    private val scrollbarSlide = CaracatronColors.icon("scrollbar_slide")
    private val scrollbarTrack get() = part("scrollbar_track")
    private val tabIcons = INFO_TAB_ICONS.map(CaracatronColors::icon)
    private val materialTabIcons = MATERIAL_TAB_ICONS.map(CaracatronColors::icon)

    private val hasTierSlots: Boolean by lazy {
        val resources = Minecraft.getInstance().resourceManager
        (1..data.tierCount).all { resources.getResource(CaracatronColors.icon("pokemon_slot_t$it")).isPresent }
    }

    /**
     * Page shown by this screen: the Caracatron itself, its drops page or its materials page, which share the whole
     * layout.
     */
    var page: CaracatronPage = CaracatronPage.CARACA
        private set

    private val navigations = listOf(CaracatronPage.CARACA, CaracatronPage.DROPS, CaracatronPage.MATERIALS).associateWith { shown ->
        CaracatronNavigation(shown) { CaracatronPages.open(it, this) }
    }

    /**
     * Drawer of the filter bar, on every page but Materials.
     */
    private val filterSelect = CaracatronFilterSelect { picked ->
        filter = picked
        updateFilter()
    }

    private val navigation: CaracatronNavigation get() = navigations.getValue(page)

    private val showsDrops: Boolean get() = page == CaracatronPage.DROPS

    private val showsMaterials: Boolean get() = page == CaracatronPage.MATERIALS

    private val showsSpawns: Boolean get() = page == CaracatronPage.CARACA

    private val showsSnack: Boolean get() = showsSpawns && selectedTab == SNACK_TAB

    private val entries: List<Entry> = data.speciesTiers.mapNotNull { (id, tier) ->
        PokemonSpecies.getByIdentifier(id)?.let { it to tier }
    }.sortedBy { (species, _) -> species.nationalPokedexNumber }.mapIndexed { index, (species, tier) ->
        Entry(species, tier, normalize(species.translatedName.string), CaracatronTierColors.colorOf(tier, data.tierCount), index)
    }

    /**
     * Item search of the drops page, built the first time it's needed.
     */
    private val dropIndex: CaracatronDropIndex by lazy { CaracatronDropIndex(entries.map { it.species }, ::normalize) }

    /**
     * Item search of the materials page, shared between Caracatron screens ([CaracatronMaterialIndex.shared]).
     */
    private val materialIndex: CaracatronMaterialIndex by lazy { CaracatronMaterialIndex.shared(::normalize) }

    private val searches = mutableMapOf<CaracatronPage, String>()
    private var matchedItems: Set<ResourceLocation> = emptySet()
    private var materials: List<Item> = emptyList()
    private var selectedMaterial: Item? = null
    private var selectedMaterialTab = CaracatronMaterialText.WORLD
    private var hoveredDrop: ResourceLocation? = null
    private var hoveredLink: String? = null
    private var pendingScroll = false
    private val materialStages = mutableMapOf<ResourceLocation, CaracatronGrowthStages?>()
    private var materialQueryLength = 0
    private val materialTabContent = mutableMapOf<ResourceLocation, List<Boolean>>()
    private val materialInfo = mutableMapOf<ResourceLocation, CaracatronMaterialInfoPayload>()
    private val itemStacks = mutableMapOf<ResourceLocation, ItemStack>()

    private val tierFilters: List<CaracatronFilter> = (1..data.tierCount).map { CaracatronFilter.Tier(it) }

    /**
     * Filters of the category bar. The drops page gets "By biggest drop" while it searches something.
     */
    private val filters: List<CaracatronFilter>
        get() {
            val byDrop = if (showsDrops && dropQuery.isNotEmpty()) listOf(CaracatronFilter.ByDrop) else emptyList()
            return listOf(CaracatronFilter.AllByTier, CaracatronFilter.AllByNumber) + byDrop + tierFilters
        }

    private var dropQuery = ""
    private var dropSpecies: BooleanArray? = null

    private val availableCount = entries.count { it.tier <= data.tier }

    private val listScroll = CaracatronScrollBar(159, 42, 177, 4, 4, 16)
    private val infoScroll = CaracatronScrollBar(315, 57, 172, 3, 1, 12)

    private val renderables = mutableMapOf<ResourceLocation, RenderablePokemon>()
    private val spawnInfo = mutableMapOf<ResourceLocation, CaracatronSpawnInfoPayload>()
    private val biomeTooltips = mutableMapOf<String, List<Component>>()
    private val itemTooltips = mutableMapOf<ResourceLocation, List<Component>>()

    private var showingWorld = data.isShared
    private var filter: CaracatronFilter = CaracatronFilter.AllByTier
    private var filtered: List<Entry> = emptyList()
    private var selected: Entry? = null
    private var selectedTab = INFO_TABS.first()
    private var infoLines: List<CaracatronInfoLine> = emptyList()

    private lateinit var searchBox: CaracatronSearchBox

    private val left: Int get() = (width - BASE_WIDTH) / 2
    private val top: Int get() = (height - BASE_HEIGHT) / 2

    private val caughtSpecies: Set<ResourceLocation> get() = if (showingWorld) data.worldCaught else data.ownCaught

    private val textColors: CaracatronTextColors get() = CaracatronTextColors.of(folder)

    private fun part(name: String): ResourceLocation = CaracatronColors.part(folder, name)

    override fun init() {
        val firstInit = !::searchBox.isInitialized
        val previousSearch = if (firstInit) searches[page].orEmpty() else searchBox.value
        searchBox = CaracatronSearchBox(left + 37, top + 29, 112, 10, searchHint()).apply {
            value = previousSearch
            setResponder { updateFilter() }
        }
        addRenderableWidget(searchBox)
        updateFilter()
        if (firstInit && showsSpawns) selected?.let(::requestSpawnInfo)
    }

    /**
     * Brings back the searches, selections and tabs of the last time the Caracatron was closed, before the screen opens.
     */
    private fun restore() {
        searches.putAll(CaracatronState.searches)
        selected = CaracatronState.species?.let { id -> entries.firstOrNull { it.species.resourceIdentifier == id } }
        selectedMaterial = CaracatronState.material?.let { BuiltInRegistries.ITEM.getOptional(it).orElse(null) }
        CaracatronState.spawnTab?.takeIf { it in INFO_TABS }?.let { selectedTab = it }
        CaracatronState.materialTab?.takeIf { it in CaracatronMaterialText.TABS }?.let { selectedMaterialTab = it }
        pendingScroll = true
    }

    /**
     * Remembers the searches, selections and tabs when the screen closes or another page opens over it.
     */
    override fun removed() {
        super.removed()
        if (::searchBox.isInitialized) searches[page] = searchBox.value
        val material = selectedMaterial?.let { BuiltInRegistries.ITEM.getKey(it) }
        CaracatronState.remember(searches, selectedSpecies, material, selectedTab, selectedMaterialTab)
    }

    /**
     * Switches between the Caracatron page, its drops page and its materials page, keeping the selected species, item
     * and filter. Each page keeps its own search text, since one searches Pokémon names and the others item names.
     *
     * @param shown Page to show, [CaracatronPage.CARACA], [CaracatronPage.DROPS] or [CaracatronPage.MATERIALS].
     */
    fun showPage(shown: CaracatronPage) {
        if (shown == page || shown !in navigations) return
        filterSelect.close()
        if (::searchBox.isInitialized) searches[page] = searchBox.value
        page = shown
        infoScroll.reset()
        if (::searchBox.isInitialized) {
            searchBox.placeholder = searchHint()
            searchBox.value = searches[page].orEmpty()
        }
        updateFilter()
        if (showsSpawns) selected?.let(::requestSpawnInfo)
        if (showsMaterials) selectedMaterial?.let(::requestMaterialInfo)
        rebuildInfo()
    }

    private fun searchHint(): Component = Component.translatable(
        when (page) {
            CaracatronPage.DROPS -> "$KEY.search.drops"
            CaracatronPage.MATERIALS -> "$KEY.search.materials"
            else -> "$KEY.search"
        }
    )

    override fun isPauseScreen(): Boolean = false

    override fun renderBlurredBackground(delta: Float) {}

    override fun renderMenuBackground(context: GuiGraphics) {}

    /**
     * Draws the frame, the screen and the panel parts under the widgets.
     */
    override fun renderBackground(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val pose = context.pose()
        blitk(matrixStack = pose, texture = CaracatronPages.FRAME, x = left, y = top, width = BASE_WIDTH, height = BASE_HEIGHT)
        blitk(matrixStack = pose, texture = screenTexture, x = left, y = top, width = BASE_WIDTH, height = BASE_HEIGHT)
        blitk(matrixStack = pose, texture = searchBar, x = left + BAR_X, y = top + SEARCH_BAR_Y, width = BAR_WIDTH, height = BAR_HEIGHT)
        blitk(matrixStack = pose, texture = categoryBar, x = left + BAR_X, y = top + CATEGORY_BAR_Y, width = BAR_WIDTH, height = BAR_HEIGHT)
        blitk(matrixStack = pose, texture = infoOverlay, x = left + INFO_X, y = top + INFO_Y, width = INFO_WIDTH, height = INFO_HEIGHT)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        renderHeader(context)
        renderSearchAndFilter(context)
        if (showsMaterials) renderMaterialSlots(context, mouseX, mouseY) else renderSlots(context, mouseX, mouseY)
        if (pendingScroll) scrollToSelection()
        updateHoveredDrop(mouseX - left, mouseY - top)
        val link = linkAt(mouseX - left, mouseY - top)
        if (link != hoveredLink) {
            hoveredLink = link
            rebuildInfo()
        }
        renderInfo(context)
        navigation.render(context, left, top, (mouseX - left).toDouble(), (mouseY - top).toDouble())
        if (!showsMaterials) {
            if (filterSelect.isOpen) {
                context.flush()
                RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX)
            }
            filterSelect.render(context, left, top, (mouseX - left).toDouble(), (mouseY - top).toDouble(), filter, scrollbarTrack)
        }
        colorSelect.render(context, left, top, (mouseX - left).toDouble(), (mouseY - top).toDouble(), page.label())
        renderTooltips(context, mouseX, mouseY)
    }

    private fun renderHeader(context: GuiGraphics) {
        icon(context, medalIcon, 27.0f, 15.0f, height = 13)
        largeText(context, Component.translatable("$KEY.tier", roman(data.tier)), 37, 14)

        icon(context, caughtSeenIcon, 252.0f, 15.0f, textureHeight = ICON_SIZE * 2)
        largeText(context, Component.literal(availableCount.toString().padStart(4, '0')), 262, 14)

        val caughtCount = entries.count { it.tier <= data.tier && it.species.resourceIdentifier in caughtSpecies }
        icon(context, caughtSeenIcon, 289.0f, 15.0f, vOffset = ICON_SIZE, textureHeight = ICON_SIZE * 2)
        largeText(context, Component.literal(caughtCount.toString().padStart(4, '0')), 299, 14)
    }

    private fun renderSearchAndFilter(context: GuiGraphics) {
        searchBox.screenColors = textColors
        icon(context, searchIcon, 27.0f, 30.0f)
        icon(context, globeIcon, GLOBE_X.toFloat(), GLOBE_Y.toFloat(), alpha = if (data.isShared) 1.0f else DIMMED_ALPHA)

        icon(context, categoryIcon, 27.0f, 182.0f)
        if (showsMaterials) {
            largeText(context, Component.translatable("$KEY.materials.count", materials.size), 37, 181)
            return
        }
        largeText(context, filter.label(), 37, 181)
        val available = filters
        val index = available.indexOf(filter)
        val upAlpha = if (index > 0) 1.0f else DIMMED_ALPHA
        val downAlpha = if (index < available.lastIndex) 1.0f else DIMMED_ALPHA
        arrow(context, CATEGORY_ARROW_X.toFloat(), CATEGORY_UP_Y.toFloat(), pointingUp = true, alpha = upAlpha)
        arrow(context, CATEGORY_ARROW_X.toFloat(), CATEGORY_DOWN_Y.toFloat(), pointingUp = false, alpha = downAlpha)
    }

    private fun renderSlots(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val pose = context.pose()
        val hovered = hoveredSlot(mouseX, mouseY)

        forEachVisibleSlot(context, filtered.size) { index, x, y ->
            val entry = filtered[index]
            renderSlotBackground(context, entry, x, y)
            renderSlotSelection(context, x, y, entry == selected, index == hovered)

            val unfiltered = showsDrops || entry.species.resourceIdentifier in caughtSpecies
            renderModel(context, entry, x, y, unfiltered)

            val darkNumber = unfiltered && textColors.screen == CaracatronTextColors.DARK
            pose.pushPose()
            pose.translate(0.0, 0.0, 100.0)
            drawScaledText(
                context = context,
                text = Component.literal(entry.species.nationalPokedexNumber.toString().padStart(4, '0')),
                x = x + 2,
                y = y + 2.5,
                scale = SCALE,
                colour = if (darkNumber) SLOT_NUMBER_DARK else CaracatronTextColors.LIGHT,
                shadow = !darkNumber
            )
            caughtIconHalf(entry)?.let { half ->
                icon(context, caughtSlotSmallIcon, x - left + SLOT_CAUGHT_X, y - top + SLOT_CAUGHT_Y, width = SMALL_ICON_SIZE, height = SMALL_ICON_SIZE, vOffset = half * SMALL_ICON_SIZE, textureHeight = SMALL_ICON_SIZE * 2)
            }
            pose.popPose()
        }
    }

    /**
     * Draws the materials page list: the plain slot with the item's sprite for every search result, or a message in the
     * middle of the list while the search is too short or found nothing.
     */
    private fun renderMaterialSlots(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val hovered = hoveredSlot(mouseX, mouseY)

        if (materials.isEmpty()) {
            val message = if (materialQueryLength < CaracatronMaterialIndex.MIN_QUERY_LENGTH) "materials.start" else "materials.empty"
            textColors.drawScreenText(
                context,
                Component.translatable("$KEY.$message"),
                left + (LIST_X + LIST_RIGHT) / 2.0,
                top + (LIST_TOP + LIST_BOTTOM) / 2.0 - 2,
                scale = SCALE,
                centered = true
            )
        }

        forEachVisibleSlot(context, materials.size) { index, x, y ->
            val item = materials[index]
            blitk(matrixStack = context.pose(), texture = slotTexture, x = x, y = y, width = SLOT_SIZE, height = SLOT_SIZE)
            renderSlotSelection(context, x, y, item == selectedMaterial, index == hovered)
            context.renderItem(stackOf(item), x + SLOT_ITEM_OFFSET, y + SLOT_ITEM_OFFSET)
        }
    }

    /**
     * Draws every slot of the list visible at the current scroll, clipped to the list, then the list's scroll bar.
     *
     * @param count How many slots the list has.
     * @param slot Draws one slot from its index in the list and its top left corner on screen.
     */
    private inline fun forEachVisibleSlot(context: GuiGraphics, count: Int, slot: (index: Int, x: Int, y: Int) -> Unit) {
        val firstRow = (listScroll.offset / SLOT_STEP).toInt()
        val lastRow = ((listScroll.offset + (LIST_BOTTOM - LIST_TOP)) / SLOT_STEP).toInt()

        context.enableScissor(left + LIST_X, top + LIST_TOP, left + LIST_RIGHT, top + LIST_BOTTOM)
        for (row in firstRow..lastRow) {
            for (column in 0 until COLUMNS) {
                val index = row * COLUMNS + column
                if (index >= count) break
                val x = left + LIST_X + column * SLOT_STEP
                val y = top + LIST_TOP + SLOT_TOP_PADDING + row * SLOT_STEP - listScroll.offset.toInt()
                slot(index, x, y)
            }
        }
        context.disableScissor()

        val rows = ceil(count / COLUMNS.toDouble()).toInt()
        listScroll.resize(rows * SLOT_STEP + SLOT_TOP_PADDING, LIST_BOTTOM - LIST_TOP)
        listScroll.render(context, left, top, scrollbarSlide, scrollbarTrack)
    }

    /**
     * Index of the slot under the mouse, or `-1` when there's none or the color select covers it.
     */
    private fun hoveredSlot(mouseX: Int, mouseY: Int): Int {
        val x = (mouseX - left).toDouble()
        val y = (mouseY - top).toDouble()
        if (colorSelect.isOver(x, y) || filterSelect.isOverDrawer(x, y)) return -1
        return slotAt(mouseX.toDouble(), mouseY.toDouble())
    }

    /**
     * Draws the selected (top half of `slot_select`) or hovered (bottom half) look of a slot.
     */
    private fun renderSlotSelection(context: GuiGraphics, x: Int, y: Int, isSelected: Boolean, isHovered: Boolean) {
        val pose = context.pose()
        if (isSelected) {
            blitk(matrixStack = pose, texture = slotSelect, x = x, y = y, width = SLOT_SIZE, height = SLOT_SIZE, textureHeight = SLOT_SIZE * 2)
        } else if (isHovered) {
            blitk(matrixStack = pose, texture = slotSelect, x = x, y = y, width = SLOT_SIZE, height = SLOT_SIZE, vOffset = SLOT_SIZE, textureHeight = SLOT_SIZE * 2)
        }
    }

    private fun stackOf(item: Item): ItemStack = stackOf(BuiltInRegistries.ITEM.getKey(item))

    private fun stackOf(id: ResourceLocation): ItemStack = itemStacks.getOrPut(id) { BuiltInRegistries.ITEM.get(id).defaultInstance }

    /**
     * Picks the half of the caught icon shown next to a slot's dex number (`caught_seen_icon_slot_small`, 11 px like
     * Cobblemon's `caught_icon_small`) and next to the selected species' name (`caught_seen_icon_slot`, 14 px like
     * Cobblemon's `caught_icon`): the bottom one (red Poké Ball) when the player caught the species, the top one (yellow
     * topped Poké Ball) when only other players did, whatever the globe shows. Both are drawn at half size, like
     * Cobblemon, which keeps the Poké Ball round.
     *
     * @return `1` for the bottom half, `0` for the top one, or `null` when nobody caught the species.
     */
    private fun caughtIconHalf(entry: Entry): Int? {
        val id = entry.species.resourceIdentifier
        return when (id) {
            in data.ownCaught -> 1
            in data.othersCaught -> 0
            else -> null
        }
    }

    /**
     * Draws a slot's model like Cobblemon's Pokédex: default pose, a fresh state and a fresh rotation every frame, since
     * `drawProfilePokemon` conjugates the rotation it gets.
     *
     * Species not caught yet get the [UNCAUGHT_FILTER] over the whole inside of the slot, leaving its border visible.
     * The filter skips the depth test, since the model writes its own depth and would otherwise hide parts of it, and
     * leaves the far [FILTER_Z] depth behind, so the dex number and caught icon still draw on top.
     */
    private fun renderModel(context: GuiGraphics, entry: Entry, x: Int, y: Int, caught: Boolean) {
        val renderable = renderables.getOrPut(entry.species.resourceIdentifier) {
            RenderablePokemon(entry.species, entry.species.standardForm.aspects.toSet())
        }
        val state = FloatingState().also { it.currentAspects = renderable.aspects }

        val pose = context.pose()
        context.enableScissor(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 2)

        pose.pushPose()
        pose.translate(x + SLOT_SIZE / 2.0, y + 1.0, 0.0)
        pose.scale(2.5f, 2.5f, 1.0f)
        drawProfilePokemon(
            renderablePokemon = renderable,
            matrixStack = pose,
            rotation = Quaternionf().fromEulerXYZDegrees(SLOT_ROTATION),
            state = state,
            partialTicks = 0.0f,
            scale = 4.5f
        )
        pose.popPose()

        context.disableScissor()

        if (!caught) depthQuad(context, x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, FILTER_Z, GL11.GL_ALWAYS, UNCAUGHT_FILTER)
    }

    /**
     * Draws a flat quad tested against the depth buffer with a custom function.
     *
     * @param z Depth of the quad.
     * @param depthFunc OpenGL depth function the quad is tested with.
     * @param color ARGB color of the quad.
     */
    private fun depthQuad(
        context: GuiGraphics,
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        z: Float,
        depthFunc: Int,
        color: Int
    ) {
        context.flush()
        val matrix = context.pose().last().pose()
        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(depthFunc)
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()

        val buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
        buffer.addVertex(matrix, x1.toFloat(), y1.toFloat(), z).setColor(color)
        buffer.addVertex(matrix, x1.toFloat(), y2.toFloat(), z).setColor(color)
        buffer.addVertex(matrix, x2.toFloat(), y2.toFloat(), z).setColor(color)
        buffer.addVertex(matrix, x2.toFloat(), y1.toFloat(), z).setColor(color)
        BufferUploader.drawWithShader(buffer.buildOrThrow())

        RenderSystem.depthFunc(GL11.GL_LEQUAL)
        RenderSystem.disableBlend()
    }

    /**
     * Draws a slot's background: the slot texture of the species' tier (`pokemon_slot_t<tier>`) when the icons folder has
     * one for every tier, otherwise the plain slot outlined with the tier's color, which covers tier counts past the
     * textures shipped with the mod.
     */
    private fun renderSlotBackground(context: GuiGraphics, entry: Entry, x: Int, y: Int) {
        if (hasTierSlots) {
            blitk(matrixStack = context.pose(), texture = CaracatronColors.icon("pokemon_slot_t${entry.tier}"), x = x, y = y, width = SLOT_SIZE, height = SLOT_SIZE)
            return
        }
        blitk(matrixStack = context.pose(), texture = slotTexture, x = x, y = y, width = SLOT_SIZE, height = SLOT_SIZE)
        context.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, OUTLINE_ALPHA or entry.color)
    }

    /**
     * Writes the tier the selected species belongs to at the top right of the info panel, in the tier's color with a thin
     * white outline so it stands out from the background.
     */
    private fun renderTierLabel(context: GuiGraphics, entry: Entry) {
        val text = Component.translatable("$KEY.tier", roman(entry.tier))
            .withStyle { it.withFont(CobblemonResources.DEFAULT_LARGE).withBold(true) }
            .visualOrderText
        val x = left + TIER_LABEL_RIGHT - font.width(text)
        font.drawInBatch8xOutline(
            text,
            x.toFloat(),
            (top + TIER_LABEL_Y).toFloat(),
            entry.color,
            TIER_LABEL_OUTLINE,
            context.pose().last().pose(),
            context.bufferSource(),
            LightTexture.FULL_BRIGHT
        )
        context.flush()
    }

    /**
     * Draws the selected item at the top of the info panel on the materials page: its sprite where the types go and its
     * name in bold.
     */
    private fun renderMaterialHeader(context: GuiGraphics, material: Item, panelColor: Int) {
        blitk(matrixStack = context.pose(), texture = typeBar, x = left + INFO_X, y = top + INFO_Y, width = INFO_WIDTH, height = 25)
        context.renderItem(stackOf(material), left + MATERIAL_ICON_X, top + MATERIAL_ICON_Y)
        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = material.description.copy().withStyle(ChatFormatting.BOLD),
            x = left + MATERIAL_NAME_X,
            y = top + 41,
            colour = panelColor,
            maxCharacterWidth = MATERIAL_NAME_MAX_WIDTH
        )
    }

    /**
     * Draws the materials page tabs under the info panel, spread over its width: natural generation, structures, recipes
     * and digging, dimmed when they have nothing for the selected item, with the select arrow over the open one.
     */
    private fun renderMaterialTabs(context: GuiGraphics) {
        val info = selectedMaterialInfo()
        CaracatronMaterialText.TABS.forEachIndexed { index, tab ->
            val x = MATERIAL_TAB_X[index].toFloat()
            val hasContent = info != null && materialTabsWithContent(info)[index]
            icon(context, materialTabIcons[index], x, INFO_TAB_Y.toFloat(), alpha = if (hasContent) 1.0f else DIMMED_ALPHA)
            if (info != null && tab == selectedMaterialTab) arrow(context, x + 0.5f, INFO_TAB_ARROW_Y.toFloat(), pointingUp = false)
        }
    }

    private fun selectedMaterialInfo(): CaracatronMaterialInfoPayload? {
        return selectedMaterial?.let { materialInfo[BuiltInRegistries.ITEM.getKey(it)] }
    }

    private fun materialTabAt(x: Number, y: Number): Int? {
        return MATERIAL_TAB_X.indices.firstOrNull { inside(x, y, (MATERIAL_TAB_X[it] - 1).toInt(), INFO_TAB_Y - 1, 9, 9) }
    }

    private fun renderInfo(context: GuiGraphics) {
        val pose = context.pose()
        val entry = selected

        val panelColor = textColors.panel
        val material = selectedMaterial
        if (showsMaterials) {
            if (material != null) renderMaterialHeader(context, material, panelColor)
        } else if (entry != null) {
            val dualType = entry.species.secondaryType != null
            val nameShift = if (dualType) DUAL_TYPE_SHIFT else 0
            blitk(matrixStack = pose, texture = if (dualType) typeBarLong else typeBar, x = left + INFO_X, y = top + INFO_Y, width = INFO_WIDTH, height = 25)
            TypeIcon(
                x = left + 185,
                y = top + 31,
                type = entry.species.primaryType,
                secondaryType = entry.species.secondaryType,
                secondaryOffset = 9.0f
            ).render(context)
            drawScaledText(
                context = context,
                font = CobblemonResources.DEFAULT_LARGE,
                text = nameMatch(entry, entry.species.nationalPokedexNumber.toString().padStart(4, '0')).withStyle(ChatFormatting.BOLD),
                x = left + 212 + nameShift,
                y = top + 41,
                colour = panelColor
            )
            drawScaledText(
                context = context,
                font = CobblemonResources.DEFAULT_LARGE,
                text = nameMatch(entry, entry.species.translatedName.string).withStyle(ChatFormatting.BOLD),
                x = left + 236 + nameShift,
                y = top + 41,
                colour = panelColor,
                maxCharacterWidth = NAME_MAX_WIDTH - nameShift
            )
            renderTierLabel(context, entry)
            caughtIconHalf(entry)?.let { half ->
                icon(context, caughtSlotIcon, INFO_CAUGHT_X, INFO_CAUGHT_Y, vOffset = half * ICON_SIZE, textureHeight = ICON_SIZE * 2)
            }
        }

        val info = entry?.let { spawnInfo[it.species.resourceIdentifier] }
        if (showsSpawns) {
            INFO_TABS.forEachIndexed { index, tab ->
                val hasContent = info != null && hasContent(info, tab)
                val x = INFO_TAB_X[index]
                val lit = hasContent || tab == SNACK_TAB
                icon(context, tabIcons[index], x.toFloat(), INFO_TAB_Y.toFloat(), alpha = if (lit) 1.0f else DIMMED_ALPHA)
                if (info != null && tab == selectedTab) {
                    arrow(context, x + 0.5f, INFO_TAB_ARROW_Y.toFloat(), pointingUp = false)
                }
            }
        }
        if (showsMaterials) renderMaterialTabs(context)

        val lines = when {
            showsMaterials && material == null -> placeholder("info.select_material")
            showsMaterials -> if (showsInfoLines()) infoLines else placeholder("info.loading")
            entry == null -> placeholder(if (showsDrops) "info.select_drops" else "info.select")
            showsDrops -> infoLines
            info == null -> placeholder("info.loading")
            else -> infoLines
        }

        infoScroll.resize(lines.size * INFO_LINE_HEIGHT + 2, INFO_VIEW_BOTTOM - INFO_VIEW_TOP - 2)
        context.enableScissor(left + INFO_X + 1, top + INFO_VIEW_TOP, left + 313, top + INFO_VIEW_BOTTOM)
        pose.pushPose()
        pose.translate((left + INFO_TEXT_X).toDouble(), top + INFO_TEXT_TOP - infoScroll.offset, 0.0)
        pose.scale(SCALE, SCALE, 1.0f)
        lines.forEachIndexed { index, line ->
            val y = (index * INFO_LINE_HEIGHT / SCALE).toInt()
            val x = lineX(line)
            context.drawString(font, line.text, x, y + line.textOffset, panelTextColor(line.color, textColors), false)
            line.item?.let { context.renderItem(stackOf(it), 0, y + INFO_ICON_OFFSET) }
            line.grid?.let { drawGrid(context, it, y) }
            line.stages?.render(context, y, if (textColors.darkPanel) STAGE_ON_DARK else STAGE_ON_LIGHT)
        }
        pose.popPose()
        context.disableScissor()

        infoScroll.render(context, left, top, scrollbarTrack, scrollbarTrack, trackAlpha = DIMMED_ALPHA)
    }

    /**
     * Draws a recipe grid on the info panel, in its scaled space: a faint square per slot with the item icon over it, the
     * flame of furnace recipes, then the arrow, the result and how many it makes.
     *
     * @param y Top of the grid, in font units.
     */
    private fun drawGrid(context: GuiGraphics, grid: CaracatronRecipeGrid, y: Int) {
        val cellColor = when {
            !textColors.darkPanel -> GRID_CELL_ON_LIGHT
            showsMaterials -> MATERIALS_GRID_CELL_ON_DARK
            else -> GRID_CELL_ON_DARK
        }
        val left = gridLeft(grid)
        val cellSize = CaracatronRecipeGrid.CELL_SIZE
        grid.cells.forEach { cell ->
            val cellX = left + cell.x
            val cellY = y + cell.y
            context.fill(cellX, cellY, cellX + cellSize, cellY + cellSize, cellColor)
            if (cell.kind == CaracatronRecipeGrid.Kind.FUEL) {
                val inset = (cellSize - FLAME_SIZE) / 2
                context.blitSprite(FLAME, cellX + inset, cellY + inset, FLAME_SIZE, FLAME_SIZE)
            }
            cell.item?.let { gridIcon(context, it, cellX, cellY) }
        }

        if (grid.result == null) return
        drawArrow(context, left + grid.arrowX, y + grid.arrowY, if (textColors.darkPanel) ARROW_ON_DARK else ARROW_ON_LIGHT)
        val count = "${grid.resultCount}x"
        val countX = left + grid.resultX + (cellSize - font.width(count)) / 2
        val countY = y + grid.resultY + cellSize + CaracatronRecipeGrid.COUNT_GAP
        context.drawString(font, count, countX, countY, panelTextColor(CaracatronSpawnText.TEXT_COLOR, textColors), false)
    }

    /**
     * Draws an item icon a bit larger than usual, filling a recipe grid cell but its border.
     */
    private fun gridIcon(context: GuiGraphics, item: ResourceLocation, cellX: Int, cellY: Int) {
        val inset = (CaracatronRecipeGrid.CELL_SIZE - CaracatronRecipeGrid.ICON_SIZE) / 2.0f
        val pose = context.pose()
        pose.pushPose()
        pose.translate(cellX + inset, cellY + inset, 0.0f)
        pose.scale(CaracatronRecipeGrid.ICON_SIZE / ITEM_SIZE.toFloat(), CaracatronRecipeGrid.ICON_SIZE / ITEM_SIZE.toFloat(), 1.0f)
        context.renderItem(stackOf(item), 0, 0)
        pose.popPose()
    }

    /**
     * Draws the arrow pointing at a recipe's result: a short bar and a triangle tip.
     */
    private fun drawArrow(context: GuiGraphics, x: Int, y: Int, color: Int) {
        val size = CaracatronRecipeGrid.ARROW_SIZE
        val tip = size / 2
        val shaft = size / 7
        context.fill(x, y + size / 2 - shaft, x + size - tip, y + size / 2 + shaft, color)
        for (step in 0 until tip) {
            context.fill(x + size - tip + step, y + step, x + size - tip + step + 1, y + size - step, color)
        }
    }

    /**
     * Left edge of a line's text on the info panel, in font units: after the icon column when indented, or centered.
     */
    private fun lineX(line: CaracatronInfoLine): Int = when {
        line.centered -> (INFO_UNITS - font.width(line.text)) / 2
        line.indented -> CaracatronInfoLine.ICON_INDENT
        else -> 0
    }

    /**
     * Left edge of a recipe grid on the info panel, in font units, centering it.
     */
    private fun gridLeft(grid: CaracatronRecipeGrid): Int = (INFO_UNITS - grid.widthUnits) / 2

    /**
     * Finds the recipe grid item under the mouse on the info panel: a slot, the side column or the result.
     *
     * @param x Mouse x relative to the Caracatron.
     * @param y Mouse y relative to the Caracatron.
     * @return The item, [FUEL_SLOT] over a flame, or `null` when the mouse isn't over a filled cell.
     */
    private fun gridItemAt(x: Int, y: Int): ResourceLocation? {
        if (!showsInfoLines() || !inside(x, y, INFO_TEXT_X, INFO_VIEW_TOP, INFO_TEXT_WIDTH, INFO_VIEW_BOTTOM - INFO_VIEW_TOP)) return null
        if (colorSelect.isOver(x.toDouble(), y.toDouble()) || filterSelect.isOverDrawer(x.toDouble(), y.toDouble())) return null
        val unitX = (x - INFO_TEXT_X) / SCALE
        val unitY = (y - INFO_TEXT_TOP + infoScroll.offset) / SCALE
        val cellSize = CaracatronRecipeGrid.CELL_SIZE
        infoLines.forEachIndexed { index, line ->
            val grid = line.grid ?: return@forEachIndexed
            val gridX = unitX - gridLeft(grid)
            val gridY = unitY - index * INFO_LINE_HEIGHT / SCALE
            val cell = grid.cells.firstOrNull { gridX >= it.x && gridX < it.x + cellSize && gridY >= it.y && gridY < it.y + cellSize }
            if (cell != null) return if (cell.kind == CaracatronRecipeGrid.Kind.FUEL) FUEL_SLOT else cell.item
        }
        return null
    }

    /**
     * Swaps the info panel's title and plain text colors for the screen color's own, keeping colors that mean something,
     * like the locked tier warning.
     *
     * @param color RGB color the line was built with.
     * @param colors Text colors of the screen color.
     */
    private fun panelTextColor(color: Int, colors: CaracatronTextColors): Int {
        return when (color) {
            CaracatronSpawnText.TITLE_COLOR -> colors.title
            CaracatronSpawnText.TEXT_COLOR -> if (showsMaterials && colors.darkPanel) MATERIALS_DARK_PANEL_TEXT else colors.panel
            else -> color
        }
    }

    /**
     * Whether an info tab has something to show: spawns of that rarity, or seasonings that attract the species.
     */
    private fun hasContent(info: CaracatronSpawnInfoPayload, tab: String): Boolean {
        if (tab == SNACK_TAB) return info.snackSeasonings.isNotEmpty()
        return info.entries.any { it.bucket == tab }
    }

    private fun renderTooltips(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val x = mouseX - left
        val y = mouseY - top
        if (colorSelect.showsTooltip(x.toDouble(), y.toDouble())) {
            context.renderComponentTooltip(font, CaracatronPages.wheelTooltip(Component.translatable("option.caraca_customs.caracatron_color")), mouseX, mouseY)
            return
        }
        if (colorSelect.isOver(x.toDouble(), y.toDouble()) || filterSelect.isOverDrawer(x.toDouble(), y.toDouble())) return

        gridItemAt(x, y)?.let { item ->
            when {
                item == FUEL_SLOT -> context.renderComponentTooltip(font, CaracatronFuels.tooltip(), mouseX, mouseY)
                showsSnack -> context.renderComponentTooltip(font, itemSourceTooltip(item), mouseX, mouseY)
                else -> context.renderTooltip(font, stackOf(item).hoverName, mouseX, mouseY)
            }
            return
        }

        infoItemAt(x, y)?.let { item ->
            if (showsSnack) {
                context.renderComponentTooltip(font, itemSourceTooltip(item), mouseX, mouseY)
            } else {
                context.renderTooltip(font, stackOf(item).hoverName, mouseX, mouseY)
            }
            return
        }

        infoTooltipAt(x, y)?.let { lines ->
            if (lines.isNotEmpty()) {
                val maxLines = (height - TOOLTIP_MARGIN) / TOOLTIP_LINE_HEIGHT
                context.renderComponentTooltip(font, CaracatronBiomeTags.fit(lines, maxLines), mouseX, mouseY)
            }
            return
        }

        if (!showsMaterials && inside(x, y, 26, 180, 125, 11)) {
            context.renderComponentTooltip(font, CaracatronPages.wheelTooltip(filter.tooltip()), mouseX, mouseY)
            return
        }

        val tooltip: Component? = when {
            inside(x, y, 26, 13, 70, 10) -> Component.translatable(if (data.isShared) "$KEY.tier.world" else "$KEY.tier.player")
            inside(x, y, 251, 13, 34, 10) -> Component.translatable("$KEY.available")
            inside(x, y, 288, 13, 34, 10) -> Component.translatable(if (showingWorld) "$KEY.caught.world" else "$KEY.caught.player")
            inside(x, y, GLOBE_X - 1, GLOBE_Y - 1, 9, 9) -> globeTooltip()
            inside(x, y, 26, 180, 125, 11) -> Component.translatable("$KEY.materials.count.tooltip")
            showsMaterials -> materialTabAt(x, y)?.let { Component.translatable("$KEY.materials.tab.${CaracatronMaterialText.TABS[it]}") }
                ?: materials.getOrNull(slotAt(mouseX.toDouble(), mouseY.toDouble()))?.let { stackOf(it).hoverName }
            else -> infoTabAt(x, y)?.takeIf { showsSpawns }?.let { Component.translatable("$KEY.rarity.${INFO_TABS[it]}") }
                ?: filtered.getOrNull(slotAt(mouseX.toDouble(), mouseY.toDouble()))?.species?.translatedName
        }

        tooltip?.let { context.renderTooltip(font, it, mouseX, mouseY) }
    }

    /**
     * Whether the info panel shows [infoLines], rather than a placeholder: the selected species' spawns or drops, or the
     * selected item's sources, once they're known.
     */
    private fun showsInfoLines(): Boolean = when (page) {
        CaracatronPage.CARACA -> selected?.let { spawnInfo[it.species.resourceIdentifier] } != null
        CaracatronPage.DROPS -> selected != null
        CaracatronPage.MATERIALS -> selectedMaterial?.let { materialInfo[BuiltInRegistries.ITEM.getKey(it)] } != null
        else -> false
    }

    /**
     * Finds the item icon under the mouse on the info panel. Icons start at the top of their line and reach into the next
     * one, so both lines are checked.
     *
     * @param x Mouse x relative to the Caracatron.
     * @param y Mouse y relative to the Caracatron.
     * @return The item, or `null` when the mouse isn't over an icon.
     */
    /**
     * Finds the drops page item under the mouse, over its icon or any of its lines ([CaracatronInfoLine.link]).
     *
     * @param x Mouse x relative to the Caracatron.
     * @param y Mouse y relative to the Caracatron.
     * @return The item, or `null` when the mouse isn't over one, or the color select covers it.
     */
    private fun dropItemAt(x: Int, y: Int): ResourceLocation? {
        if (!showsDrops || !showsInfoLines() || colorSelect.isOver(x.toDouble(), y.toDouble())) return null
        if (filterSelect.isOverDrawer(x.toDouble(), y.toDouble())) return null
        if (!inside(x, y, INFO_TEXT_X, INFO_VIEW_TOP, INFO_TEXT_WIDTH, INFO_VIEW_BOTTOM - INFO_VIEW_TOP)) return null
        infoItemAt(x, y)?.let { return it }
        val lineY = y - INFO_TEXT_TOP + infoScroll.offset
        if (lineY < 0) return null
        return infoLines.getOrNull((lineY / INFO_LINE_HEIGHT).toInt())?.link
    }

    /**
     * Underlines the drops page item under the mouse, rebuilding the panel lines only when it changes.
     */
    private fun updateHoveredDrop(x: Int, y: Int) {
        val hovered = dropItemAt(x, y)
        if (hovered == hoveredDrop) return
        hoveredDrop = hovered
        if (showsDrops) rebuildInfo()
    }

    private fun infoItemAt(x: Int, y: Int): ResourceLocation? {
        if (!showsInfoLines()) return null
        val iconSize = ITEM_SIZE * SCALE
        if (!inside(x, y, INFO_TEXT_X, INFO_VIEW_TOP, iconSize.toInt(), INFO_VIEW_BOTTOM - INFO_VIEW_TOP)) return null

        val lineY = y - INFO_TEXT_TOP + infoScroll.offset
        if (lineY < 0) return null
        val index = (lineY / INFO_LINE_HEIGHT).toInt()
        return listOf(index, index - 1).firstNotNullOfOrNull { lineIndex ->
            val iconTop = lineIndex * INFO_LINE_HEIGHT + INFO_ICON_OFFSET * SCALE
            infoLines.getOrNull(lineIndex)?.item?.takeIf { lineY >= iconTop && lineY < iconTop + iconSize }
        }
    }

    /**
     * Builds the tooltip of the info panel text under the mouse: the biomes of a biome group or of a cut biome list
     * ([CaracatronBiomeTags]), what the pasture tags of the drops page mean, or how to get an item on the snack tab
     * ([CaracatronItemSourceText]). Biomes and items are cached while the screen is open.
     *
     * @param x Mouse x relative to the Caracatron.
     * @param y Mouse y relative to the Caracatron.
     * @return The tooltip lines, or `null` when the mouse isn't over text with a tooltip.
     */
    private fun infoTooltipAt(x: Int, y: Int): List<Component>? {
        val insertion = insertionAt(x, y) ?: return null
        return when {
            insertion.startsWith("#") -> biomeTooltips.getOrPut(insertion) { CaracatronBiomeTags.tooltip(insertion).orEmpty() }
            insertion.startsWith(CaracatronMaterialText.MORE_BIOMES) -> biomeTooltips.getOrPut(insertion) {
                CaracatronBiomeTags.listTooltip(insertion.removePrefix(CaracatronMaterialText.MORE_BIOMES).split(CaracatronMaterialText.BIOME_SEPARATOR))
            }
            insertion.startsWith(CaracatronDropText.PASTURE_INSERTION) -> {
                val key = insertion.removePrefix(CaracatronDropText.PASTURE_INSERTION)
                listOf(Component.translatable("text.caraca_customs.caracatron.drops.$key.tooltip"))
            }
            insertion.startsWith(CaracatronSnackText.ITEM_INSERTION) -> {
                val item = ResourceLocation.tryParse(insertion.removePrefix(CaracatronSnackText.ITEM_INSERTION)) ?: return null
                itemSourceTooltip(item)
            }
            else -> null
        }
    }

    /**
     * Builds how to get an item of the snack tab ([CaracatronItemSourceText]), cached while the screen is open.
     */
    private fun itemSourceTooltip(item: ResourceLocation): List<Component> = itemTooltips.getOrPut(item) {
        val source = selected?.let { spawnInfo[it.species.resourceIdentifier] }?.itemSources?.firstOrNull { it.item == item }
        CaracatronItemSourceText.tooltip(item, source)
    }

    /**
     * Finds the text under the mouse on the info panel, by measuring the hovered line up to the mouse (lines drawn lower
     * by [CaracatronInfoLine.textOffset] included), and reads the insertion its style carries ([CaracatronSpawnText.biomeName], [CaracatronSnackText.ITEM_INSERTION]).
     *
     * @param x Mouse x relative to the Caracatron.
     * @param y Mouse y relative to the Caracatron.
     * @return The insertion, or `null` when the mouse isn't over text carrying one.
     */
    private fun insertionAt(x: Int, y: Int): String? {
        if (!showsInfoLines()) return null
        if (!inside(x, y, INFO_TEXT_X, INFO_VIEW_TOP, INFO_TEXT_WIDTH, INFO_VIEW_BOTTOM - INFO_VIEW_TOP)) return null

        val lineY = y - INFO_TEXT_TOP + infoScroll.offset
        if (lineY < 0) return null
        val index = (lineY / INFO_LINE_HEIGHT).toInt()
        val line = listOf(index, index - 1).firstNotNullOfOrNull { lineIndex ->
            infoLines.getOrNull(lineIndex)?.takeIf { line ->
                val textY = lineY - lineIndex * INFO_LINE_HEIGHT - line.textOffset * SCALE
                textY >= 0 && textY < font.lineHeight * SCALE
            }
        } ?: return null
        val textX = ((x - INFO_TEXT_X) / SCALE).toInt() - lineX(line)
        val style = font.splitter.componentStyleAtWidth(line.text, textX) ?: return null
        return style.insertion
    }

    private fun globeTooltip(): Component {
        if (!data.isShared) return Component.translatable("$KEY.globe.solo")
        return Component.translatable(if (showingWorld) "$KEY.globe.world" else "$KEY.globe.player")
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val x = mouseX - left
        val y = mouseY - top

        if (button == 0) {
            if (colorSelect.mouseClicked(x, y)) {
                playClick()
                return true
            }
            if (!showsMaterials && filterSelect.mouseClicked(x, y, filters, filter)) {
                playClick()
                return true
            }
            if (navigation.mouseClicked(x, y)) return true
            if (listScroll.startDrag(x, y) || infoScroll.startDrag(x, y)) return true
            dropItemAt(x.toInt(), y.toInt())?.let { item ->
                searchBox.value = item.toString()
                playClick()
                return true
            }
            gridItemAt(x.toInt(), y.toInt())?.takeIf { it != FUEL_SLOT }?.let { item ->
                openMaterial(item)
                return true
            }
            linkAt(x.toInt(), y.toInt())?.takeIf { it.startsWith(CaracatronSnackText.ITEM_INSERTION) }?.let { link ->
                ResourceLocation.tryParse(link.removePrefix(CaracatronSnackText.ITEM_INSERTION))?.let(::openMaterial)
                return true
            }

            when {
                showsMaterials -> {
                    materialTabAt(x, y)?.let { index ->
                        selectMaterialTab(CaracatronMaterialText.TABS[index])
                        playClick()
                        return true
                    }
                    insertionAt(x.toInt(), y.toInt())?.takeIf { it.startsWith(CaracatronMaterialText.DROPS_LINK) }?.let { link ->
                        searchDrops(link.removePrefix(CaracatronMaterialText.DROPS_LINK))
                        return true
                    }
                    materials.getOrNull(slotAt(mouseX, mouseY))?.let { item ->
                        selectMaterial(item)
                        playClick()
                        return true
                    }
                    return super.mouseClicked(mouseX, mouseY, button)
                }
                inside(x, y, CATEGORY_ARROW_X - 1, CATEGORY_UP_Y - 1, 8, 5) -> return changeFilter(-1)
                inside(x, y, CATEGORY_ARROW_X - 1, CATEGORY_DOWN_Y - 1, 8, 5) -> return changeFilter(1)
                inside(x, y, GLOBE_X - 1, GLOBE_Y - 1, 9, 9) && data.isShared -> {
                    showingWorld = !showingWorld
                    playClick()
                    return true
                }
            }

            infoTabAt(x, y)?.takeIf { showsSpawns }?.let { index ->
                selectTab(INFO_TABS[index])
                playClick()
                return true
            }

            filtered.getOrNull(slotAt(mouseX, mouseY))?.let { entry ->
                select(entry)
                playClick()
                return true
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (colorSelect.drag(deltaY) || filterSelect.drag(deltaY) || listScroll.drag(deltaY) || infoScroll.drag(deltaY)) return true
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        colorSelect.stopDrag()
        filterSelect.stopDrag()
        listScroll.stopDrag()
        infoScroll.stopDrag()
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val x = mouseX - left
        val y = mouseY - top
        if (colorSelect.mouseScrolled(x, y, scrollY)) return true
        if (filterSelect.mouseScrolled(x, y, scrollY)) return true
        if (navigation.mouseScrolled(x, y, scrollY)) return true
        if (!showsMaterials && scrollY != 0.0 && inside(x, y, BAR_X, CATEGORY_BAR_Y, BAR_WIDTH, BAR_HEIGHT)) {
            return changeFilter(if (scrollY > 0) -1 else 1)
        }
        when {
            inside(x, y, LIST_X, LIST_TOP, LIST_RIGHT - LIST_X + 6, LIST_BOTTOM - LIST_TOP) -> listScroll.scrollBy(-scrollY * SLOT_STEP)
            inside(x, y, INFO_X, INFO_VIEW_TOP, INFO_WIDTH, INFO_VIEW_BOTTOM - INFO_VIEW_TOP) -> infoScroll.scrollBy(-scrollY * INFO_LINE_HEIGHT * 2)
            else -> return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        }
        return true
    }

    /**
     * Shows the spawn pool sent by the server, if it belongs to a species of this screen.
     *
     * @param info Spawn pool of a species.
     */
    fun receiveSpawnInfo(info: CaracatronSpawnInfoPayload) {
        spawnInfo[info.species] = info
        if (selected?.species?.resourceIdentifier != info.species) return
        if (selectedTab != SNACK_TAB && info.entries.none { it.bucket == selectedTab }) {
            info.entries.map { it.bucket }.firstOrNull { it in CaracatronSpawnText.BUCKETS }?.let { selectedTab = it }
        }
        rebuildInfo()
    }

    /**
     * Shows where an item comes from, sent by the server, if it's the item selected on the materials page.
     *
     * @param info Sources of an item.
     */
    fun receiveMaterialInfo(info: CaracatronMaterialInfoPayload) {
        materialInfo[info.item] = info
        materialTabContent.remove(info.item)
        val material = selectedMaterial ?: return
        if (BuiltInRegistries.ITEM.getKey(material) != info.item) return
        pickMaterialTab(info)
        if (showsMaterials) rebuildInfo()
    }

    /**
     * Opens the first materials tab with content when the open one has nothing for the item, like the spawn rarities do.
     */
    private fun pickMaterialTab(info: CaracatronMaterialInfoPayload) {
        val content = materialTabsWithContent(info)
        if (content[CaracatronMaterialText.TABS.indexOf(selectedMaterialTab)]) return
        CaracatronMaterialText.TABS.indices.firstOrNull { content[it] }?.let { selectedMaterialTab = CaracatronMaterialText.TABS[it] }
    }

    /**
     * Whether each materials tab has something for an item, worked out once per item instead of on every frame.
     *
     * @return One flag per tab, in [CaracatronMaterialText.TABS] order.
     */
    private fun materialTabsWithContent(info: CaracatronMaterialInfoPayload): List<Boolean> = materialTabContent.getOrPut(info.item) {
        val dropping = dropIndex.holderCount(info.item)
        val stages = stagesOf(info)
        CaracatronMaterialText.TABS.map { CaracatronMaterialText.hasContent(info, it, dropping, stages) }
    }

    /**
     * Growth stages of an item of the materials page, worked out once per item ([CaracatronGrowthStages.of]).
     */
    private fun stagesOf(info: CaracatronMaterialInfoPayload): CaracatronGrowthStages? = materialStages.getOrPut(info.item) {
        CaracatronGrowthStages.of(info.item, info.blocks.map { it.block })
    }

    /**
     * Opens the drops page searching an item, from the Pokémon drops link of the materials page, and selects the first
     * Pokémon of the list.
     *
     * @param item Item id, searched as is (like `minecraft:emerald`).
     */
    private fun searchDrops(item: String) {
        searches[CaracatronPage.DROPS] = item
        CaracatronPages.open(CaracatronPage.DROPS, this)
        filtered.firstOrNull()?.let(::select)
    }

    /**
     * Color of the materials page link to the drops page: the label color, or a brighter one while it's hovered.
     */
    private fun linkColor(): Int = if (hoveredLink?.startsWith(CaracatronMaterialText.DROPS_LINK) == true) textColors.linkHover else textColors.label

    /**
     * Finds the link under the mouse on the info panel: the drops link of the materials page, or an item name of the
     * snack tab.
     *
     * @param x Mouse x relative to the Caracatron.
     * @param y Mouse y relative to the Caracatron.
     * @return The link's insertion, or `null` when the mouse isn't over one, or a drawer covers it.
     */
    private fun linkAt(x: Int, y: Int): String? {
        if (colorSelect.isOver(x.toDouble(), y.toDouble()) || filterSelect.isOverDrawer(x.toDouble(), y.toDouble())) return null
        val insertion = insertionAt(x, y) ?: return null
        return when {
            showsMaterials && insertion.startsWith(CaracatronMaterialText.DROPS_LINK) -> insertion
            showsSnack && insertion.startsWith(CaracatronSnackText.ITEM_INSERTION) -> insertion
            else -> null
        }
    }

    /**
     * Opens an item on the materials page, from the snack tab or a recipe grid: searches its id, selects it and goes back
     * to the natural generation tab.
     *
     * @param id Item id.
     */
    private fun openMaterial(id: ResourceLocation) {
        val item = BuiltInRegistries.ITEM.getOptional(id).orElse(null) ?: return
        searches[CaracatronPage.MATERIALS] = id.toString()
        selectedMaterialTab = CaracatronMaterialText.WORLD
        selectedMaterial = item
        infoScroll.reset()
        if (showsMaterials) {
            searchBox.value = id.toString()
            requestMaterialInfo(item)
            CaracatronPages.playClick()
        } else {
            CaracatronPages.open(CaracatronPage.MATERIALS, this)
        }
        pendingScroll = true
    }

    /**
     * Scrolls the list to the selected species or item, once the list knows its size. Used when the Caracatron opens on
     * its last selection and when an item is opened from another page.
     */
    private fun scrollToSelection() {
        pendingScroll = false
        val index = if (showsMaterials) materials.indexOf(selectedMaterial) else filtered.indexOf(selected)
        if (index > 0) listScroll.scrollBy((index / COLUMNS * SLOT_STEP).toDouble())
    }

    private fun selectMaterialTab(tab: String) {
        if (tab == selectedMaterialTab) return
        selectedMaterialTab = tab
        infoScroll.reset()
        rebuildInfo()
    }

    private fun selectMaterial(item: Item) {
        if (item == selectedMaterial) return
        selectedMaterial = item
        infoScroll.reset()
        requestMaterialInfo(item)
    }

    /**
     * Shows an item's cached sources, or asks the server for them.
     */
    private fun requestMaterialInfo(item: Item) {
        val id = BuiltInRegistries.ITEM.getKey(item)
        val cached = materialInfo[id]
        if (cached != null) {
            pickMaterialTab(cached)
            rebuildInfo()
        } else if (ClientPlayNetworking.canSend(CaracatronMaterialRequestPayload.TYPE)) {
            ClientPlayNetworking.send(CaracatronMaterialRequestPayload(id))
        }
    }

    private fun select(entry: Entry) {
        if (entry == selected) return
        selected = entry
        infoScroll.reset()
        if (showsDrops) rebuildInfo() else requestSpawnInfo(entry)
    }

    /**
     * Shows a species' cached spawn pool, or asks the server for it. The drops page never asks, its data is already on
     * the client.
     */
    private fun requestSpawnInfo(entry: Entry) {
        val id = entry.species.resourceIdentifier
        val cached = spawnInfo[id]
        if (cached != null) {
            receiveSpawnInfo(cached)
        } else if (ClientPlayNetworking.canSend(CaracatronSpawnRequestPayload.TYPE)) {
            ClientPlayNetworking.send(CaracatronSpawnRequestPayload(id))
        }
    }

    private fun selectTab(tab: String) {
        if (tab == selectedTab) return
        selectedTab = tab
        infoScroll.reset()
        rebuildInfo()
    }

    private fun rebuildInfo() {
        val width = (INFO_TEXT_WIDTH / SCALE).toInt()
        if (showsMaterials) {
            val material = selectedMaterial ?: return
            val id = BuiltInRegistries.ITEM.getKey(material)
            val info = materialInfo[id] ?: return
            infoLines = CaracatronMaterialText.lines(
                info, selectedMaterialTab, dropIndex.holderCount(id), stagesOf(info), textColors.label, linkColor(), font, width
            )
            return
        }
        val entry = selected ?: return
        if (showsDrops) {
            infoLines = CaracatronDropText.lines(dropIndex.dropsAt(entry.index), data.pastureLoot, matchedItems, textColors.match, hoveredDrop, font, width)
            return
        }
        val info = spawnInfo[entry.species.resourceIdentifier] ?: return
        infoLines = if (selectedTab == SNACK_TAB) {
            val hoveredItem = hoveredLink?.takeIf { showsSnack }?.removePrefix(CaracatronSnackText.ITEM_INSERTION)?.let(ResourceLocation::tryParse)
            CaracatronSnackText.lines(info, entry.species, hoveredItem, textColors.linkHover, font, width)
        } else {
            CaracatronSpawnText.lines(info, selectedTab, data.tier, font, width)
        }
    }

    private fun changeFilter(step: Int): Boolean {
        val available = filters
        val index = available.indexOf(filter).coerceAtLeast(0)
        val next = (index + step).coerceIn(0, available.lastIndex)
        if (next != index) {
            filter = available[next]
            updateFilter()
            playClick()
        }
        return true
    }

    /**
     * Lists the species of the selected filter matching the search: by name or dex number on the Caracatron page, by the
     * items they drop on the drops page ([CaracatronDropIndex]), plus the ones whose name or number matches, where an empty
     * search lists every species. The materials
     * page lists the items matching the search instead ([CaracatronMaterialIndex]).
     *
     * "By biggest drop" puts first the species dropping the most of the searched items, by average amount times chance.
     * It goes away with the search, and the list falls back to "All by tier".
     */
    private fun updateFilter() {
        val query = normalize(searchBox.value.trim())
        if (showsMaterials) {
            materialQueryLength = query.length
            materials = materialIndex.search(query)
            listScroll.reset()
            return
        }
        dropQuery = if (showsDrops) query else ""
        if (filter !in filters) filter = CaracatronFilter.AllByTier
        val current = filter
        val matching = if (showsDrops && query.isNotEmpty()) {
            val result = dropIndex.search(query)
            if (result.items != matchedItems) {
                matchedItems = result.items
                rebuildInfo()
            }
            dropSpecies = result.species
            entries.filter { current.accepts(it.tier) && (result.species[it.index] || matchesSearch(it, query)) }
        } else {
            dropSpecies = null
            if (matchedItems.isNotEmpty()) {
                matchedItems = emptySet()
                rebuildInfo()
            }
            entries.filter { current.accepts(it.tier) && (showsDrops || matchesSearch(it, query)) }
        }
        filtered = when {
            current == CaracatronFilter.ByDrop -> matching.sortedByDescending { dropIndex.bestDrop(it.index, matchedItems) }
            current.sortsByTier -> matching.sortedBy { it.tier }
            else -> matching
        }
        listScroll.reset()
    }

    /**
     * Writes the selected species' name or dex number, with the part matching the drops page search underlined in the
     * match green when the species is listed for its name or number rather than for a drop.
     *
     * @param entry Selected species.
     * @param text Name or four digit dex number.
     */
    private fun nameMatch(entry: Entry, text: String): MutableComponent {
        val drops = dropSpecies
        if (!showsDrops || dropQuery.isEmpty() || drops == null || drops[entry.index]) return Component.literal(text)

        val normalized = StringBuilder()
        val origin = mutableListOf<Int>()
        text.forEachIndexed { index, char ->
            val piece = normalize(char.toString())
            repeat(piece.length) { origin += index }
            normalized.append(piece)
        }
        val at = normalized.indexOf(dropQuery)
        if (at < 0) return Component.literal(text)

        val from = origin[at]
        val to = origin[at + dropQuery.length - 1] + 1
        val match = textColors.match
        return Component.literal(text.substring(0, from))
            .append(Component.literal(text.substring(from, to)).withStyle { it.withColor(match).withUnderlined(true) })
            .append(text.substring(to))
    }

    /**
     * Checks a species against the search text: part of its name or of its four digit dex number.
     */
    private fun matchesSearch(entry: Entry, query: String): Boolean {
        return query.isEmpty()
            || entry.searchName.contains(query)
            || entry.species.nationalPokedexNumber.toString().padStart(4, '0').contains(query)
    }

    /**
     * Index in [filtered] of the slot under the mouse, or `-1`.
     */
    private fun slotAt(mouseX: Double, mouseY: Double): Int {
        val x = mouseX - left - LIST_X
        val y = mouseY - top - LIST_TOP
        if (x < 0 || mouseX - left >= LIST_RIGHT || y < 0 || mouseY - top >= LIST_BOTTOM) return -1

        val contentY = y + listScroll.offset - SLOT_TOP_PADDING
        if (contentY < 0) return -1
        val column = (x / SLOT_STEP).toInt()
        val row = (contentY / SLOT_STEP).toInt()
        if (column >= COLUMNS || x % SLOT_STEP >= SLOT_SIZE || contentY % SLOT_STEP >= SLOT_SIZE) return -1
        return row * COLUMNS + column
    }

    private fun infoTabAt(x: Number, y: Number): Int? {
        return INFO_TAB_X.indices.firstOrNull { inside(x, y, INFO_TAB_X[it] - 1, INFO_TAB_Y - 1, 9, 9) }
    }

    private fun inside(x: Number, y: Number, areaX: Int, areaY: Int, areaWidth: Int, areaHeight: Int): Boolean {
        val px = x.toDouble()
        val py = y.toDouble()
        return px >= areaX && px < areaX + areaWidth && py >= areaY && py < areaY + areaHeight
    }

    private fun placeholder(key: String): List<CaracatronInfoLine> {
        return font.split(Component.translatable("$KEY.$key"), (INFO_TEXT_WIDTH / SCALE).toInt()).map { CaracatronInfoLine(it, CaracatronSpawnText.TEXT_COLOR) }
    }

    /**
     * Draws an icon (or part of a taller sheet) at half size, Cobblemon style, 14 × 14 unless told otherwise. Positions are
     * relative to the Caracatron's top left corner.
     */
    private fun icon(
        context: GuiGraphics,
        texture: ResourceLocation,
        x: Float,
        y: Float,
        width: Int = ICON_SIZE,
        height: Int = ICON_SIZE,
        vOffset: Int = 0,
        textureHeight: Int = height,
        alpha: Float = 1.0f,
        scale: Float = SCALE
    ) {
        blitk(
            matrixStack = context.pose(),
            texture = texture,
            x = (left + x) / scale,
            y = (top + y) / scale,
            width = width,
            height = height,
            vOffset = vOffset,
            textureHeight = textureHeight,
            alpha = alpha,
            scale = scale
        )
    }

    /**
     * Draws the select arrow at half size. The texture points down, so the up arrow is drawn mirrored.
     */
    private fun arrow(context: GuiGraphics, x: Float, y: Float, pointingUp: Boolean, alpha: Float = 1.0f) {
        val pose = context.pose()
        pose.pushPose()
        pose.translate(left + x, top + y + if (pointingUp) ARROW_HEIGHT * SCALE else 0.0f, 0.0f)
        pose.scale(SCALE, if (pointingUp) -SCALE else SCALE, 1.0f)
        RenderSystem.disableCull()
        blitk(matrixStack = pose, texture = selectArrow, x = 0, y = 0, width = ARROW_WIDTH, height = ARROW_HEIGHT, alpha = alpha)
        RenderSystem.enableCull()
        pose.popPose()
    }

    /**
     * Writes a bold text in Cobblemon's large font on the header or the filter bar, always white.
     */
    private fun largeText(context: GuiGraphics, text: MutableComponent, x: Int, y: Int) {
        val bold = text.withStyle(ChatFormatting.BOLD)
        CaracatronTextColors.drawHeaderText(context, bold, left + x, top + y, font = CobblemonResources.DEFAULT_LARGE)
    }

    private fun playClick() {
        Minecraft.getInstance().soundManager.play(
            SimpleSoundInstance.forUI(CobblemonSounds.POKEDEX_CLICK, 1.0f)
        )
    }
}
