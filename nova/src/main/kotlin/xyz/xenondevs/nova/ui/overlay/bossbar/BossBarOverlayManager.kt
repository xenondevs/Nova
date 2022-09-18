package xyz.xenondevs.nova.ui.overlay.bossbar

import net.md_5.bungee.api.chat.ComponentBuilder
import net.minecraft.network.chat.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.nmsutils.bossbar.BossBar
import xyz.xenondevs.nmsutils.bossbar.operation.AddBossBarOperation
import xyz.xenondevs.nmsutils.bossbar.operation.RemoveBossBarOperation
import xyz.xenondevs.nmsutils.bossbar.operation.UpdateNameBossBarOperation
import xyz.xenondevs.nmsutils.bossbar.operation.UpdateProgressBossBarOperation
import xyz.xenondevs.nmsutils.bossbar.operation.UpdatePropertiesBossBarOperation
import xyz.xenondevs.nmsutils.bossbar.operation.UpdateStyleBossBarOperation
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
import xyz.xenondevs.nova.util.unregisterEvents
import xyz.xenondevs.nova.util.unregisterPacketListener
import java.util.*

private val BAR_AMOUNT by configReloadable { DEFAULT_CONFIG.getInt("overlay.bossbar.amount") }
private val SEND_BARS_AFTER_RESOURCE_PACK_LOADED by configReloadable { DEFAULT_CONFIG.getBoolean("overlay.bossbar.send_bars_after_resource_pack_loaded") }

object BossBarOverlayManager : Initializable(), Listener {
    
    override val inMainThread = true
    override val dependsOn = emptySet<Initializable>()
    
    private var tickTask: BukkitTask? = null
    private val bars = HashMap<UUID, Array<BossBar>>()
    private val overlays = HashMap<UUID, ArrayList<BossBarOverlay>>()
    private val changes = HashSet<UUID>()
    
    internal val trackedBars = HashMap<Player, LinkedHashMap<UUID, BossBar>>()
    internal val isEnabled by configReloadable { DEFAULT_CONFIG.getBoolean("overlay.bossbar.enabled") }
    
    override fun init() = reload()
    
    fun reload() {
        // was previously enabled?
        if (tickTask != null) {
            unregisterEvents()
            unregisterPacketListener()
            Bukkit.getOnlinePlayers().forEach(::removeBars)
            tickTask?.cancel()
            tickTask = null
        }
        
        if (isEnabled) {
            registerEvents()
            registerPacketListener()
            tickTask = runTaskTimer(0, 1, ::handleTick)
            Bukkit.getOnlinePlayers().forEach(::sendBars)
        } else {
            // re-add tracked boss bars as real boss bars
            trackedBars.forEach { (player, bars) ->
                bars.values.forEach { bar -> player.send(bar.addPacket) }
            }
        }
    }
    
    override fun disable() {
        Bukkit.getOnlinePlayers().forEach(::removeBars)
    }
    
    fun registerOverlay(player: Player, overlay: BossBarOverlay) {
        val uuid = player.uniqueId
        overlays.getOrPut(uuid, ::ArrayList) += overlay
        changes += uuid
    }
    
    fun registerBackgroundOverlay(player: Player, overlay: BossBarOverlay) {
        val uuid = player.uniqueId
        overlays.getOrPut(uuid, ::ArrayList).add(0, overlay)
        changes += uuid
    }
    
    fun registerOverlays(player: Player, overlays: Iterable<BossBarOverlay>) {
        val uuid = player.uniqueId
        this.overlays.getOrPut(uuid, ::ArrayList) += overlays
        changes += uuid
    }
    
    fun unregisterOverlay(player: Player, overlay: BossBarOverlay) {
        val uuid = player.uniqueId
        val changed = overlays.getOrPut(uuid, ::ArrayList).remove(overlay)
        if (changed) changes += uuid
    }
    
