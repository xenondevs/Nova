package xyz.xenondevs.nova.world.block.tileentity

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.benmanes.caffeine.cache.Scheduler
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.joml.primitives.AABBdc
import xyz.xenondevs.commons.collections.weakHashSet
import xyz.xenondevs.invui.dsl.NormalSplitWindowDsl
import xyz.xenondevs.invui.dsl.WindowDsl
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.ui.menu.locale
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.ui.overlay.guitexture.getTitle
import xyz.xenondevs.nova.util.PlayerMapManager
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.toAABBd
import xyz.xenondevs.nova.world.block.name
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * A container for [Windows][Window] that belong to a [TileEntity].
 */
interface TileEntityMenu {
    
    /**
     * Registers [window] as a part of this menu.
     * Apart from the main window, every window created that belongs to a tile entity
     * must be registered through this.
     * 
     * Subsequent calls to [close] will [Window.close] [window] if it is open.
     */
    fun register(window: Window)
    
    /**
     * Opens the main [Window] associated with this container for [player].
     * Returns `true` if a window was opened, `false` otherwise.
     */
    fun open(player: Player): Boolean
    
    /**
     * Closes all windows associated with this container.
     */
    fun close()
    
    companion object {
        
        /**
         * Creates a new [TileEntityMenu] that has no main window, but can still track other windows.
         * The [open] function of the returned container will never open a window and always return `false`.
         * Any window opened under the menu will be automatically closed once the viewer is out of the interaction range with [bounds].
         */
        context(tileEntity: TileEntity)
        fun none(bounds: AABBdc = tileEntity.block.toAABBd()): TileEntityMenu = TileEntityMenuImpl(bounds)
        
        /**
         * Create a new [TileEntityMenu] that uses [getWindow] to get the main 
         * window for the given [Player]. [getWindow] may create a new window each time
         * or re-use existing windows.
         * Any window opened under the menu will be automatically closed once the viewer is out of the interaction range with [bounds].
         */
        context(tileEntity: TileEntity)
        fun from(bounds: AABBdc = tileEntity.block.toAABBd(), getWindow: (Player) -> Window): TileEntityMenu =
            IndividualTileEntityMenuImpl(bounds, getWindow)
        
        /**
         * Shortcut for [creating][from] a [TileEntityMenu] using the [standard][xyz.xenondevs.invui.dsl.window] [WindowDsl]
         * with an optional [texture] that automatically sets the title text to the tile entity's block name.
         * Any window opened under the menu will be automatically closed once the viewer is out of the interaction range with [bounds].
         * 
         * Equivalent to:
         * ```kotlin
         * MenuContainer.from { viewer -> 
         *     window(viewer) {
         *         if (texture != null) {
         *             title by texture.getTitle(tileEntity.block.name, locale)
         *         } else {
         *             title by tileEntity.block.name
         *         }
         *         window()
         *     }
         * }
         * ```
         */
        context(tileEntity: TileEntity)
        fun window(
            texture: RegistryEntry.Nova<GuiTexture>? = null,
            bounds: AABBdc = tileEntity.block.toAABBd(),
            window: NormalSplitWindowDsl.() -> Unit
        ): TileEntityMenu = window(::window, texture, bounds, window)
        
        /**
         * Shortcut for [creating][from] a [TileEntityMenu] using [WindowDsl] of a custom type with an optional [texture]
         * that automatically sets the title text to the tile entity's block name.
         * Any window opened under the menu will be automatically closed once the viewer is out of the interaction range with [bounds].
         * 
         * Equivalent to:
         * ```kotlin
         * MenuContainer.from { viewer -> 
         *     windowDsl(viewer) {
         *         if (texture != null) {
         *             title by texture.getTitle(tileEntity.block.name, locale)
         *         } else {
         *             title by tileEntity.block.name
         *         }
         *         window()
         *     }
         * }
         * ```
         */
        context(tileEntity: TileEntity)
        fun <T : WindowDsl> window(
            windowDsl: (Player, T.() -> Unit) -> Window,
            texture: RegistryEntry.Nova<GuiTexture>? = null,
            bounds: AABBdc = tileEntity.block.toAABBd(),
            window: T.() -> Unit
        ): TileEntityMenu = from(bounds) {
            windowDsl(it) {
                if (texture != null) {
                    title by texture.getTitle(tileEntity.blockType.name, locale)
                } else {
                    title by tileEntity.blockType.name
                }
                window()
            }
        }
        
        /**
         * Shortcut for [creating][from] a [TileEntityMenu] using the [standard][xyz.xenondevs.invui.dsl.window] [WindowDsl]
         * with an optional [texture] that automatically sets the title text to the tile entity's block name and is cached with [expireAfterClose].
         * Any window opened under the menu will be automatically closed once the viewer is out of the interaction range with [bounds].
         * 
         * @see TileEntityMenu.window
         */
        context(tileEntity: TileEntity)
        fun cachedWindow(
            texture: RegistryEntry.Nova<GuiTexture>? = null,
            bounds: AABBdc = tileEntity.block.toAABBd(),
            expireAfterClose: Duration = 1.minutes,
            window: NormalSplitWindowDsl.() -> Unit
        ): TileEntityMenu = cachedWindow(::window, texture, bounds, expireAfterClose, window)
        
        /**
         * Shortcut for [creating][from] a [TileEntityMenu] using [WindowDsl] of a custom type with an optional [texture]
         * that automatically sets the title text to the tile entity's block name and is cached with [expireAfterClose].
         * Any window opened under the menu will be automatically closed once the viewer is out of the interaction range with [bounds].
         * 
         * @see TileEntityMenu.window
         */
        context(tileEntity: TileEntity)
        fun <T : WindowDsl> cachedWindow(
            windowDsl: (Player, T.() -> Unit) -> Window,
            texture: RegistryEntry.Nova<GuiTexture>? = null,
            bounds: AABBdc = tileEntity.block.toAABBd(),
            expireAfterClose: Duration = 1.minutes,
            window: (T.() -> Unit),
        ): TileEntityMenu = CachedWindowTileEntityMenuImpl(bounds, expireAfterClose) {
            windowDsl(it) {
                if (texture != null) {
                    title by texture.getTitle(tileEntity.blockType.name, locale)
                } else {
                    title by tileEntity.blockType.name
                }
                window()
            }
        }
        
    }
    
}

