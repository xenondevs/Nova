package xyz.xenondevs.nova.util.data

import net.minecraft.nbt.*
import net.minecraft.world.item.ItemStack
import java.util.stream.Stream

object NBTUtils {
    
    var TAG_END = 0
    var TAG_BYTE = 1
    var TAG_SHORT = 2
    var TAG_INT = 3
    var TAG_LONG = 4
    var TAG_FLOAT = 5
    var TAG_DOUBLE = 6
    var TAG_BYTE_ARRAY = 7
    var TAG_STRING = 8
    var TAG_LIST = 9
    var TAG_COMPOUND = 10
    var TAG_INT_ARRAY = 11
    var TAG_LONG_ARRAY = 12
    var TAG_ANY_NUMERIC = 99
    
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
    
    fun convertListToStream(tag: ListTag) : Stream<ItemStack> {
        return tag.stream().map { ItemStack.of(it as CompoundTag) }
    }
    
}