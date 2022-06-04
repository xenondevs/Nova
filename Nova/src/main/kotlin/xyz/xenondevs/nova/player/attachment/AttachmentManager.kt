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
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.serialization.persistentdata.get
import xyz.xenondevs.nova.data.serialization.persistentdata.set
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.network.event.clientbound.SetPassengersPacketEvent
import xyz.xenondevs.nova.util.runTaskTimer
import kotlin.collections.set

private val ATTACHMENTS_KEY = NamespacedKey(NOVA, "attachments")

object AttachmentManager : Initializable(), Listener {
    
    override val inMainThread = true
    override val dependsOn = setOf(AddonsInitializer)
    
    private val activeAttachments = HashMap<Player, HashMap<AttachmentType<*>, Attachment>>()
    
    override fun init() {
        LOGGER.info("Initializing AttachmentManager")
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        Bukkit.getOnlinePlayers().forEach(::loadAttachments)
        runTaskTimer(0, 1) { activeAttachments.values.flatMap(Map<*, Attachment>::values).forEach(Attachment::handleTick) }
    }
    
    override fun disable() {
        LOGGER.info("Saving attachments")
        Bukkit.getOnlinePlayers().forEach { saveAndRemoveAttachments(it) }
    }
    
    fun <A : Attachment, T : AttachmentType<A>> addAttachment(player: Player, type: T): A {
        val attachmentsMap = activeAttachments.getOrPut(player, ::HashMap)
        if (type in attachmentsMap)
            throw java.lang.IllegalStateException("An attachment with that type is already active")
        
        
        val attachment = type.constructor(player)
        attachmentsMap[type] = attachment
        
        return attachment
    }
    
    fun removeAttachment(player: Player, type: AttachmentType<*>) {
        val attachmentsMap = activeAttachments[player] ?: return
        val attachment = attachmentsMap.remove(type) ?: return
        
        if (attachmentsMap.isEmpty())
            activeAttachments -= player
        
        attachment.despawn()
    }
    
    @EventHandler
    private fun handlePlayerJoin(event: PlayerJoinEvent) {
        loadAttachments(event.player)
    }
    
    @EventHandler
    private fun handlePlayerQuit(event: PlayerQuitEvent) {
        saveAndRemoveAttachments(event.player)
    }
    
    @EventHandler
    private fun handlePassengersSet(event: SetPassengersPacketEvent) {
        val attachments = (activeAttachments.entries.firstOrNull { it.key.entityId == event.vehicle } ?: return).value.values
        event.passengers += attachments.map(Attachment::entityId)
    }
    
    private fun loadAttachments(player: Player) {
        player.persistentDataContainer
            .get<List<NamespacedId>>(ATTACHMENTS_KEY)
            ?.forEach {
                val type = AttachmentTypeRegistry.of<AttachmentType<*>>(it)
                if (type != null) {
                    addAttachment(player, type)
                } else LOGGER.severe("Unknown attachment type $it on player ${player.name}")
            }
    }
    
    private fun saveAttachments(player: Player) {
        val dataContainer = player.persistentDataContainer
        val activeAttachments = activeAttachments[player]?.map { it.key.id }
        if (activeAttachments != null && activeAttachments.isNotEmpty()) {
            dataContainer.set(ATTACHMENTS_KEY, activeAttachments)
        } else dataContainer.remove(ATTACHMENTS_KEY)
    }
    
    private fun saveAndRemoveAttachments(player: Player) {
        saveAttachments(player)
        activeAttachments.remove(player)
            ?.forEach { it.value.despawn() }
    }
    
}