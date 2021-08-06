package xyz.xenondevs.nova.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_17_R1.CraftServer
import org.bukkit.craftbukkit.v1_17_R1.command.VanillaCommandWrapper
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.command.impl.NovaCommand
import xyz.xenondevs.nova.util.ReflectionUtils

val COMMAND_DISPATCHER: CommandDispatcher<CommandSourceStack> = (Bukkit.getServer() as CraftServer).server.vanillaCommandDispatcher.dispatcher

object CommandManager {
    
    private val registeredCommands = ArrayList<String>()
    
    fun init() {
        registerCommands()
        NOVA.disableHandlers += this::unregisterCommands
    }
    
    private fun registerCommands() {
        registerCommand(NovaCommand)
    }
    
    fun registerCommand(command: PlayerCommand) {
        registeredCommands += command.name
        COMMAND_DISPATCHER.register(command.builder)
        
        val craftServer = Bukkit.getServer() as CraftServer
        
        val vanillaCommandWrapper = VanillaCommandWrapper(craftServer.server.vanillaCommandDispatcher, command.builder.build())
        vanillaCommandWrapper.permission = null
        craftServer.commandMap.register("nova", vanillaCommandWrapper)
        
        craftServer.syncCommands()
    }
    
    private fun unregisterCommands() {
        registeredCommands.forEach { ReflectionUtils.unregisterCommand(it) }
    }
    
}