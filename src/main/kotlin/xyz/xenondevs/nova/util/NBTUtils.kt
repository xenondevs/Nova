package xyz.xenondevs.nova.util

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.FloatTag
import net.minecraft.nbt.ListTag

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
    
    fun removeItemData(tag: CompoundTag): CompoundTag {
        tag.remove("Items")
        tag.remove("HandItems")
        tag.remove("ArmorItems")
        tag.remove("SaddleItem")
        
        return tag
    }
    
    fun getIdTag(tag: CompoundTag): CompoundTag {
        return CompoundTag().apply { putString("id", tag.getString("id")) }
    }
    
}