package xyz.xenondevs.nova.transformer.patch.event

import org.bukkit.event.Event
import org.bukkit.event.player.PlayerEvent
import org.objectweb.asm.tree.LabelNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.FakePlayer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.serverPlayer

/**
 * Patches the fireEvent method in SimplePluginManager to prevent fake players from triggering events.
 */
internal object FakePlayerEventPreventionPatch : MethodTransformer(ReflectionRegistry.SIMPLE_PLUGIN_MANAGER_FIRE_EVENT_METHOD) {
    
    override fun transform() {
        val instructions = methodNode.instructions
        instructions.insertBefore(instructions.first, buildInsnList {
            aLoad(1)
            invokeStatic(::shouldPreventEvent)
            val label = LabelNode()
            ifeq(label) // if not shouldFireEvent, jump to label node
            addLabel()
            _return()
            add(label) // label after which the function continues normally
        })
    }
    
    @JvmStatic
    fun shouldPreventEvent(event: Event): Boolean {
        if (event is PlayerEvent) {
            val player = event.player.serverPlayer
            return player is FakePlayer && !player.hasEvents
        }
        return false
    }
    
}