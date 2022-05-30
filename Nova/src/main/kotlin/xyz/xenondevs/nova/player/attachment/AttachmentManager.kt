package xyz.xenondevs.nova.player.attachment

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.serialization.cbf.Compound
import xyz.xenondevs.nova.data.serialization.persistentdata.CompoundDataType
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.util.runTaskTimer
import java.util.*
import kotlin.collections.set

private val ATTACHMENTS_KEY = NamespacedKey(NOVA, "attachmentsCBF")

object AttachmentManager : Initializable(), Listener {
    
    private val attachments = HashMap<UUID, MutableList<Attachment>>()
    private var tick = 0
    
    override val inMainThread = true
    override val dependsOn = emptySet<Initializable>()
    
    override fun init() {
        LOGGER.info("Initializing AttachmentManager")
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        Bukkit.getOnlinePlayers().forEach { loadAttachments(it) }
        runTaskTimer(0, 1) { attachments.values.flatten().forEach { it.handleTick(tick++) } }
    }
    
    override fun disable() {
        LOGGER.info("Saving attachments")
        Bukkit.getOnlinePlayers().forEach { saveAndRemoveAttachments(it) }
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
        player.persistentDataContainer
            .get(ATTACHMENTS_KEY, CompoundDataType)
            ?.get<List<Compound>>("attachments")
            ?.forEach(::Attachment)
    }
    
    private fun saveAndRemoveAttachments(player: Player) {
        val uuid = player.uniqueId
        
        saveAttachments(player)
        getAttachments(uuid).forEach(Attachment::despawn)
        attachments.remove(uuid)
    }
    
    private fun saveAttachments(player: Player) {
        val dataContainer = player.persistentDataContainer
        val activeAttachments = attachments[player.uniqueId]?.toList()
        if (activeAttachments != null && activeAttachments.isNotEmpty()) {
            dataContainer.set(ATTACHMENTS_KEY, CompoundDataType, Compound().apply { set("attachments", activeAttachments) })
        } else dataContainer.remove(ATTACHMENTS_KEY)
    }
    
}