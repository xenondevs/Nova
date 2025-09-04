package xyz.xenondevs.nova.util.item

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.CustomModelData
import org.bukkit.Color
import org.bukkit.inventory.ItemStack

private const val DEFAULT_FLOAT = 0f
private const val DEFAULT_FLAG = false
private const val DEFAULT_STRING = ""
private val DEFAULT_COLOR = Color.WHITE

/**
 * Sets the [CustomModelData.floats] at the given [index] to [value].
 * Fills all intermediary indices with [DEFAULT_FLOAT] if necessary.
 */
fun ItemStack.setCustomModelDataFloat(index: Int, value: Float) =
    setCustomModelDataFloats(index, listOf(value))

/**
 * Sets the [CustomModelData.flags] at the given [index] to [value].
 * Fills all intermediary indices with [DEFAULT_FLAG] if necessary.
 */
fun ItemStack.setCustomModelDataFlag(index: Int, value: Boolean) =
    setCustomModelDataFlags(index, listOf(value))

/**
 * Sets the [CustomModelData.strings] at the given [index] to [value].
 * Fills all intermediary indices with [DEFAULT_STRING] if necessary.
 */
fun ItemStack.setCustomModelDataString(index: Int, value: String) =
    setCustomModelDataStrings(index, listOf(value))

/**
 * Sets the [CustomModelData.colors] at the given [index] to [value].
 * Fills all intermediary indices with [DEFAULT_COLOR] if necessary.
 */
fun ItemStack.setCustomModelDataColor(index: Int, value: Color) =
    setCustomModelDataColors(index, listOf(value))

/**
 * Overrides the range from [`offset`, `offset + patch.size`) in the [CustomModelData.floats] list with the values from [patch].
 * Fills all intermediary indices with [DEFAULT_FLOAT] if necessary.
 */
fun ItemStack.setCustomModelDataFloats(
    offset: Int,
    patch: List<Float>
) = setCustomModelData(offset, patch, DEFAULT_FLOAT, CustomModelData::floats, ::rebuildFloats)

/**
 * Overrides the range from [`offset`, `offset + patch.size`) in the [CustomModelData.flags] list with the values from [patch].
 * Fills all intermediary indices with [DEFAULT_FLAG] if necessary.
 */
fun ItemStack.setCustomModelDataFlags(
    offset: Int,
    patch: List<Boolean>
) = setCustomModelData(offset, patch, DEFAULT_FLAG, CustomModelData::flags, ::rebuildFlags)

/**
 * Overrides the range from [`offset`, `offset + patch.size`) in the [CustomModelData.strings] list with the values from [patch].
 * Fills all intermediary indices with [DEFAULT_STRING] if necessary.
 */
fun ItemStack.setCustomModelDataStrings(
    offset: Int,
    patch: List<String>
) = setCustomModelData(offset, patch, DEFAULT_STRING, CustomModelData::strings, ::rebuildStrings)

/**
 * Overrides the range from [`offset`, `offset + patch.size`) in the [CustomModelData.colors] list with the values from [patch].
 * Fills all intermediary indices with [DEFAULT_COLOR] if necessary.
 */
fun ItemStack.setCustomModelDataColors(
    offset: Int,
    patch: List<Color>
) = setCustomModelData(offset, patch, DEFAULT_COLOR, CustomModelData::colors, ::rebuildColors)

private fun <T> ItemStack.setCustomModelData(
    offset: Int,
    patch: List<T>,
    default: T,
    getSection: CustomModelData.() -> List<T>,
    rebuild: (previous: CustomModelData?, elements: List<T>) -> CustomModelData
) {
    val currentCmd = getData(DataComponentTypes.CUSTOM_MODEL_DATA)
    
    // write cmd strings from offset to offset + strings.size
    val elements = (currentCmd?.getSection() ?: emptyList()).toMutableList()
    while (elements.size <= (offset + patch.size - 1)) {
        elements.add(default)
    }
    for ((i, s) in patch.withIndex()) {
        elements[offset + i] = s
    }
    
    setData(DataComponentTypes.CUSTOM_MODEL_DATA, rebuild(currentCmd, elements))
}

/**
 * Clears the [CustomModelData.floats] at the given [index] either removing
 * the element (if it is at the end of the list) or setting it to [DEFAULT_FLOAT].
 */
fun ItemStack.clearCustomModelDataFloat(index: Int) =
    clearCustomModelDataFloats(index, 1)

/**
 * Clears the [CustomModelData.flags] at the given [index] either removing
 * the element (if it is at the end of the list) or setting it to [DEFAULT_FLAG].
 */
