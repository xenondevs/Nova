package xyz.xenondevs.nova.util

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.RootCommandNode
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Consumer
import xyz.xenondevs.nova.util.ReflectionUtils.getCB
import xyz.xenondevs.nova.util.ReflectionUtils.getCBClass
import xyz.xenondevs.nova.util.ReflectionUtils.getConstructor
import xyz.xenondevs.nova.util.ReflectionUtils.getField
import xyz.xenondevs.nova.util.ReflectionUtils.getFieldOf
import xyz.xenondevs.nova.util.ReflectionUtils.getMethod
import xyz.xenondevs.nova.util.ReflectionUtils.getNMS
import xyz.xenondevs.nova.util.ReflectionUtils.getNMSClass

@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
object ReflectionRegistry {
    
    // NMS & CB paths
    val NMS_PACKAGE_PATH = getNMS()
    val CB_PACKAGE_PATH = getCB()
    
    // NMS classes
    val NMS_MINECRAFT_SERVER_CLASS = getNMSClass("MinecraftServer")
    val NMS_COMMAND_DISPATCHER_CLASS = getNMSClass("CommandDispatcher")
    val NMS_COMMAND_LISTENER_WRAPPER_CLASS = getNMSClass("CommandListenerWrapper")
    val NMS_ENTITY_CLASS = getNMSClass("Entity")
    val NMS_ENTITY_ARMOR_STAND_CLASS = getNMSClass("EntityArmorStand")
    val NMS_BLOCK_POSITION_CLASS = getNMSClass("BlockPosition")
    val NMS_PACKET_PLAY_OUT_BLOCK_BREAK_ANIMATION_CLASS = getNMSClass("PacketPlayOutBlockBreakAnimation")
    val NMS_ENTITY_PLAYER_CLASS = getNMSClass("EntityPlayer")
    val NMS_PLAYER_CONNECTION_CLASS = getNMSClass("PlayerConnection")
    val NMS_PACKET_CLASS = getNMSClass("Packet")
    val NMS_I_BLOCK_DATA_ClASS = getNMSClass("IBlockData")
    val NMS_BLOCK_CLASS = getNMSClass("Block")
    val NMS_SOUND_EFFECT_TYPE_CLASS = getNMSClass("SoundEffectType")
    val NMS_SOUND_EFFECT_CLASS = getNMSClass("SoundEffect")
    
    // CB classes
    val CB_CRAFT_SERVER_CLASS = getCBClass("CraftServer")
    val CB_CRAFT_ENTITY_CLASS = getCBClass("entity.CraftEntity")
    val CB_CRAFT_WORLD_CLASS = getCBClass("CraftWorld")
    val CB_CRAFT_ITEM_STACK_CLASS = getCBClass("inventory.CraftItemStack")
    val CB_CRAFT_SOUND_CLASS = getCBClass("CraftSound")
    val CB_CRAFT_BLOCK_CLASS = getCBClass("block.CraftBlock")
    
    // NMS constructors
    val NMS_BLOCK_POSITION_CONSTRUCTOR = getConstructor(NMS_BLOCK_POSITION_CLASS, false, Double::class.java, Double::class.java, Double::class.java)
    val NMS_PACKET_PLAY_OUT_BLOCK_BREAK_ANIMATION_CONSTRUCTOR = getConstructor(NMS_PACKET_PLAY_OUT_BLOCK_BREAK_ANIMATION_CLASS, false, Int::class.java, NMS_BLOCK_POSITION_CLASS, Int::class.java)
    
    // NMS methods
    val NMS_COMMAND_DISPATCHER_GET_BRIGADIER_COMMAND_DISPATCHER_METHOD = getMethod(NMS_COMMAND_DISPATCHER_CLASS, false, "a")
    val NMS_COMMAND_LISTENER_WRAPPER_GET_ENTITY_METHOD = getMethod(NMS_COMMAND_LISTENER_WRAPPER_CLASS, false, "getEntity")
    val NMS_ENTITY_GET_BUKKIT_ENTITY_METHOD = getMethod(NMS_ENTITY_CLASS, false, "getBukkitEntity")
    val NMS_PLAYER_CONNECTION_SEND_PACKET_METHOD = getMethod(NMS_PLAYER_CONNECTION_CLASS, false, "sendPacket", NMS_PACKET_CLASS)
    val NMS_BLOCK_GET_SOUND_EFFECT_TYPE_METHOD = getMethod(NMS_BLOCK_CLASS, false, "getStepSound", NMS_I_BLOCK_DATA_ClASS)
    
