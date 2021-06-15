package xyz.xenondevs.nova.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_17_R1.CraftServer
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
        registerCommand(::NovaCommand, "nova", "nova.nova")
        ReflectionUtils.syncCommands()
    }
    
    private fun registerCommand(createCommand: (String, String) -> PlayerCommand, name: String, permission: String) {
        registeredCommands += name
        val command = createCommand(name, permission)
        COMMAND_DISPATCHER.register(command.builder)
    }
    
    private fun unregisterCommands() {
        registeredCommands.forEach { ReflectionUtils.unregisterCommand(it) }
    }
    
}