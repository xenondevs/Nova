package xyz.xenondevs.nova.util.reflection

import com.mojang.brigadier.tree.CommandNode
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.network.ServerConnectionListener
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.crafting.RecipeManager
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.event.inventory.PrepareItemCraftEvent
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getCB
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getCBClass
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getClass
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getConstructor
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getField
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getMethod
import java.util.*
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KProperty1

@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
object ReflectionRegistry {
    
    // Paths
    val CB_PACKAGE_PATH = getCB()
    
    // Classes
    val CB_CRAFT_META_ITEM_CLASS = getCBClass("inventory.CraftMetaItem")
    val SECTION_PATH_DATA_CLASS = getClass("org.bukkit.configuration.SectionPathData")
    
    // Constructors
    val ENUM_MAP_CONSTRUCTOR = getConstructor(EnumMap::class.java, false, Class::class.java)
    val MEMORY_SECTION_CONSTRUCTOR = getConstructor(MemorySection::class.java, true, ConfigurationSection::class.java, String::class.java)
    val SECTION_PATH_DATA_CONSTRUCTOR = getConstructor(SECTION_PATH_DATA_CLASS, true, Any::class.java)
    
    // Methods
    val CB_CRAFT_META_APPLY_TO_METHOD = getMethod(CB_CRAFT_META_ITEM_CLASS, true, "applyToItem", CompoundTag::class.java)
    val K_PROPERTY_1_GET_DELEGATE_METHOD = getMethod(KProperty1::class.java, false, "getDelegate", Any::class.java)
    
    // Fields
    val CRAFT_META_ITEM_UNHANDLED_TAGS_FIELD = getField(CB_CRAFT_META_ITEM_CLASS, true, "unhandledTags")
    val ARMOR_STAND_ARMOR_ITEMS_FIELD = getField(ArmorStand::class.java, true, "SRF(net.minecraft.world.entity.decoration.ArmorStand armorItems)")
    val SERVER_CONNECTION_LISTENER_CHANNELS_FIELD = getField(ServerConnectionListener::class.java, true, "SRF(net.minecraft.server.network.ServerConnectionListener channels)")
    val RECIPE_MANAGER_BY_NAME_FIELD = getField(RecipeManager::class.java, true, "SRF(net.minecraft.world.item.crafting.RecipeManager byName)")
    val COMMAND_NODE_CHILDREN_FIELD = getField(CommandNode::class.java, true, "children")
    val COMMAND_NODE_LITERALS_FIELD = getField(CommandNode::class.java, true, "literals")
    val COMMAND_NODE_ARGUMENTS_FIELD = getField(CommandNode::class.java, true, "arguments")
    val CALLABLE_REFERENCE_RECEIVER_FIELD = getField(CallableReference::class.java, true, "receiver")
    val PREPARE_ITEM_CRAFT_EVENT_MATRIX_FIELD = getField(PrepareItemCraftEvent::class.java, true, "matrix")
    val MEMORY_SECTION_MAP_FIELD = getField(MemorySection::class.java, true, "map")
    
}
