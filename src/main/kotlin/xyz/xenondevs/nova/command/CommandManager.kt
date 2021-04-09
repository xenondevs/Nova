package xyz.xenondevs.nova.command

import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.command.impl.NovaCommand
import xyz.xenondevs.nova.util.ReflectionUtils

object CommandManager {
    
    private val registeredCommands = ArrayList<String>()
    
    fun init() {
        registerCommands()
        NOVA.disableHandlers += this::unregisterCommands
    }
    
    private fun registerCommands() {
        registerCommand(::NovaCommand, "nova", "nova.nova")
    }
    
    private fun registerCommand(createCommand: (String, String) -> PlayerCommand, name: String, permission: String) {
        registeredCommands += name
        val command = createCommand(name, permission)
        ReflectionUtils.registerCommand(command.builder)
        ReflectionUtils.syncCommands()
    }
    
    private fun unregisterCommands() {
        registeredCommands.forEach { ReflectionUtils.unregisterCommand(it) }
    }
    
}