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
import net.minecraft.nbt.NumericTag
import net.minecraft.nbt.ShortTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.nbt.TagType
import net.minecraft.world.item.ItemStack
import java.util.stream.Stream

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
    
    fun removeItemData(tag: CompoundTag): CompoundTag {
        tag.remove("Items")
        tag.remove("HandItems")
        tag.remove("ArmorItems")
        tag.remove("SaddleItem")
        tag.remove("Inventory")
        
        return tag
    }
    
    fun convertListToStream(tag: ListTag): Stream<ItemStack> {
        return tag.stream().map { ItemStack.of(it as CompoundTag) }
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

fun CompoundTag.getByteOrNull(key: String): Byte? {
    if (contains(key, NBTUtils.TAG_BYTE))
        return (get(key) as? NumericTag)?.asByte
    
    return null
}

fun CompoundTag.getShortOrNull(key: String): Short? {
    if (contains(key, NBTUtils.TAG_SHORT))
        return (get(key) as? NumericTag)?.asShort
    
    return null
}

fun CompoundTag.getIntOrNull(key: String): Int? {
    if (contains(key, NBTUtils.TAG_INT))
        return (get(key) as? NumericTag)?.asInt
    
    return null
}

fun CompoundTag.getLongOrNull(key: String): Long? {
    if (contains(key, NBTUtils.TAG_LONG))
        return (get(key) as? NumericTag)?.asLong
    
    return null
}

fun CompoundTag.getFloatOrNull(key: String): Float? {
    if (contains(key, NBTUtils.TAG_FLOAT))
        return (get(key) as? NumericTag)?.asFloat
    
    return null
}

fun CompoundTag.getDoubleOrNull(key: String): Double? {
    if (contains(key, NBTUtils.TAG_DOUBLE))
        return (get(key) as? NumericTag)?.asDouble
    
    return null
}

fun CompoundTag.getStringOrNull(key: String): String? {
    if (contains(key, NBTUtils.TAG_STRING))
        return (get(key) as? StringTag)?.asString
    
    return null
}

fun CompoundTag.getByteArrayOrNull(key: String): ByteArray? {
    if (contains(key, NBTUtils.TAG_BYTE_ARRAY))
        return (get(key) as? ByteArrayTag)?.asByteArray
    
    return null
}

fun CompoundTag.getIntArrayOrNull(key: String): IntArray? {
    if (contains(key, NBTUtils.TAG_INT_ARRAY))
        return (get(key) as? IntArrayTag)?.asIntArray
    
    return null
}

fun CompoundTag.getLongArrayOrNull(key: String): LongArray? {
    if (contains(key, NBTUtils.TAG_LONG_ARRAY))
        return (get(key) as? LongArrayTag)?.asLongArray
    
    return null
}

fun CompoundTag.getCompoundOrNull(key: String): CompoundTag? {
    if (contains(key, NBTUtils.TAG_COMPOUND))
        return get(key) as? CompoundTag
    
    return null
}

fun CompoundTag.getListOrNull(key: String): ListTag? {
    if (contains(key, NBTUtils.TAG_LIST))
        return get(key) as? ListTag
    
    return null
}