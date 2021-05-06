package xyz.xenondevs.nova.attachment

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.util.runTaskTimer

object AttachmentManager : Listener {
    
    private val attachments = ArrayList<Attachment>()
    private var tick = 0
    
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        PermanentStorage.retrieveOrNull<ArrayList<Attachment>>("attachments")
        NOVA.disableHandlers.add {
            PermanentStorage.store("attachments", attachments)
            attachments.forEach(Attachment::despawn)
        }
        
        runTaskTimer(0, 1) { attachments.forEach { it.handleTick(tick++) } }
    }
    
    fun registerAttachment(attachment: Attachment) {
        attachments.add(attachment)
    }
    
    fun unregisterAttachment(attachment: Attachment) {
        attachments.remove(attachment)
    }
    
    fun getAttachments(player: Player): List<Attachment> {
        val uuid = player.uniqueId
        return attachments.filter { it.uuid == uuid }
    }
    
    @EventHandler
    fun handlePlayerJoin(event: PlayerJoinEvent) {
        getAttachments(event.player).forEach(Attachment::spawn)
    }
    
    @EventHandler
    fun handlePlayerQuit(event: PlayerQuitEvent) {
        getAttachments(event.player).forEach(Attachment::despawn)
    }
    
}