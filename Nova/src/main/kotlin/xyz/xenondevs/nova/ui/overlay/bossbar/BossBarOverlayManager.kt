package xyz.xenondevs.nova.ui.overlay.bossbar

import net.md_5.bungee.api.chat.ComponentBuilder
import net.minecraft.network.chat.Component
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_19_R1.util.CraftChatMessage
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import xyz.xenondevs.nmsutils.bossbar.BossBar
import xyz.xenondevs.nmsutils.bossbar.operation.AddBossBarOperation
import xyz.xenondevs.nmsutils.bossbar.operation.UpdateNameBossBarOperation
import xyz.xenondevs.nmsutils.network.event.PacketHandler
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundBossEventPacketEvent
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.ui.overlay.character.MoveCharacters
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.registerPacketListener
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.send
import java.util.*

private val BAR_AMOUNT by configReloadable { DEFAULT_CONFIG.getInt("overlay.bossbar.amount") }

// TODO: proper config reloading
// TODO: preserve normal boss bars
object BossBarOverlayManager : Initializable(), Listener {
    
    override val inMainThread = true
    override val dependsOn = emptySet<Initializable>()
    
    private val bars = HashMap<UUID, Array<BossBar>>()
    private val overlays = HashMap<UUID, HashSet<BossBarOverlay>>()
    
    override fun init() {
        registerEvents()
        registerPacketListener()
        runTaskTimer(0, 1, ::handleTick)
    }
    
    fun registerOverlay(player: Player, overlay: BossBarOverlay) {
        overlays.getOrPut(player.uniqueId, ::HashSet) += overlay
        remakeBars(player.uniqueId)
    }
    
    fun unregisterOverlay(player: Player, overlay: BossBarOverlay) {
        overlays.getOrPut(player.uniqueId, ::HashSet) -= overlay
        remakeBars(player.uniqueId)
    }
    
    private fun handleTick() {
        overlays.forEach { (uuid, overlays) ->
            if (overlays.any { it.changed })
                remakeBars(uuid)
        }
    }
    
    private fun remakeBars(playerUUID: UUID) {
        val overlays = overlays[playerUUID]!!
        val bars = bars[playerUUID]!!
        
        // clear bars
        bars.forEach { it.nmsName = Component.literal("") }
        
        overlays
            .groupBy { it.barLevel }
            .forEach { (barLevel, overlays) ->
                val builder = ComponentBuilder()
                overlays.forEach {
                    // reset changed state
                    it.changed = false
                    
                    // append text / movement
                    builder
                        .append(it.components)
                        .append(MoveCharacters.getMovingComponent(-it.width))
                }
                
                bars[barLevel].name = builder.create()
            }
        
        // send update if player is online
        val player = Bukkit.getPlayer(playerUUID) ?: return
        bars.forEach { player.send(it.updateNamePacket) }
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        val player = event.player
        val playerBars = bars.getOrPut(player.uniqueId) { Array(BAR_AMOUNT) { BossBar(UUID(it.toLong(), 0L)) } }
        playerBars.forEach { player.send(it.addPacket) }
    }
    
    @PacketHandler(ignoreIfCancelled = true)
    private fun handleBossBar(event: ClientboundBossEventPacketEvent) {
        val operation = event.operation
        if (operation is AddBossBarOperation) {
            println(CraftChatMessage.toJSON(operation.name))
        } else if (operation is UpdateNameBossBarOperation) {
            println(CraftChatMessage.toJSON(operation.name))
        }
        
        val id = event.id
        event.isCancelled = id.leastSignificantBits != 0L || id.mostSignificantBits !in 0 until BAR_AMOUNT
    }
    
}