    fun unregisterOverlays(player: Player, overlays: Iterable<BossBarOverlay>) {
        val uuid = player.uniqueId
        val changed = this.overlays.getOrPut(uuid, ::ArrayList).removeAll(overlays.toSet())
        if (changed) changes += uuid
    }
    
    fun unregisterOverlayIf(player: Player, predicate: (BossBarOverlay) -> Boolean) {
        val uuid = player.uniqueId
        val changed = overlays[uuid]?.removeIf(predicate) ?: false
        if (changed) changes += uuid
    }
    
    fun getEndY(player: Player): Int {
        val overlays = overlays[player.uniqueId] ?: return 0
        var endY = 0
        overlays.asSequence()
            .filter { it !is FakeBossBarOverlay }
            .forEach {
                val curEndY = it.barLevel * -19 + it.endY
                if (curEndY < endY)
                    endY = curEndY
            }
        
        return endY
    }
    
    private fun handleTick() {
        overlays.forEach { (uuid, overlays) ->
            if (uuid in changes || overlays.any { it.changed }) {
                changes -= uuid
                remakeBars(uuid)
            }
        }
    }
    
    private fun remakeBars(playerUUID: UUID) {
        val overlays = overlays[playerUUID] ?: return
        val bars = bars[playerUUID] ?: return
        
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
        if (!SEND_BARS_AFTER_RESOURCE_PACK_LOADED) {
            sendBars(event.player)
            changes += event.player.uniqueId
        }
    }
    
    @EventHandler
    private fun handlePackStatus(event: PlayerResourcePackStatusEvent) {
        if (event.status == Status.SUCCESSFULLY_LOADED && SEND_BARS_AFTER_RESOURCE_PACK_LOADED) {
            sendBars(event.player)
            changes += event.player.uniqueId
        }
    }
    
    @EventHandler
    private fun handleQuit(event: PlayerQuitEvent) {
        val player = event.player
        trackedBars -= player
        unregisterOverlayIf(player) { it is FakeBossBarOverlay }
    }
    
    @PacketHandler(ignoreIfCancelled = true)
    private fun handleBossBar(event: ClientboundBossEventPacketEvent) {
        val id = event.id
        if (id.leastSignificantBits != 0L || id.mostSignificantBits !in 0 until BAR_AMOUNT) {
            event.isCancelled = true
            
            val player = event.player
            when (val operation = event.operation) {
                is AddBossBarOperation -> {
                    val bar = BossBar.of(id, operation)
                    // add the bar to the tracked bar map
                    trackedBars.getOrPut(player, ::LinkedHashMap)[id] = bar
                    // create a fake bar for rendering
                    registerOverlay(player, FakeBossBarOverlay(player, bar))
                }
                
                is RemoveBossBarOperation -> {
                    // remove from tracked bars map
                    val bar = trackedBars[player]?.remove(id)
                    // remove the fake bar associated with it
                    unregisterOverlayIf(player) { it is FakeBossBarOverlay && it.bar == bar }
                }
                
                else -> {
                    // update the values in the boss bar
                    val bossBar = trackedBars[player]?.get(id) ?: return
                    when (operation) {
                        is UpdateNameBossBarOperation -> bossBar.nmsName = operation.name
                        is UpdateProgressBossBarOperation -> bossBar.progress = operation.progress
                        
                        is UpdateStyleBossBarOperation -> {
                            bossBar.color = operation.color
                            bossBar.overlay = operation.overlay
                        }
                        
                        is UpdatePropertiesBossBarOperation -> {
                            bossBar.darkenScreen = operation.darkenScreen
                            bossBar.playMusic = operation.playMusic
                            bossBar.createWorldFog = operation.createWorldFog
                        }
                        
                        else -> throw UnsupportedOperationException()
                    }
                    
                    // mark fake bar overlay changes
                    overlays[player.uniqueId]
                        ?.first { it is FakeBossBarOverlay && it.bar == bossBar }
                        ?.changed = true
                }
            }
        }
    }
    
}