fun ItemStack.clearCustomModelDataFlag(index: Int) =
    clearCustomModelDataFlags(index, 1)

/**
 * Clears the [CustomModelData.strings] at the given [index] either removing
 * the element (if it is at the end of the list) or setting it to [DEFAULT_STRING].
 */
fun ItemStack.clearCustomModelDataString(index: Int) =
    clearCustomModelDataStrings(index, 1)

/**
 * Clears the [CustomModelData.colors] at the given [index] either removing
 * the element (if it is at the end of the list) or setting it to [DEFAULT_COLOR].
 */
fun ItemStack.clearCustomModelDataColor(index: Int) =
    clearCustomModelDataColors(index, 1)

/**
 * Clears the range from [`offset`, `offset + length`) in the [CustomModelData.floats] either removing
 * the elements (if they are at the end of the list) or setting them to [DEFAULT_FLOAT].
 */
fun ItemStack.clearCustomModelDataFloats(
    offset: Int,
    length: Int
) = clearCustomModelData(offset, length, DEFAULT_FLOAT, CustomModelData::floats, ::rebuildFloats)

/**
 * Clears the range from [`offset`, `offset + length`) in the [CustomModelData.flags] either removing
 * the elements (if they are at the end of the list) or setting them to [DEFAULT_FLAG].
 */
fun ItemStack.clearCustomModelDataFlags(
    offset: Int,
    length: Int
) = clearCustomModelData(offset, length, DEFAULT_FLAG, CustomModelData::flags, ::rebuildFlags)

/**
 * Clears the range from [`offset`, `offset + length`) in the [CustomModelData.strings] either removing
 * the elements (if they are at the end of the list) or setting them to [DEFAULT_STRING].
 */
fun ItemStack.clearCustomModelDataStrings(
    offset: Int,
    length: Int
) = clearCustomModelData(offset, length, DEFAULT_STRING, CustomModelData::strings, ::rebuildStrings)

/**
 * Clears the range from [`offset`, `offset + length`) in the [CustomModelData.colors] either removing
 * the elements (if they are at the end of the list) or setting them to [DEFAULT_COLOR].
 */
fun ItemStack.clearCustomModelDataColors(
    offset: Int,
    length: Int
) = clearCustomModelData(offset, length, DEFAULT_COLOR, CustomModelData::colors, ::rebuildColors)

private fun <T> ItemStack.clearCustomModelData(
    offset: Int,
    length: Int,
    default: T,
    getSection: CustomModelData.() -> List<T>,
    rebuild: (previous: CustomModelData?, elements: List<T>) -> CustomModelData
) {
    val currentCmd = getData(DataComponentTypes.CUSTOM_MODEL_DATA)
    
    val elements = currentCmd?.getSection()?.toMutableList()
        ?: return
    
    // override range with default element
    for (i in offset..<(offset + length)) {
        if (i >= elements.size)
            break
        elements[i] = default
    }
    
    // drop empty elements at tail
    for (i in elements.indices.reversed()) {
        if (elements[i] != default)
            break
        elements.removeAt(i)
    }
    
    setData(DataComponentTypes.CUSTOM_MODEL_DATA, rebuild(currentCmd, elements))
}

private fun rebuildFloats(prev: CustomModelData?, elements: List<Float>) =
    CustomModelData.customModelData()
        .addFloats(elements)
        .addFlags(prev?.flags() ?: emptyList())
        .addStrings(prev?.strings() ?: emptyList())
        .addColors(prev?.colors() ?: emptyList())
        .build()

private fun rebuildFlags(prev: CustomModelData?, elements: List<Boolean>) =
    CustomModelData.customModelData()
        .addFloats(prev?.floats() ?: emptyList())
        .addFlags(elements)
        .addStrings(prev?.strings() ?: emptyList())
        .addColors(prev?.colors() ?: emptyList())
        .build()

private fun rebuildStrings(prev: CustomModelData?, elements: List<String>) =
    CustomModelData.customModelData()
        .addFloats(prev?.floats() ?: emptyList())
        .addFlags(prev?.flags() ?: emptyList())
        .addStrings(elements)
        .addColors(prev?.colors() ?: emptyList())
        .build()

private fun rebuildColors(prev: CustomModelData?, elements: List<Color>) =
    CustomModelData.customModelData()
        .addFloats(prev?.floats() ?: emptyList())
        .addFlags(prev?.flags() ?: emptyList())
        .addStrings(prev?.strings() ?: emptyList())
        .addColors(elements)
        .build()