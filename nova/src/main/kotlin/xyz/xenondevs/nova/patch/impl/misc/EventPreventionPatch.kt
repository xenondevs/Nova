package xyz.xenondevs.nova.patch.impl.misc

import io.papermc.paper.plugin.manager.PaperPluginManagerImpl
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerEvent
import org.objectweb.asm.tree.LabelNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.nova.patch.MethodTransformer
import xyz.xenondevs.nova.util.FakePlayer
import xyz.xenondevs.nova.util.serverPlayer

/**
 * Patches the callEvent method in SimplePluginManager to prevent fake players from triggering events.
 */
@PublishedApi
internal object EventPreventionPatch : MethodTransformer(PaperPluginManagerImpl::callEvent) {
    
    var dropAll = false
    
    override fun transform() {
        val instructions = methodNode.instructions
        instructions.insert(buildInsnList {
            val continueLabel = instructions.first as LabelNode
            
            addLabel()
            aLoad(1)
            invokeStatic(EventPreventionPatch::shouldPreventEvent)
            ifeq(continueLabel)
            addLabel()
            _return()
        })
    }
    
    @Suppress("UNNECESSARY_SAFE_CALL") // some plugins might set the player to null
    @JvmStatic
    fun shouldPreventEvent(event: Event): Boolean {
        // prevent all events of fake players
        if (event is PlayerEvent) {
            val player = event.player?.serverPlayer
            return player is FakePlayer && !player.hasEvents
        }
        
        // only sync events are dropped in event prevention scope
        return !event.isAsynchronous && dropAll
    }
    
}