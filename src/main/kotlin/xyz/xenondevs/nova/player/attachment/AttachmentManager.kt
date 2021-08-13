package xyz.xenondevs.nova.player.attachment

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.serialization.persistentdata.JsonElementDataType
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.fromJson
import xyz.xenondevs.nova.util.runTaskTimer
import java.util.*
import kotlin.collections.set

private val ATTACHMENTS_KEY = NamespacedKey(NOVA, "attachments")

object AttachmentManager : Listener {
    
    private val attachments = HashMap<UUID, MutableList<Attachment>>()
    private var tick = 0
    
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        Bukkit.getOnlinePlayers().forEach { loadAttachments(it) }
        NOVA.disableHandlers.add { Bukkit.getOnlinePlayers().forEach { saveAndRemoveAttachments(it) } }
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
        loadAttachments(event.player)
    }
    
    @EventHandler
    fun handlePlayerQuit(event: PlayerQuitEvent) {
        saveAndRemoveAttachments(event.player)
    }
    
    private fun loadAttachments(player: Player) {
        val dataContainer = player.persistentDataContainer
        val jsonElement = dataContainer.get(ATTACHMENTS_KEY, JsonElementDataType)
        if (jsonElement != null)
            GSON.fromJson<List<Attachment>>(jsonElement)
    }
    
    private fun saveAndRemoveAttachments(player: Player) {
        val uuid = player.uniqueId
        
        saveAttachments(player)
        getAttachments(uuid).forEach(Attachment::despawn)
        attachments.remove(uuid)
    }
    
    private fun saveAttachments(player: Player) {
        val dataContainer = player.persistentDataContainer
        dataContainer.set(ATTACHMENTS_KEY, JsonElementDataType, GSON.toJsonTree(attachments[player.uniqueId]))
    }
    
}