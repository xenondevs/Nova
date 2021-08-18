package xyz.xenondevs.nova.util

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.Packet
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.ReflectionRegistry.CB_CRAFT_COMMAND_MAP_GET_COMMAND_METHOD
import xyz.xenondevs.nova.util.ReflectionRegistry.CB_CRAFT_ENTITY_GET_HANDLE_METHOD
import xyz.xenondevs.nova.util.ReflectionRegistry.CB_CRAFT_ITEM_STACK_AS_NMS_COPY_METHOD
import xyz.xenondevs.nova.util.ReflectionRegistry.CB_CRAFT_SERVER_GET_COMMAND_MAP_METHOD
import xyz.xenondevs.nova.util.ReflectionRegistry.CB_CRAFT_SERVER_SYNC_COMMANDS_METHOD
import xyz.xenondevs.nova.util.ReflectionRegistry.CB_CRAFT_WORLD_ADD_ENTITY_METHOD
import xyz.xenondevs.nova.util.ReflectionRegistry.CB_CRAFT_WORLD_CREATE_ENTITY_METHOD
import xyz.xenondevs.nova.util.ReflectionRegistry.CB_CRAFT_WORLD_GET_HANDLE_METHOD
import xyz.xenondevs.nova.util.ReflectionRegistry.CB_PACKAGE_PATH
import xyz.xenondevs.nova.util.ReflectionRegistry.COMMAND_DISPATCHER_ROOT_NODE
import xyz.xenondevs.nova.util.ReflectionRegistry.COMMAND_NODE_ARGUMENTS_FIELD
import xyz.xenondevs.nova.util.ReflectionRegistry.COMMAND_NODE_CHILDREN_FIELD
import xyz.xenondevs.nova.util.ReflectionRegistry.COMMAND_NODE_LITERALS_FIELD
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import net.minecraft.world.entity.Entity as NMSEntity
import net.minecraft.world.item.ItemStack as NMSItemStack

@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
object ReflectionUtils {
    
    fun getCB(): String {
        val path = Bukkit.getServer().javaClass.getPackage().name
        val version = path.substring(path.lastIndexOf(".") + 1)
        return "org.bukkit.craftbukkit.$version."
    }
    
    fun getVersion(): Int {
        val version = Bukkit.getVersion().substringAfter("MC: ").substringBefore(')')
        return version.split(".")[1].toInt()
    }
    
    fun getCB(name: String): String {
        return CB_PACKAGE_PATH + name
    }
    
    fun getCBClass(name: String): Class<*> {
        return Class.forName(getCB(name))
    }
    
    fun getMethod(clazz: Class<*>, declared: Boolean, methodName: String, vararg args: Class<*>): Method {
        val method = if (declared) clazz.getDeclaredMethod(methodName, *args) else clazz.getMethod(methodName, *args)
        if (declared) method.isAccessible = true
        return method
    }
    
    fun getConstructor(clazz: Class<*>, declared: Boolean, vararg args: Class<*>): Constructor<*> {
        return if (declared) clazz.getDeclaredConstructor(*args) else clazz.getConstructor(*args)
    }
    
    fun getField(clazz: Class<*>, declared: Boolean, name: String): Field {
        val field = if (declared) clazz.getDeclaredField(name) else clazz.getField(name)
        if (declared) field.isAccessible = true
        return field
    }
    
    fun syncCommands() {
        CB_CRAFT_SERVER_SYNC_COMMANDS_METHOD.invoke(Bukkit.getServer())
    }
    
    fun getCommand(name: String): Command {
        val commandMap = CB_CRAFT_SERVER_GET_COMMAND_MAP_METHOD.invoke(Bukkit.getServer())
        return CB_CRAFT_COMMAND_MAP_GET_COMMAND_METHOD.invoke(commandMap, name) as Command
    }
    
    fun unregisterCommand(name: String) {
        val children = COMMAND_NODE_CHILDREN_FIELD.get(COMMAND_DISPATCHER_ROOT_NODE) as MutableMap<String, *>
        val literals = COMMAND_NODE_LITERALS_FIELD.get(COMMAND_DISPATCHER_ROOT_NODE) as MutableMap<String, *>
        val arguments = COMMAND_NODE_ARGUMENTS_FIELD.get(COMMAND_DISPATCHER_ROOT_NODE) as MutableMap<String, *>
        
        children.remove(name)
        literals.remove(name)
        arguments.remove(name)
    }
    
    val Entity.nmsEntity: NMSEntity
        get() = getNMSEntity(this)
    
    val Player.nmsEntity: ServerPlayer
        get() = getNMSEntity(this) as ServerPlayer
    
    val ItemStack.nmsStack: net.minecraft.world.item.ItemStack
        get() = CB_CRAFT_ITEM_STACK_AS_NMS_COPY_METHOD.invoke(null, this) as NMSItemStack
    
    val Location.blockPos: BlockPos
        get() = BlockPos(blockX, blockY, blockZ)
    
    val World.nmsWorld: ServerLevel
        get() = CB_CRAFT_WORLD_GET_HANDLE_METHOD.invoke(this) as ServerLevel
    
    fun Player.send(packet: Packet<*>) = nmsEntity.connection.send(packet)
    
    fun getNMSEntity(entity: Entity): NMSEntity {
        return CB_CRAFT_ENTITY_GET_HANDLE_METHOD.invoke(entity) as NMSEntity
    }
    
    fun createNMSEntity(world: World, location: Location, entityType: EntityType): Any {
        return CB_CRAFT_WORLD_CREATE_ENTITY_METHOD.invoke(world, location, entityType.entityClass)
    }
    
    fun addNMSEntityToWorld(world: World, entity: Any): Entity {
        return CB_CRAFT_WORLD_ADD_ENTITY_METHOD.invoke(world, entity, CreatureSpawnEvent.SpawnReason.CUSTOM) as Entity
    }
    
}