private open class TileEntityMenuImpl(
    private val bounds: AABBdc
) : TileEntityMenu {
    
    private val registeredWindows = weakHashSet<Window>()
    private val openWindows = HashSet<Window>()
    
    override fun register(window: Window) {
        if (!registeredWindows.add(window))
            return
        
        window.addOpenHandler {
            openWindows += window
            DistanceBasedMenuCloser.openMenus[window.viewer] = bounds
        }
        window.addCloseHandler {
            openWindows -= window
            DistanceBasedMenuCloser.openMenus -= window.viewer
        }
    }
    
    override fun open(player: Player) = false
    
    override fun close() {
        openWindows.toSet().forEach(Window::close)
    }
    
}

private class IndividualTileEntityMenuImpl(
    bounds: AABBdc,
    private val createWindow: (player: Player) -> Window
) : TileEntityMenuImpl(bounds) {
    
    override fun open(player: Player): Boolean {
        val window = createWindow(player)
        check(window.viewer == player)
        register(window)
        window.open()
        return true
    }
    
}

private class CachedWindowTileEntityMenuImpl(
    private val bounds: AABBdc,
    expireAfterClose: Duration,
    createWindow: (Player) -> Window
) : TileEntityMenu {
    
    private val registeredWindows = weakHashSet<Window>()
    private val openWindows = HashSet<Window>()
    private val activeViewers = HashSet<Player>()
    
    private val cache: LoadingCache<Player, Window> by lazy {
        Caffeine.newBuilder()
            .weakKeys()
            .expireAfter(AfterCloseExpiry(expireAfterClose.inWholeNanoseconds))
            .scheduler(Scheduler.systemScheduler())
            .build(createWindow)
    }
    
    override fun open(player: Player): Boolean {
        val window = cache.get(player)
        check(window.viewer == player)
        register(window)
        window.open()
        return true
    }
    
    override fun close() {
        openWindows.toList().forEach(Window::close)
    }
    
    override fun register(window: Window) {
        if (!registeredWindows.add(window))
            return
        val viewer = window.viewer
        
        window.addOpenHandler {
            openWindows += window
            activeViewers += viewer
            DistanceBasedMenuCloser.openMenus[viewer] = bounds
            refreshExpiry(viewer)
        }
        
        window.addCloseHandler {
            openWindows -= window
            activeViewers -= window.viewer
            DistanceBasedMenuCloser.openMenus -= viewer
            refreshExpiry(viewer)
        }
    }
    
    private fun refreshExpiry(player: Player) {
        cache.asMap().computeIfPresent(player) { _, w -> w }
    }
    
    inner class AfterCloseExpiry(private val nanos: Long) : Expiry<Player, Window> {
        
        override fun expireAfterCreate(key: Player, value: Window, currentTime: Long) = Long.MAX_VALUE
        override fun expireAfterRead(key: Player, value: Window, currentTime: Long, currentDuration: Long) = currentDuration
        
        override fun expireAfterUpdate(key: Player, value: Window, currentTime: Long, currentDuration: Long) =
            if (key in activeViewers) Long.MAX_VALUE else nanos
        
    }
    
}

private object DistanceBasedMenuCloser : Listener {
    
    val openMenus: MutableMap<Player, AABBdc> = PlayerMapManager.createMap()
    
    init {
        registerEvents()
    }
    
    @EventHandler
    private fun handleTeleport(event: PlayerTeleportEvent) {
        val player = event.player
        if (player in openMenus)
            player.closeInventory()
    }
    
    @EventHandler
    private fun handleMove(event: PlayerMoveEvent) {
        val player = event.player
        val bounds = openMenus[player]
            ?: return
        
        val playerPos = player.eyeLocation
        val dx = max(max(bounds.minX() - playerPos.x, playerPos.x - bounds.maxX()), 0.0)
        val dy = max(max(bounds.minY() - playerPos.y, playerPos.y - bounds.maxY()), 0.0)
        val dz = max(max(bounds.minZ() - playerPos.z, playerPos.z - bounds.maxZ()), 0.0)
        val distanceSqr = dx * dx + dy * dy + dz * dz
        
        val reach = (player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)?.value ?: 10.0) + 4.0
        val maxDistanceSqr = reach * reach
        
        if (distanceSqr > maxDistanceSqr)
            player.closeInventory()
    }
    
}
