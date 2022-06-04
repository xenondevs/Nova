package xyz.xenondevs.nova.util.data

import net.minecraft.nbt.*
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
    
}

@Suppress("UNCHECKED_CAST")
fun <T : Tag> CompoundTag.getOrPut(key: String, create: () -> T): T {
    if (contains(key))
        return get(key) as T
    
    val value = create()
    put(key, value)
    
    return value
}

@Suppress("UNCHECKED_CAST")
fun <T : Tag> CompoundTag.getOrNull(key: String): T? {
    return if (contains(key)) {
        get(key) as? T
    } else null
}