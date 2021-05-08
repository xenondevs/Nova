package xyz.xenondevs.nova.attachment

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.util.runTaskTimer
import java.util.*

object AttachmentManager : Listener {
    
    private val attachments = HashMap<UUID, MutableList<Attachment>>()
    private var tick = 0
    
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        PermanentStorage.retrieveOrNull<Map<UUID, List<Attachment>>>("attachments")
        NOVA.disableHandlers.add {
            PermanentStorage.store("attachments", attachments)
            attachments.values.flatten().forEach(Attachment::despawn)
        }
        
        runTaskTimer(0, 1) { attachments.values.flatten().forEach { it.handleTick(tick++) } }
    }
    
    fun registerAttachment(attachment: Attachment) {
        val uuid = attachment.playerUUID
        val attachmentList = attachments[uuid] ?: ArrayList<Attachment>().also { attachments[uuid] = it }
        if (attachmentList.any { it.key == attachment.key })
            throw IllegalStateException("Tried to register an attachment under a key which is already occupied.")
        attachmentList += attachment
    }
    
    fun unregisterAttachment(attachment: Attachment) {
        val uuid = attachment.playerUUID
        val attachmentList = attachments[uuid]
        if (attachmentList != null) {
            attachmentList.remove(attachment)
            if (attachmentList.isEmpty()) attachments.remove(uuid)
        }
    }
    
    fun getAttachments(uuid: UUID): List<Attachment> {
        return attachments[uuid] ?: emptyList()
    }
    
    fun getAttachment(uuid: UUID, key: String): Attachment? {
        return attachments[uuid]?.firstOrNull { it.key == key }
    }
    
    @EventHandler
    fun handlePlayerJoin(event: PlayerJoinEvent) {
        getAttachments(event.player.uniqueId).forEach(Attachment::spawn)
    }
    
    @EventHandler
    fun handlePlayerQuit(event: PlayerQuitEvent) {
        getAttachments(event.player.uniqueId).forEach(Attachment::despawn)
    }
    
}