package xyz.xenondevs.nova.ui.overlay.bossbar

import net.md_5.bungee.api.chat.ComponentBuilder
import net.minecraft.network.chat.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import xyz.xenondevs.nmsutils.bossbar.BossBar
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
    private val overlays = HashMap<UUID, ArrayList<BossBarOverlay>>()
    
    override fun init() {
        registerEvents()
        registerPacketListener()
        runTaskTimer(0, 1, ::handleTick)
        Bukkit.getOnlinePlayers().forEach(::sendBars)
    }
    
    override fun disable() {
        Bukkit.getOnlinePlayers().forEach(::removeBars)
    }
    
    fun registerOverlay(player: Player, overlay: BossBarOverlay) {
        overlays.getOrPut(player.uniqueId, ::ArrayList) += overlay
        remakeBars(player.uniqueId)
    }
    
    fun registerOverlays(player: Player, overlays: Iterable<BossBarOverlay>) {
        this.overlays.getOrPut(player.uniqueId, ::ArrayList) += overlays
        remakeBars(player.uniqueId)
    }
    
    fun unregisterOverlay(player: Player, overlay: BossBarOverlay) {
        overlays.getOrPut(player.uniqueId, ::ArrayList) -= overlay
        remakeBars(player.uniqueId)
    }
    
    fun unregisterOverlays(player: Player, overlays: Iterable<BossBarOverlay>) {
        this.overlays.getOrPut(player.uniqueId, ::ArrayList) -= overlays.toSet()
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
                    
                    val centerX = it.centerX
                    var width = it.width
                    if (centerX != null) {
                        val preMove = centerX - width / 2
                        builder.append(MoveCharacters.getMovingComponent(preMove))
                        
                        width += preMove
                    }
                    
                    builder
                        .append(it.components)
                        .append(MoveCharacters.getMovingComponent(-width))
                }
                
                bars[barLevel].name = builder.create()
            }
        
        // send update if player is online
        val player = Bukkit.getPlayer(playerUUID) ?: return
        bars.forEach { player.send(it.updateNamePacket) }
    }
    
    private fun sendBars(player: Player) {
        val playerBars = bars.getOrPut(player.uniqueId) { Array(BAR_AMOUNT) { BossBar(UUID(it.toLong(), 0L)) } }
        playerBars.forEach { player.send(it.addPacket) }
    }
    
    private fun removeBars(player: Player) {
        val playerBars = bars[player.uniqueId] ?: return
        playerBars.forEach { player.send(it.removePacket) }
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        sendBars(event.player)
    }
    
    @PacketHandler(ignoreIfCancelled = true)
    private fun handleBossBar(event: ClientboundBossEventPacketEvent) {
        val id = event.id
        event.isCancelled = id.leastSignificantBits != 0L || id.mostSignificantBits !in 0 until BAR_AMOUNT
    }
    
}