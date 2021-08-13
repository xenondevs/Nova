package xyz.xenondevs.nova.util.reflection

import com.mojang.brigadier.tree.CommandNode
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.decoration.ArmorStand
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getCB
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getCBClass
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getField
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getMethod

@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
object ReflectionRegistry {
    
    // Path and version
    val CB_PACKAGE_PATH = getCB()
    
    // CB classes
    val CB_CRAFT_META_ITEM_CLASS = getCBClass("inventory.CraftMetaItem")
    
    // CB methods
    val CB_CRAFT_META_APPLY_TO_METHOD = getMethod(CB_CRAFT_META_ITEM_CLASS, true, "applyToItem", CompoundTag::class.java)
    
    // NMS fields
    val ARMOR_STAND_ARMOR_ITEMS_FIELD = getField(ArmorStand::class.java, true, "cd")
    
    // other fields
    val COMMAND_NODE_CHILDREN_FIELD = getField(CommandNode::class.java, true, "children")
    val COMMAND_NODE_LITERALS_FIELD = getField(CommandNode::class.java, true, "literals")
    val COMMAND_NODE_ARGUMENTS_FIELD = getField(CommandNode::class.java, true, "arguments")
    
}
