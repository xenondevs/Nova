package xyz.xenondevs.nova.patch.impl.misc

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent
import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent
import io.papermc.paper.plugin.manager.PaperPluginManagerImpl
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityCombustByEntityEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.player.PlayerEvent
import org.objectweb.asm.tree.LabelNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.nova.patch.MethodTransformer
import xyz.xenondevs.nova.util.FakePlayer
import xyz.xenondevs.nova.util.nmsEntity
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
    
    @Suppress("UNNECESSARY_SAFE_CALL", "removal", "DEPRECATION") // some plugins might set the player to null
    @JvmStatic
    fun shouldPreventEvent(event: Event): Boolean {
        // prevent all events of fake players
        if (event is PlayerEvent) {
            val player = event.player?.serverPlayer
            if (player is FakePlayer && !player.hasEvents)
                return false
        }
        if (event is EntityEvent) {
            val entity = event.entity?.nmsEntity
            if (entity is FakePlayer && !entity.hasEvents)
                return false
        }
        if (event is EntityDamageByEntityEvent) {
            val damager = event.damager?.nmsEntity
            if (damager is FakePlayer && !damager.hasEvents)
                return false
        }
        if (event is EntityKnockbackByEntityEvent) {
            val pushedBy = event.pushedBy?.nmsEntity
            if (pushedBy is FakePlayer && !pushedBy.hasEvents)
                return false
        }
        if (event is org.bukkit.event.entity.EntityKnockbackByEntityEvent) {
            val source = event.sourceEntity?.nmsEntity
            if (source is FakePlayer && !source.hasEvents)
                return false
        }
        if (event is EntityCombustByEntityEvent) {
            val combuster = event.combuster?.nmsEntity
            if (combuster is FakePlayer && !combuster.hasEvents)
                return false
        }
        if (event is EntityPushedByEntityAttackEvent) {
            val pushedBy = event.pushedBy?.nmsEntity
            if (pushedBy is FakePlayer && !pushedBy.hasEvents)
                return false
        }
        if (event is HangingBreakByEntityEvent) {
            val remover = event.remover?.nmsEntity
            if (remover is FakePlayer && !remover.hasEvents)
                return false
        }
        
        // only sync events are dropped in event prevention scope
        return !event.isAsynchronous && dropAll
    }
    
}