    // CB methods
    val CB_CRAFT_SERVER_GET_SERVER_METHOD = getMethod(CB_CRAFT_SERVER_CLASS, false, "getServer")
    val CB_CRAFT_SERVER_SYNC_COMMANDS_METHOD = getMethod(CB_CRAFT_SERVER_CLASS, true, "syncCommands")
    val CB_CRAFT_ENTITY_GET_HANDLE_METHOD = getMethod(CB_CRAFT_ENTITY_CLASS, false, "getHandle")
    val CB_CRAFT_WORLD_CREATE_ENTITY_METHOD = getMethod(CB_CRAFT_WORLD_CLASS, false, "createEntity", Location::class.java, Class::class.java)
    val CB_CRAFT_WORLD_ADD_ENTITY_METHOD = getMethod(CB_CRAFT_WORLD_CLASS, false, "addEntity", NMS_ENTITY_CLASS, SpawnReason::class.java, Consumer::class.java)
    val CB_CRAFT_ITEM_STACK_AS_NMS_COPY_METHOD = getMethod(CB_CRAFT_ITEM_STACK_CLASS, false, "asNMSCopy", ItemStack::class.java)
    val CB_CRAFT_SOUND_GET_BUKKIT_METHOD = getMethod(CB_CRAFT_SOUND_CLASS, false, "getBukkit", NMS_SOUND_EFFECT_CLASS)
    val CB_CRAFT_BLOCK_GET_NMS_BLOCK_METHOD = getMethod(CB_CRAFT_BLOCK_CLASS, true, "getNMSBlock")
    
    // NMS fields
    val NMS_MINECRAFT_SERVER_VANILLA_COMMAND_DISPATCHER_FIELD = getField(NMS_MINECRAFT_SERVER_CLASS, true, "vanillaCommandDispatcher")
    val NMS_ENTITY_ARMOR_STAND_ARMOR_ITEMS_FIELD = getField(NMS_ENTITY_ARMOR_STAND_CLASS, true, "armorItems")
    val NMS_ENTITY_PLAYER_PLAYER_CONNECTION_FIELD = getField(NMS_ENTITY_PLAYER_CLASS, false, "playerConnection")
    val NMS_SOUND_EFFECT_TYPE_BREAK_SOUND_FIELD = getFieldOf(NMS_SOUND_EFFECT_TYPE_CLASS, NMS_SOUND_EFFECT_CLASS, true, "breakSound", "X")
    val NMS_SOUND_EFFECT_TYPE_STEP_SOUND_FIELD = getFieldOf(NMS_SOUND_EFFECT_TYPE_CLASS, NMS_SOUND_EFFECT_CLASS, true, "stepSound", "Y")
    val NMS_SOUND_EFFECT_TYPE_PLACE_SOUND_FIELD = getFieldOf(NMS_SOUND_EFFECT_TYPE_CLASS, NMS_SOUND_EFFECT_CLASS, true, "placeSound", "Z")
    val NMS_SOUND_EFFECT_TYPE_HIT_SOUND_FIELD = getFieldOf(NMS_SOUND_EFFECT_TYPE_CLASS, NMS_SOUND_EFFECT_CLASS, true, "hitSound", "aa")
    val NMS_SOUND_EFFECT_TYPE_FALL_SOUND_FIELD = getFieldOf(NMS_SOUND_EFFECT_TYPE_CLASS, NMS_SOUND_EFFECT_CLASS, true, "fallSound", "bb")
    
    // other fields
    val COMMAND_DISPATCHER_ROOT_FIELD = getField(CommandDispatcher::class.java, true, "root")
    val COMMAND_NODE_CHILDREN_FIELD = getField(CommandNode::class.java, true, "children")
    val COMMAND_NODE_LITERALS_FIELD = getField(CommandNode::class.java, true, "literals")
    val COMMAND_NODE_ARGUMENTS_FIELD = getField(CommandNode::class.java, true, "arguments")
    
    // objects
    val NMS_DEDICATED_SERVER = CB_CRAFT_SERVER_GET_SERVER_METHOD.invoke(Bukkit.getServer())!!
    val NMS_COMMAND_DISPATCHER = NMS_MINECRAFT_SERVER_VANILLA_COMMAND_DISPATCHER_FIELD.get(NMS_DEDICATED_SERVER)!!
    val COMMAND_DISPATCHER = NMS_COMMAND_DISPATCHER_GET_BRIGADIER_COMMAND_DISPATCHER_METHOD.invoke(NMS_COMMAND_DISPATCHER)!! as CommandDispatcher<Any>
    val COMMAND_DISPATCHER_ROOT_NODE = COMMAND_DISPATCHER_ROOT_FIELD.get(COMMAND_DISPATCHER)!! as RootCommandNode<Any>
    
}
