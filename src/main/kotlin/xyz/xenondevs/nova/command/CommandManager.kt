package xyz.xenondevs.nova.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_17_R1.CraftServer
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.command.impl.NovaCommand
import xyz.xenondevs.nova.util.ReflectionUtils
import xyz.xenondevs.nova.util.runTask

val COMMAND_DISPATCHER: CommandDispatcher<CommandSourceStack> = (Bukkit.getServer() as CraftServer).server.vanillaCommandDispatcher.dispatcher

object CommandManager {
    
    private val registeredCommands = ArrayList<String>()
    
    fun init() {
        registerCommands()
        NOVA.disableHandlers += this::unregisterCommands
    }
    
    private fun registerCommands() {
        registerCommand(NovaCommand)
        ReflectionUtils.syncCommands()
        runTask { registeredCommands.forEach { ReflectionUtils.getCommand(it).permission = null } }
    }
    
    private fun registerCommand(command: PlayerCommand) {
        registeredCommands += command.name
        COMMAND_DISPATCHER.register(command.builder)
    }
    
    private fun unregisterCommands() {
        registeredCommands.forEach { ReflectionUtils.unregisterCommand(it) }
    }
    
}