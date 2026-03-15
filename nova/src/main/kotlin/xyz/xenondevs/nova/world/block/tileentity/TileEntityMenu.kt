package xyz.xenondevs.nova.world.block.tileentity

import org.bukkit.entity.Player
import xyz.xenondevs.commons.collections.weakHashSet
import xyz.xenondevs.invui.dsl.NormalSplitWindowDsl
import xyz.xenondevs.invui.dsl.WindowDsl
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.tileentity.TileEntityMenu.Companion.from

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
         * that automatically sets the title to the [TileEntity.block] [NovaBlock.name].
         * 
         * Equivalent to:
         * ```kotlin
         * MenuContainer.from { viewer -> 
         *     window(viewer) {
         *         title by block.name
         *         window()
         *     }
         * }
         * ```
         */
        context(_: TileEntity)
        fun window(window: NormalSplitWindowDsl.() -> Unit): TileEntityMenu =
            window(::window, window)
        
        /**
         * Shortcut for [creating][from] a [TileEntityMenu] using [WindowDsl] of a custom type
         * that automatically sets the title to the [TileEntity.block] [NovaBlock.name].
         * 
         * Equivalent to:
         * ```kotlin
         * MenuContainer.from { viewer ->
         *     windowDsl(viewer) {
         *         title by block.name
         *         window()
         *     }
         * }
         * ```
         */
        context(tileEntity: TileEntity)
        fun <T : WindowDsl> window(
            windowDsl: (Player, T.() -> Unit) -> Window,
            window: T.() -> Unit
        ): TileEntityMenu = from {
            windowDsl(it) { 
                title by tileEntity.block.name 
                window()
            } 
        }
        
    }
    
}

private open class TileEntityMenuImpl : TileEntityMenu {
    
    private val registeredWindows = weakHashSet<Window>()
    private val openWindows = HashSet<Window>()
    
    override fun register(window: Window) {
        if (registeredWindows.add(window)) {
            window.addOpenHandler {
                openWindows += window
            }
            window.addCloseHandler {
                openWindows -= window
            }
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