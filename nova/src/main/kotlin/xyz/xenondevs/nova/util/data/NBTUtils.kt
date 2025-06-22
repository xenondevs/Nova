package xyz.xenondevs.nova.util.data

import net.minecraft.nbt.ByteArrayTag
import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.EndTag
import net.minecraft.nbt.FloatTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.LongArrayTag
import net.minecraft.nbt.LongTag
import net.minecraft.nbt.ShortTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.nbt.TagType
import kotlin.jvm.optionals.getOrNull

object NBTUtils {
    
    const val TAG_END = 0
    const val TAG_BYTE = 1
    const val TAG_SHORT = 2
    const val TAG_INT = 3
    const val TAG_LONG = 4
    const val TAG_FLOAT = 5
    const val TAG_DOUBLE = 6
    const val TAG_BYTE_ARRAY = 7
    const val TAG_STRING = 8
    const val TAG_LIST = 9
    const val TAG_COMPOUND = 10
    const val TAG_INT_ARRAY = 11
    const val TAG_LONG_ARRAY = 12
    const val TAG_ANY_NUMERIC = 99
    
    val TAG_TYPES: Array<TagType<*>> = arrayOf(
        EndTag.TYPE, ByteTag.TYPE, ShortTag.TYPE, IntTag.TYPE, LongTag.TYPE, FloatTag.TYPE, DoubleTag.TYPE,
        ByteArrayTag.TYPE, StringTag.TYPE, ListTag.TYPE, CompoundTag.TYPE, IntArrayTag.TYPE, LongArrayTag.TYPE
    )
    
    fun createDoubleList(vararg doubles: Double): ListTag {
        val listTag = ListTag()
        doubles.forEach { listTag.add(DoubleTag.valueOf(it)) }
        return listTag
    }
    
    fun createFloatList(vararg floats: Float): ListTag {
        val listTag = ListTag()
        floats.forEach { listTag.add(FloatTag.valueOf(it)) }
        return listTag
    }
    
    fun createStringList(strings: Iterable<String>): ListTag {
        val listTag = ListTag()
        strings.forEach { listTag.add(StringTag.valueOf(it)) }
        return listTag
    }
    
    /**
     * Sanitizes [tag] from item-related data by removing commonly used keys for items.
     */
    fun removeItemData(tag: CompoundTag): CompoundTag {
        tag.remove("Items")
        tag.remove("HandItems")
        tag.remove("ArmorItems")
        tag.remove("SaddleItem")
        tag.remove("Inventory")
        tag.remove("equipment")
        
        return tag
    }
    
}

@Suppress("UNCHECKED_CAST")
fun <T : Tag> CompoundTag.getOrPut(key: String, defaultValue: () -> T): T {
    if (contains(key))
        return get(key) as T
    
    val value = defaultValue()
    put(key, value)
    
    return value
}

@Suppress("UNCHECKED_CAST")
fun <T : Tag> CompoundTag.getOrNull(key: String): T? {
    return if (contains(key)) {
        get(key) as? T
    } else null
}

fun CompoundTag.getByteOrNull(key: String): Byte? =
    get(key)?.asByte()?.getOrNull()

fun CompoundTag.getShortOrNull(key: String): Short? =
    get(key)?.asShort()?.getOrNull()

fun CompoundTag.getIntOrNull(key: String): Int? =
    get(key)?.asInt()?.getOrNull()

fun CompoundTag.getLongOrNull(key: String): Long? =
    get(key)?.asLong()?.getOrNull()

fun CompoundTag.getFloatOrNull(key: String): Float? =
    get(key)?.asFloat()?.getOrNull()

fun CompoundTag.getDoubleOrNull(key: String): Double? =
    get(key)?.asDouble()?.getOrNull()

fun CompoundTag.getStringOrNull(key: String): String? =
    get(key)?.asString()?.getOrNull()

fun CompoundTag.getByteArrayOrNull(key: String): ByteArray? =
    get(key)?.asByteArray()?.getOrNull()

fun CompoundTag.getIntArrayOrNull(key: String): IntArray? =
    get(key)?.asIntArray()?.getOrNull()

fun CompoundTag.getLongArrayOrNull(key: String): LongArray? =
    get(key)?.asLongArray()?.getOrNull()

fun CompoundTag.getCompoundOrNull(key: String): CompoundTag? =
    get(key)?.asCompound()?.getOrNull()

fun CompoundTag.getListOrNull(key: String): ListTag? =
    get(key)?.asList()?.getOrNull()