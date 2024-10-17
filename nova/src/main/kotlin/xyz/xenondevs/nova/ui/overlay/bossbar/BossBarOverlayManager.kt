package xyz.xenondevs.nova.ui.overlay.bossbar

import net.kyori.adventure.text.Component
import net.minecraft.world.BossEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.inventoryaccess.util.ReflectionRegistry
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.config.node
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.network.event.PacketHandler
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.clientbound.ClientboundBossEventPacketEvent
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.network.event.unregisterPacketListener
import xyz.xenondevs.nova.ui.overlay.MovedFonts
import xyz.xenondevs.nova.ui.overlay.bossbar.positioning.BarMatchInfo
import xyz.xenondevs.nova.ui.overlay.bossbar.positioning.BarOrigin
import xyz.xenondevs.nova.ui.overlay.bossbar.positioning.BarPositioning
import xyz.xenondevs.nova.ui.overlay.bossbar.vanilla.VanillaBossBarOverlay
import xyz.xenondevs.nova.ui.overlay.bossbar.vanilla.VanillaBossBarOverlayCompound
import xyz.xenondevs.nova.util.bossbar.BossBar
import xyz.xenondevs.nova.util.bossbar.operation.AddBossBarOperation
import xyz.xenondevs.nova.util.bossbar.operation.RemoveBossBarOperation
import xyz.xenondevs.nova.util.bossbar.operation.UpdateNameBossBarOperation
import xyz.xenondevs.nova.util.bossbar.operation.UpdateProgressBossBarOperation
import xyz.xenondevs.nova.util.bossbar.operation.UpdatePropertiesBossBarOperation
import xyz.xenondevs.nova.util.bossbar.operation.UpdateStyleBossBarOperation
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.util.unregisterEvents
import java.util.*
import kotlin.math.max

@InternalInit(stage = InternalInitStage.POST_WORLD)
object BossBarOverlayManager : Listener, PacketListener {
    
    private val BOSSBAR_CONFIG = MAIN_CONFIG.node("overlay", "bossbar")
    internal val ENABLED by BOSSBAR_CONFIG.entry<Boolean>("enabled")
    private val BAR_AMOUNT by BOSSBAR_CONFIG.entry<Int>("amount")
    private val SEND_BARS_AFTER_RESOURCE_PACK_LOADED by BOSSBAR_CONFIG.entry<Boolean>("send_bars_after_resource_pack_loaded")
    
    private var tickTask: BukkitTask? = null
    private val bars = HashMap<UUID, Array<BossBar>>()
    
    private val overlays = HashMap<UUID, ArrayList<BossBarOverlayCompound>>()
    private val sortedFixedOverlays = HashMap<UUID, List<BossBarOverlayCompound>>()
    private val sortedDynamicOverlays = HashMap<UUID, List<BossBarOverlayCompound>>()
    private val changes = HashSet<UUID>()
    
    private val trackedOrigins = HashMap<UUID, BarOrigin>()
    private val trackedBars = HashMap<Player, LinkedHashMap<UUID, BossBar>>()
    private val vanillaBarOverlays = HashMap<BossBar, VanillaBossBarOverlayCompound>()
    
    @InitFun
    private fun init() {
        BOSSBAR_CONFIG.subscribe { reload() }
        reload()
    }
    
    private fun reload() {
        // was previously enabled?
        if (tickTask != null) {
            unregisterEvents()
            unregisterPacketListener()
            Bukkit.getOnlinePlayers().forEach(::removeBars)
            tickTask?.cancel()
            tickTask = null
            
            if (!ENABLED) {
                // re-add tracked boss bars as real boss bars
                trackedBars.forEach { (player, bars) ->
                    bars.values.forEach { bar -> player.send(bar.addPacket) }
                }
                
                // clear tracked bars to prevent them from being sent again
                trackedBars.clear()
            }
        }
        
        if (ENABLED) {
            registerEvents()
            registerPacketListener()
            tickTask = runTaskTimer(0, 1, ::handleTick)
            Bukkit.getOnlinePlayers().forEach(::sendBars)
        }
    }
    
    @DisableFun
    private fun disable() {
        Bukkit.getOnlinePlayers().forEach(::removeBars)
    }
    
