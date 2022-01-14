package xyz.xenondevs.nova.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_18_R1.CraftServer
import org.bukkit.craftbukkit.v1_18_R1.command.VanillaCommandWrapper
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.command.impl.NovaCommand
import xyz.xenondevs.nova.command.impl.NovaModelDataCommand
import xyz.xenondevs.nova.command.impl.UninstallCommand
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry

val COMMAND_DISPATCHER: CommandDispatcher<CommandSourceStack> = (Bukkit.getServer() as CraftServer).server.vanillaCommandDispatcher.dispatcher

object CommandManager : Initializable() {
    
    private val registeredCommands = ArrayList<String>()
    
    override val inMainThread = true
    override val dependsOn = CustomItemServiceManager // TODO: depend on RecipeManager
    
    override fun init() {
        LOGGER.info("Registering Commands")
        registerCommands()
        NOVA.disableHandlers += this::unregisterCommands
    }
    
    private fun registerCommands() {
        registerCommand(NovaCommand)
        registerCommand(UninstallCommand)
        registerCommand(NovaModelDataCommand)
    }
    
    fun registerCommand(command: Command) {
        registeredCommands += command.name
        COMMAND_DISPATCHER.register(command.builder)
        
        val craftServer = Bukkit.getServer() as CraftServer
        
        val vanillaCommandWrapper = VanillaCommandWrapper(craftServer.server.vanillaCommandDispatcher, command.builder.build())
        vanillaCommandWrapper.permission = null
        craftServer.commandMap.register("nova", vanillaCommandWrapper)
        
        craftServer.syncCommands()
    }
    
    private fun unregisterCommands() {
        registeredCommands.forEach { unregisterCommand(it) }
    }
    
    private fun unregisterCommand(name: String) {
        val rootNode = (Bukkit.getServer() as CraftServer).server.vanillaCommandDispatcher.dispatcher.root
        
        val children = ReflectionRegistry.COMMAND_NODE_CHILDREN_FIELD.get(rootNode) as MutableMap<*, *>
        val literals = ReflectionRegistry.COMMAND_NODE_LITERALS_FIELD.get(rootNode) as MutableMap<*, *>
        val arguments = ReflectionRegistry.COMMAND_NODE_ARGUMENTS_FIELD.get(rootNode) as MutableMap<*, *>
        
        children.remove(name)
        literals.remove(name)
        arguments.remove(name)
    }
    
}