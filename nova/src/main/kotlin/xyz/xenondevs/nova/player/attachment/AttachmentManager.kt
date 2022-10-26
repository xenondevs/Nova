package xyz.xenondevs.nova.player.attachment

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import xyz.xenondevs.nmsutils.network.event.PacketEventManager
import xyz.xenondevs.nmsutils.network.event.PacketHandler
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundSetPassengersPacketEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.serialization.persistentdata.get
import xyz.xenondevs.nova.data.serialization.persistentdata.set
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTaskLater
import xyz.xenondevs.nova.util.runTaskTimer
import kotlin.collections.set

private val ATTACHMENTS_KEY = NamespacedKey(NOVA, "attachments1")

object AttachmentManager : Initializable(), Listener {
    
    override val inMainThread = true
    override val dependsOn = setOf(AddonsInitializer)
    
    private val activeAttachments = HashMap<Player, HashMap<AttachmentType<*>, Attachment>>()
    private val inactiveAttachments = HashMap<Player, HashSet<NamespacedId>>()
    
    override fun init() {
        registerEvents()
        PacketEventManager.registerListener(this)
        Bukkit.getOnlinePlayers().forEach(::loadAttachments)
        runTaskTimer(0, 1) { activeAttachments.values.flatMap(Map<*, Attachment>::values).forEach(Attachment::handleTick) }
    }
    
    override fun disable() {
        LOGGER.info("Saving attachments")
        Bukkit.getOnlinePlayers().forEach { saveAndRemoveAttachments(it) }
    }
    
    fun <A : Attachment, T : AttachmentType<A>> addAttachment(player: Player, type: T): A {
        check(!player.isDead) { "Attachments cannot be added to dead players" }
        
        val attachmentsMap = activeAttachments.getOrPut(player, ::HashMap)
        check(type !in attachmentsMap) { "An attachment with that type is already active" }
        
        val attachment = type.constructor(player)
        attachmentsMap[type] = attachment
        
        return attachment
    }
    
    fun removeAttachment(player: Player, type: AttachmentType<*>) {
        val inactiveAttachmentsMap = inactiveAttachments[player]
        inactiveAttachmentsMap?.remove(type.id)
        
        if (inactiveAttachmentsMap != null && inactiveAttachmentsMap.isEmpty())
            inactiveAttachments -= player
        
        val activeAttachmentsMap = activeAttachments[player]
        val attachment = activeAttachmentsMap?.remove(type)
        
        if (activeAttachmentsMap != null && activeAttachmentsMap.isEmpty())
            activeAttachments -= player
        
        attachment?.despawn()
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
    private fun handleDeath(event: PlayerDeathEvent) {
        deactivateAttachments(event.entity)
    }
    
    @EventHandler
    private fun handleRespawn(event: PlayerRespawnEvent) {
        runTaskLater(1) {
            val player = event.player
            if (player.isOnline && !player.isDead)
                activateAttachments(event.player)
        }
    }
    
    @PacketHandler
    private fun handlePassengersSet(event: ClientboundSetPassengersPacketEvent) {
        val attachments = (activeAttachments.entries.firstOrNull { (player, _) -> player.entityId == event.vehicle } ?: return).value.values
        event.passengers += attachments.map(Attachment::passengerId)
    }
    
    private fun deactivateAttachments(player: Player) {
        val attachmentsMap = activeAttachments[player] ?: return
        val inactive = inactiveAttachments.getOrPut(player, ::HashSet)
        attachmentsMap.forEach { (type, attachment) ->
            inactive += type.id
            attachment.despawn()
        }
        
        activeAttachments -= player
    }
    
    private fun activateAttachments(player: Player) {
        val attachmentIds = inactiveAttachments[player] ?: return
        activateAttachments(player, attachmentIds)
        inactiveAttachments -= player
    }
    
    private fun activateAttachments(player: Player, attachmentIds: Set<NamespacedId>) {
        attachmentIds.forEach {
            val type = AttachmentTypeRegistry.of<AttachmentType<*>>(it)
            if (type != null) {
                addAttachment(player, type)
            } else LOGGER.severe("Unknown attachment type $it on player ${player.name}")
        }
    }
    
    private fun loadAttachments(player: Player) {
        val attachmentIds = player.persistentDataContainer
            .get<HashSet<NamespacedId>>(ATTACHMENTS_KEY)
            ?: return
        
        if (player.isDead) {
            inactiveAttachments.getOrPut(player, ::HashSet) += attachmentIds
        } else {
            activateAttachments(player, attachmentIds)
        }
    }
    
    private fun saveAttachments(player: Player) {
        val dataContainer = player.persistentDataContainer
        val attachmentIds = HashSet<NamespacedId>()
        activeAttachments[player]?.forEach { attachmentIds += it.key.id }
        inactiveAttachments[player]?.let { attachmentIds += it }
        if (attachmentIds.isNotEmpty()) {
            dataContainer.set(ATTACHMENTS_KEY, attachmentIds)
        } else dataContainer.remove(ATTACHMENTS_KEY)
    }
    
    private fun saveAndRemoveAttachments(player: Player) {
        saveAttachments(player)
        activeAttachments.remove(player)
            ?.forEach { it.value.despawn() }
    }
    
}