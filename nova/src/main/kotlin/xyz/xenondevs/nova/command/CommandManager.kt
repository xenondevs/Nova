package xyz.xenondevs.nova.command

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import xyz.xenondevs.nova.LIFECYCLE_MANAGER
import xyz.xenondevs.nova.command.impl.NovaCommand
import xyz.xenondevs.nova.command.impl.NovaRecipeCommand
import xyz.xenondevs.nova.command.impl.NovaUsageCommand
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage

@InternalInit(stage = InternalInitStage.PRE_WORLD)
internal object CommandManager {
    
    @Suppress("UnstableApiUsage")
    @InitFun
    private fun registerCommands() {
        LIFECYCLE_MANAGER.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()
            commands.register(NovaCommand.node)
            commands.register(NovaRecipeCommand.node)
            commands.register(NovaUsageCommand.node)
        }
    }
    
}