    fun registerOverlay(player: Player, overlay: BossBarOverlayCompound) {
        val uuid = player.uniqueId
        overlays.getOrPut(uuid, ::ArrayList) += overlay
        changes += uuid
        sortedFixedOverlays.remove(uuid)
        sortedDynamicOverlays.remove(uuid)
    }
    
    fun unregisterOverlay(player: Player, overlay: BossBarOverlayCompound) {
        val uuid = player.uniqueId
        val changed = overlays.getOrPut(uuid, ::ArrayList).remove(overlay)
        if (changed) {
            changes += uuid
            sortedFixedOverlays.remove(uuid)
            sortedDynamicOverlays.remove(uuid)
        }
    }
    
    private fun handleTick() {
        overlays.forEach { (uuid, overlays) ->
            if (uuid in changes || overlays.any { it.hasChanged }) {
                if (remakeBars(uuid)) changes -= uuid
            }
        }
    }
    
    @Suppress("DEPRECATION")
    private fun remakeBars(playerUUID: UUID): Boolean {
        val overlays = overlays[playerUUID] ?: return false
        val bars = bars[playerUUID] ?: return false
        val player = Bukkit.getPlayer(playerUUID) ?: return false
        val locale = player.locale
        
        // clear bars
        bars.forEach { it.name = Component.empty() }
        
        // group sorted fixed bars by bar level
        val groupedFixedOverlays = groupOverlaysByBarLevel(
            sortedFixedOverlays.getOrPut(playerUUID) {
                overlays.filter { it.positioning is BarPositioning.Fixed }.let(BarPositioning::sort).reversed()
            }
        ) { (it.positioning as BarPositioning.Fixed).offset }
        
        // group sorted dynamic bars by bar level
        val dynamicOffsets = calculateDynamicOverlayCompoundOffsets(
            sortedDynamicOverlays.getOrPut(playerUUID) {
                overlays.filter { it.positioning is BarPositioning.Dynamic }.let(BarPositioning::sort)
            },
            locale
        )
        val groupedDynamicOverlays = groupOverlaysByBarLevel(dynamicOffsets.keys) {
            dynamicOffsets[it] ?: throw NoSuchElementException("Could not find calculated dynamic offset for: $it")
        }
        
        // build new bars
        bars.forEachIndexed { barLevel, bar ->
            val barLevelOverlays = (groupedFixedOverlays[barLevel] ?: emptyList()) + (groupedDynamicOverlays[barLevel] ?: emptyList())
            if (barLevelOverlays.isEmpty())
                return@forEachIndexed
            
            val builder = Component.text()
            barLevelOverlays.forEach { (overlay, offset) ->
                
                val centerX = overlay.centerX
                var width = overlay.getWidth(player.locale)
                if (centerX != null) {
                    val preMove = centerX - width / 2
                    builder.move(preMove)
                    
                    width += preMove
                }
                
                builder
                    .append(MovedFonts.moveVertically(overlay.component, offset, true))
                    .move(-width)
            }
            
            bar.name = builder.build()
        }
        
        // reset changed state
        overlays.forEach { it.hasChanged = false }
        
        // send update
        bars.forEach { player.send(it.updateNamePacket) }
        
        return true
    }
    
    private fun calculateDynamicOverlayCompoundOffsets(
        sortedDynamicOverlays: Iterable<BossBarOverlayCompound>,
        locale: String
    ): Map<BossBarOverlayCompound, Int> {
        val offsets = HashMap<BossBarOverlayCompound, Int>()
        
        var offset = 0
        var prevPositioning: BarPositioning.Dynamic? = null
        var prevVerticalRange: IntRange? = null
        for (compound in sortedDynamicOverlays) {
            val positioning = compound.positioning as BarPositioning.Dynamic
            val verticalRange = compound.getVerticalRange(locale)
            
            if (prevPositioning != null && prevVerticalRange != null) {
                // move the offset just below the previous overlay
                offset += prevVerticalRange.last + 1
                
                // move the offset down by the amount of pixels that it draws upwards to prevent it from overlapping with the previous overlay
                // this is intentionally only done if this is not the first overlay in order to not have awkward gaps at the top
                offset += 0 - verticalRange.first
                
                // apply margin
                val marginTop = positioning.marginTop
                val prevMarginBottom = prevPositioning.marginBottom
                
                offset += max(marginTop, prevMarginBottom)
            }
            
            // set offset for this overlay
            offsets[compound] = offset
            
            prevPositioning = positioning
            prevVerticalRange = verticalRange
        }
        
        return offsets
    }
    
