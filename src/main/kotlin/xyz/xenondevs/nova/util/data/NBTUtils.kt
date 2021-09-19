package xyz.xenondevs.nova.util.data

import net.minecraft.nbt.*

object NBTUtils {
    
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
    
}