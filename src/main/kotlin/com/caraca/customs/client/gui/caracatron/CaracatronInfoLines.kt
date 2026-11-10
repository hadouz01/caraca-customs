package com.caraca.customs.client.gui.caracatron

import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence
import kotlin.math.ceil

/**
 * Collects the lines of the Caracatron info panel, wrapping each text to the panel's width.
 *
 * @param font Font used to wrap the lines.
 * @param width Wrapping width, in font units.
 *
 * @author hadouz01
 * @since 1.29.6
 */
class CaracatronInfoLines(private val font: Font, private val width: Int) {

    /**
     * Lines added so far, open for lines built by hand (icons, indents).
     */
    val lines = mutableListOf<CaracatronInfoLine>()

    /**
     * Wraps a text and adds its lines.
     *
     * @param component Text to add.
     * @param color RGB color of the text.
     * @param centered Whether the text is centered on the panel.
     */
    fun add(component: Component, color: Int = CaracatronSpawnText.TEXT_COLOR, centered: Boolean = false) {
        font.split(component, width).forEach { lines += CaracatronInfoLine(it, color, centered = centered) }
    }

    /**
     * Wraps a text without adding it, for lines built by hand.
     *
     * @param component Text to wrap.
     * @param indent Units taken from the width, like an icon column.
     */
    fun split(component: Component, indent: Int = 0): List<FormattedCharSequence> = font.split(component, width - indent)

    /**
     * Adds an empty line.
     */
    fun space() {
        lines += CaracatronInfoLine(FormattedCharSequence.EMPTY, CaracatronSpawnText.TEXT_COLOR)
    }

    /**
     * Adds a recipe grid, followed by enough empty lines to make room for it.
     *
     * @param grid Recipe grid.
     */
    fun grid(grid: CaracatronRecipeGrid) {
        val lineCount = ceil(grid.heightUnits / LINE_UNITS.toDouble()).toInt()
        lines += CaracatronInfoLine(FormattedCharSequence.EMPTY, CaracatronSpawnText.TEXT_COLOR, grid = grid)
        repeat(lineCount - 1) { space() }
    }

    /**
     * Adds a plant's growth stages, followed by enough empty lines to make room for them.
     *
     * @param stages Growth stages.
     */
    fun stages(stages: CaracatronGrowthStages) {
        val lineCount = ceil(stages.heightUnits / LINE_UNITS.toDouble()).toInt()
        lines += CaracatronInfoLine(FormattedCharSequence.EMPTY, CaracatronSpawnText.TEXT_COLOR, stages = stages)
        repeat(lineCount - 1) { space() }
    }

    private companion object {
        /**
         * Height of a line, in font units.
         */
        const val LINE_UNITS = 10
    }
}

/**
 * A recipe laid out as a grid on the info panel, one item icon per slot, like `example_recipes.png`: the grid, an
 * optional side column (the Campfire Pot's seasonings), then an arrow pointing at the result and how many it makes.
 *
 * Every position is in font units, from the grid's top left corner.
 *
 * @property width Columns of the grid.
 * @property slots Slots of the grid, row by row, `null` for empty ones.
 * @property fuel Whether a flame slot goes under the grid, for recipes burning fuel.
 * @property side Slots of the column right of the grid, `null` for empty ones.
 * @property result Item the recipe makes, drawn after the arrow, `null` for no arrow.
 * @property resultCount How many items the recipe makes.
 *
 * @author hadouz01
 * @since 1.33.0
 */
data class CaracatronRecipeGrid(
    val width: Int,
    val slots: List<ResourceLocation?>,
    val fuel: Boolean = false,
    val side: List<ResourceLocation?> = emptyList(),
    val result: ResourceLocation? = null,
    val resultCount: Int = 1
) {

    /**
     * What a cell of the grid holds.
     */
    enum class Kind { ITEM, FUEL, RESULT }

    /**
     * A cell of the grid.
     *
     * @property x Left edge, in font units.
     * @property y Top edge, in font units.
     * @property item Item in the cell, `null` when empty or for the flame.
     * @property kind What the cell holds.
     */
    data class Cell(val x: Int, val y: Int, val item: ResourceLocation?, val kind: Kind)

    /**
     * Rows of item slots.
     */
    val itemRows: Int get() = (slots.size + width - 1) / width

    /**
     * Rows of the grid, the flame included.
     */
    val height: Int get() = itemRows + if (fuel) 1 else 0

    private val gridRight: Int get() = width * SLOT_STEP - GAP

    private val sideX: Int get() = gridRight + SIDE_GAP

    private val contentRight: Int get() = if (side.isEmpty()) gridRight else sideX + CELL_SIZE

    private val itemsHeight: Int get() = maxOf(itemRows, side.size) * SLOT_STEP - GAP

    /**
     * Left edge of the arrow.
     */
    val arrowX: Int get() = contentRight + ARROW_GAP

    /**
     * Top edge of the arrow, centered on the item rows.
     */
    val arrowY: Int get() = (itemsHeight - ARROW_SIZE) / 2

    /**
     * Top edge of the result cell, centered on the item rows.
     */
    val resultY: Int get() = (itemsHeight - CELL_SIZE) / 2

    /**
     * Left edge of the result cell.
     */
    val resultX: Int get() = arrowX + ARROW_SIZE + ARROW_GAP

    /**
     * Whole width, in font units.
     */
    val widthUnits: Int get() = if (result == null) contentRight else resultX + CELL_SIZE

    /**
     * Whole height, in font units, the result count included.
     */
    val heightUnits: Int
        get() {
            val grid = height * SLOT_STEP - GAP
            return if (result == null) grid else maxOf(grid, resultY + CELL_SIZE + COUNT_GAP + COUNT_HEIGHT)
        }

    /**
     * Every cell of the grid: the item slots, the flame, the side column and the result.
     */
    val cells: List<Cell> by lazy {
        val cells = mutableListOf<Cell>()
        for (index in 0 until width * itemRows) {
            cells += Cell(index % width * SLOT_STEP, index / width * SLOT_STEP, slots.getOrNull(index), Kind.ITEM)
        }
        if (fuel) cells += Cell(0, itemRows * SLOT_STEP, null, Kind.FUEL)
        side.forEachIndexed { index, item -> cells += Cell(sideX, index * SLOT_STEP, item, Kind.ITEM) }
        result?.let { cells += Cell(resultX, resultY, it, Kind.RESULT) }
        cells
    }

    companion object {
        /**
         * Size of a cell, in font units.
         */
        const val CELL_SIZE = 20

        /**
         * Size of the item icon inside a cell, in font units.
         */
        const val ICON_SIZE = 18

        /**
         * Distance between two slots, in font units: a cell and a 2 unit gap.
         */
        const val SLOT_STEP = 22

        /**
         * Size of the arrow, in font units.
         */
        const val ARROW_SIZE = 14

        /**
         * Gap between the result cell and its count, and the count's height, in font units.
         */
        const val COUNT_GAP = 2
        const val COUNT_HEIGHT = 9

        private const val GAP = SLOT_STEP - CELL_SIZE
        private const val SIDE_GAP = 6
        private const val ARROW_GAP = 6
    }
}
