package xyz.xenondevs.nova.world.block.tileentity

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.benmanes.caffeine.cache.Scheduler
import org.bukkit.entity.Player
import xyz.xenondevs.commons.collections.weakHashSet
import xyz.xenondevs.invui.dsl.NormalSplitWindowDsl
import xyz.xenondevs.invui.dsl.WindowDsl
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.ui.menu.locale
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.ui.overlay.guitexture.getTitle
import xyz.xenondevs.nova.world.block.tileentity.TileEntityMenu.Companion.from
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
         */
        fun none(): TileEntityMenu = TileEntityMenuImpl()
        
        /**
         * Create a new [TileEntityMenu] that uses [getWindow] to get the main 
         * window for the given [Player]. [getWindow] may create a new window each time
         * or re-use existing windows.
         */
        fun from(getWindow: (Player) -> Window): TileEntityMenu =
            IndividualTileEntityMenuImpl(getWindow)
        
        /**
         * Shortcut for [creating][from] a [TileEntityMenu] using the [standard][xyz.xenondevs.invui.dsl.window] [WindowDsl]
         * with an optional [texture] that automatically sets the title text to the tile entity's block name.
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
        context(_: TileEntity)
        fun window(
            texture: RegistryEntry.Nova<GuiTexture>? = null,
            window: NormalSplitWindowDsl.() -> Unit
        ): TileEntityMenu = window(::window, texture, window)
        
        /**
         * Shortcut for [creating][from] a [TileEntityMenu] using [WindowDsl] of a custom type with an optional [texture]
         * that automatically sets the title text to the tile entity's block name.
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
            window: T.() -> Unit
        ): TileEntityMenu = from {
            windowDsl(it) {
                if (texture != null) {
                    title by texture.getTitle(tileEntity.block.name, locale)
                } else {
                    title by tileEntity.block.name
                }
                window()
            } 
        }
        
        /**
         * Shortcut for [creating][from] a [TileEntityMenu] using the [standard][xyz.xenondevs.invui.dsl.window] [WindowDsl]
         * with an optional [texture] that automatically sets the title text to the tile entity's block name and is cached with [expireAfterClose].
         * 
         * @see TileEntityMenu.window
         */
        context(_: TileEntity)
        fun cachedWindow(
            texture: RegistryEntry.Nova<GuiTexture>? = null,
            expireAfterClose: Duration = 1.minutes,
            window: NormalSplitWindowDsl.() -> Unit
        ): TileEntityMenu = cachedWindow(::window, texture, expireAfterClose, window)
        
        /**
         * Shortcut for [creating][from] a [TileEntityMenu] using [WindowDsl] of a custom type with an optional [texture]
         * that automatically sets the title text to the tile entity's block name and is cached with [expireAfterClose].
         * 
         * @see TileEntityMenu.window
         */
        context(tileEntity: TileEntity)
        fun <T : WindowDsl> cachedWindow(
            windowDsl: (Player, T.() -> Unit) -> Window,
            texture: RegistryEntry.Nova<GuiTexture>? = null,
            expireAfterClose: Duration = 1.minutes,
            window: (T.() -> Unit),
        ): TileEntityMenu = CachedWindowTileEntityMenuImpl(expireAfterClose) {
            windowDsl(it) {
                if (texture != null) {
                    title by texture.getTitle(tileEntity.block.name, locale)
                } else {
                    title by tileEntity.block.name
                }
                window()
            }
        }
        
    }
    
}

private open class TileEntityMenuImpl : TileEntityMenu {
    
    private val registeredWindows = weakHashSet<Window>()
    private val openWindows = HashSet<Window>()
    
    override fun register(window: Window) {
        if (!registeredWindows.add(window))
            return
        
        window.addOpenHandler {
            openWindows += window
        }
        window.addCloseHandler {
            openWindows -= window
        }
    }
    
    override fun open(player: Player) = false
    
    override fun close() {
        openWindows.toSet().forEach(Window::close)
    }
    
}

private class IndividualTileEntityMenuImpl(
    private val createWindow: (player: Player) -> Window
) : TileEntityMenuImpl() {
    
    override fun open(player: Player): Boolean {
        val window = createWindow(player)
        check(window.viewer == player)
        register(window)
        window.open()
        return true
    }
    
}

private class CachedWindowTileEntityMenuImpl(
    expireAfterClose: Duration,
    createWindow: (Player) -> Window
) : TileEntityMenu {
    
    private val registeredWindows = weakHashSet<Window>()
    private val openWindows = HashSet<Window>()
    private val activeViewers = HashSet<Player>()
    
    private val cache: LoadingCache<Player, Window> = Caffeine.newBuilder()
        .weakKeys()
        .expireAfter(AfterCloseExpiry(expireAfterClose.inWholeNanoseconds))
        .scheduler(Scheduler.systemScheduler())
        .build(createWindow)
    
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
            refreshExpiry(viewer)
        }
        
        window.addCloseHandler {
            openWindows -= window
            activeViewers -= window.viewer
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