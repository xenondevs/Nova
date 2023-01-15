package xyz.xenondevs.nova.ui.menu.item.recipes.group

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.item.ItemProvider
import xyz.xenondevs.nova.ui.overlay.character.gui.GUITexture

abstract class RecipeGroup<T: Any> : Comparable<RecipeGroup<*>> {
    
    private val guiCache = HashMap<T, GUI>()
    
    abstract val priority: Int
    abstract val texture: GUITexture
    abstract val icon: ItemProvider
    
    protected abstract fun createGUI(recipe: T): GUI
    
    fun getGUI(recipe: T): GUI {
        return guiCache.getOrPut(recipe) { createGUI(recipe) }
    }
    
    internal fun invalidateCache() {
        guiCache.clear()
    }
    
    override fun compareTo(other: RecipeGroup<*>): Int {
        return this.priority.compareTo(other.priority)
    }
    
}
