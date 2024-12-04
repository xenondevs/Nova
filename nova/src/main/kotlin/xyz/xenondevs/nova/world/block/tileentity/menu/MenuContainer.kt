package xyz.xenondevs.nova.world.block.tileentity.menu

import org.bukkit.entity.Player
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.block.tileentity.TileEntity.GlobalTileEntityMenu
import xyz.xenondevs.nova.world.block.tileentity.TileEntity.IndividualTileEntityMenu
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.isAccessible

// TODO: simplify this and make everything individual menus

abstract class MenuContainer internal constructor() {
    
    private val openWindows = ArrayList<Window>()
    
    fun registerWindow(window: Window) {
        window.addOpenHandler { openWindows += window }
        window.addCloseHandler {
            openWindows -= window
            handleClosed(window.viewer!!)
        }
    }
    
    fun closeWindows() {
        openWindows.toList().forEach(Window::close)
    }
    
    inline fun <reified T : TileEntity.TileEntityMenu> getMenus(): Sequence<T> {
        return getMenusInternal().filterIsInstance<T>()
    }
    
    inline fun <reified T : TileEntity.TileEntityMenu> forEachMenu(action: (T) -> Unit) {
        getMenus<T>().forEach(action)
    }
    
    abstract fun openWindow(player: Player)
    
    protected abstract fun handleClosed(player: Player)
    
    @PublishedApi
    internal abstract fun getMenusInternal(): Sequence<TileEntity.TileEntityMenu>
    
    companion object {
        
        @Suppress("UNCHECKED_CAST")
        fun of(tileEntity: TileEntity, clazz: KClass<*>): MenuContainer {
            val constructor = clazz.constructors.first()
            constructor.isAccessible = true
            
            return when {
                clazz.isSubclassOf(GlobalTileEntityMenu::class) -> GlobalMenuContainer(tileEntity, constructor as KFunction<GlobalTileEntityMenu>)
                clazz.isSubclassOf(IndividualTileEntityMenu::class) -> IndividualMenuContainer(tileEntity, constructor as KFunction<IndividualTileEntityMenu>)
                else -> throw UnsupportedOperationException("TileEntityMenu must extend either GlobalTileEntityMenu or IndividualTileEntityMenu")
            }
        }
        
    }
    
}

internal class IndividualMenuContainer internal constructor(
    private val tileEntity: TileEntity,
    private val ctor: KFunction<IndividualTileEntityMenu>
) : MenuContainer() {
    
    private val menus = WeakHashMap<Player, IndividualTileEntityMenu>()
    
    init {
        val params = ctor.parameters
        require(params.size == 2
            && params[0].kind == KParameter.Kind.INSTANCE
            && params[1].kind == KParameter.Kind.VALUE && params[1].type.classifier == Player::class
        ) { "The given constructor is not a constructor of an inner class of a TileEntity with a Player parameter" }
    }
    
    override fun openWindow(player: Player) {
        menus.getOrPut(player) { ctor.call(tileEntity, player) }.openWindow()
    }
    
    override fun handleClosed(player: Player) {
        menus.remove(player)
    }
    
    override fun getMenusInternal(): Sequence<TileEntity.TileEntityMenu> {
        return menus.values.asSequence()
    }
    
}

internal class GlobalMenuContainer internal constructor(
    private val tileEntity: TileEntity,
    private val ctor: KFunction<GlobalTileEntityMenu>
) : MenuContainer() {
    
    private var menu: GlobalTileEntityMenu? = null
    private var viewers = 0
    
    init {
        val params = ctor.parameters
        require(params.size == 1
            && params[0].kind == KParameter.Kind.INSTANCE
        ) { "The given constructor is not a constructor of an inner class of a TileEntity" }
    }
    
    override fun openWindow(player: Player) {
        menu = ctor.call(tileEntity)
            .also { it.openWindow(player) }
        viewers++
    }
    
    override fun handleClosed(player: Player) {
        viewers--
        if (viewers <= 0) {
            menu = null
        }
    }
    
    override fun getMenusInternal(): Sequence<TileEntity.TileEntityMenu> {
        val menu = menu
        return if (menu != null) sequenceOf(menu) else emptySequence()
    }
    
}