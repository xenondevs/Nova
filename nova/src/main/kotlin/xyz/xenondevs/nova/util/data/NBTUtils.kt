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
import net.minecraft.world.item.ItemStack
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.nova.data.serialization.cbf.CBFCompoundTag
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
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
        
        return tag
    }
    
    fun convertListToStream(tag: ListTag): Stream<ItemStack> {
        return tag.stream().map { ItemStack.of(it as CompoundTag) }
    }
    
    internal fun reserializeCBFCompoundTag(cbfTag: Any): CBFCompoundTag {
        val toByteArrayMethod = cbfTag::class.java.getMethod("compoundToByteArray")
        val serializedCBFCompound = toByteArrayMethod.invoke(cbfTag) as ByteArray
        return CBFCompoundTag(CBF.read<NamespacedCompound>(serializedCBFCompound)!!)
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

internal fun CompoundTag.getOrPutCBFCompoundTag(key: String, defaultValue: () -> CBFCompoundTag): CBFCompoundTag {
    var value = getCBFCompoundTag(key)
    if (value != null)
        return value
    
    value = defaultValue()
    put(key, value)
    
    return value
}

internal fun CompoundTag.getCBFCompoundTag(key: String): CBFCompoundTag? {
    val tag = get(key) ?: return null
    if (tag !is CBFCompoundTag) {
        val newTag = NBTUtils.reserializeCBFCompoundTag(tag)
        put(key, newTag)
        return newTag
    }
    
    return tag
}

internal fun MutableMap<String, Tag>.getCBFCompoundTag(key: String): CBFCompoundTag? {
    val tag = get(key) ?: return null
    if (tag !is CBFCompoundTag) {
        val newTag = NBTUtils.reserializeCBFCompoundTag(tag)
        put(key, newTag)
        return newTag
    }
    
    return tag
}

internal fun MutableMap<String, Tag>.getOrPutCBFCompoundTag(key: String, defaultValue: () -> CBFCompoundTag): CBFCompoundTag {
    var value = getCBFCompoundTag(key)
    if (value != null)
        return value
    
    value = defaultValue()
    put(key, value)
    
    return value
}