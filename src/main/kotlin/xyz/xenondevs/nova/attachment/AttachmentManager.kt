package xyz.xenondevs.nova.attachment

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.util.runTaskTimer

object AttachmentManager : Listener {
    
    private val attachments = ArrayList<Attachment>()
    
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        println(PermanentStorage.retrieveOrNull<ArrayList<Attachment>>("attachments")?.count())
        println(attachments.size)
        NOVA.disableHandlers.add {
            PermanentStorage.store("attachments", attachments)
            attachments.forEach(Attachment::despawn)
        }
        
        runTaskTimer(0, 1) { attachments.forEach(Attachment::handleTick) }
    }
    
    fun registerAttachment(attachment: Attachment) {
        attachments.add(attachment)
    }
    
    fun unregisterAttachment(attachment: Attachment) {
        attachments.remove(attachment)
    }
    
    fun findAttachment(player: Player): Attachment? {
        return attachments.firstOrNull { it.uuid == player.uniqueId }
    }
    
    @EventHandler
    fun handlePlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val uuid = player.uniqueId
        attachments.filter { it.uuid == uuid }.forEach { it.spawn() }
    }
    
    @EventHandler
    fun handlePlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val uuid = player.uniqueId
        attachments.filter { it.uuid == uuid }.forEach { it.despawn() }
    }
    
    @EventHandler
    fun handleWorldChange(event: PlayerChangedWorldEvent) {
        println(event)
    }
    
    //TODO: world change?
    
}