    private fun groupOverlaysByBarLevel(
        overlays: Iterable<BossBarOverlayCompound>,
        offsetReceiver: (BossBarOverlayCompound) -> Int
    ): Map<Int, List<Pair<BossBarOverlay, Int>>> {
        val groupedOverlays = HashMap<Int, ArrayList<Pair<BossBarOverlay, Int>>>()
        
        for (compound in overlays) {
            for (overlay in compound.overlays) {
                val totalOffset = offsetReceiver(compound) + overlay.offset
                val barLevel = (totalOffset / 19.0).toInt()
                val inBarOffset = totalOffset % 19
                
                groupedOverlays.getOrPut(barLevel, ::ArrayList) += overlay to inBarOffset
            }
        }
        
        return groupedOverlays
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
        
        // remove tracked bars and associated fake bar overlays
        trackedBars.remove(player)?.forEach { (_, bar) ->
            val compound = vanillaBarOverlays.remove(bar)
            if (compound != null)
                unregisterOverlay(player, compound)
        }
    }
    
    @PacketHandler(ignoreIfCancelled = true)
    private fun handleBossBar(event: ClientboundBossEventPacketEvent) {
        val id = event.id
        if (id.leastSignificantBits != 0L || id.mostSignificantBits !in 0..<BAR_AMOUNT) {
            event.isCancelled = true
            
            val player = event.player
            when (val operation = event.operation) {
                is AddBossBarOperation -> {
                    val bar = BossBar.of(id, operation)
                    // add the bar to the tracked bar map
                    val trackedPlayerBars = trackedBars.getOrPut(player, ::LinkedHashMap)
                    trackedPlayerBars[id] = bar
                    
                    // create a fake bar for rendering
                    val fakeBarOverlay = VanillaBossBarOverlay(player, bar)
                    val matchInfo = BarMatchInfo(bar, trackedPlayerBars.values.indexOf(bar), trackedOrigins[id] ?: BarOrigin.Minecraft)
                    val compound = VanillaBossBarOverlayCompound(fakeBarOverlay, matchInfo)
                    vanillaBarOverlays[bar] = compound
                    registerOverlay(player, compound)
                }
                
                is RemoveBossBarOperation -> {
                    // remove from tracked bars map
                    val bar = trackedBars[player]?.remove(id)
                    
                    // remove associated fake bar overlay
                    val compound = vanillaBarOverlays.remove(bar)
                    if (compound != null) unregisterOverlay(player, compound)
                }
                
                else -> {
                    // update the values in the boss bar
                    val bar = trackedBars[player]?.get(id) ?: return
                    when (operation) {
                        is UpdateNameBossBarOperation -> bar.name = operation.name
                        is UpdateProgressBossBarOperation -> bar.progress = operation.progress
                        
                        is UpdateStyleBossBarOperation -> {
                            bar.color = operation.color
                            bar.overlay = operation.overlay
                        }
                        
                        is UpdatePropertiesBossBarOperation -> {
                            bar.darkenScreen = operation.darkenScreen
                            bar.playMusic = operation.playMusic
                            bar.createWorldFog = operation.createWorldFog
                        }
                        
                        else -> throw UnsupportedOperationException()
                    }
                    
                    // mark fake bar overlay changes
                    val compound = vanillaBarOverlays[bar] ?: return
                    compound.matchInfo = compound.matchInfo.copy(barIndex = trackedBars[player]?.values?.indexOf(bar))
                    compound.overlay.update()
                    compound.hasChanged = true
                }
            }
        }
    }
    
    internal fun handleBossBarAddPacketCreation(event: BossEvent) {
        var plugin: Plugin? = null
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).forEach {
            val classLoader = it.declaringClass.classLoader
            if (classLoader?.javaClass == ReflectionRegistry.PLUGIN_CLASS_LOADER_CLASS) {
                plugin = ReflectionRegistry.PLUGIN_CLASS_LOADER_PLUGIN_FIELD.get(classLoader) as Plugin
            }
        }
        
        if (plugin != null && plugin != NOVA) {
            trackedOrigins[event.id] = BarOrigin.Plugin(plugin!!)
        }
    }
    
}