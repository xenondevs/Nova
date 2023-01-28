package xyz.xenondevs.nova.ui.menu.item.recipes.group

import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.ui.overlay.character.gui.GUITexture

abstract class RecipeGroup<T : Any> : Comparable<RecipeGroup<*>> {
    
    private val guiCache = HashMap<T, Gui>()
    
    abstract val priority: Int
    abstract val texture: GUITexture
    abstract val icon: ItemProvider
    
    protected abstract fun createGUI(recipe: T): Gui
    
    fun getGUI(recipe: T): Gui {
        return guiCache.getOrPut(recipe) { createGUI(recipe) }
    }
    
    internal fun invalidateCache() {
        guiCache.clear()
    }
    
    override fun compareTo(other: RecipeGroup<*>): Int {
        return this.priority.compareTo(other.priority)
    }
    
}
