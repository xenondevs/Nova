package xyz.xenondevs.nova.ui.menu.item.recipes.group

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.item.ItemProvider
import xyz.xenondevs.nova.data.recipe.RecipeContainer
import xyz.xenondevs.nova.ui.overlay.character.gui.GUITexture

abstract class RecipeGroup : Comparable<RecipeGroup> {
    
    private val guiCache = HashMap<RecipeContainer, GUI>()
    
    abstract val priority: Int
    abstract val texture: GUITexture
    abstract val icon: ItemProvider
    
    protected abstract fun createGUI(container: RecipeContainer): GUI
    
    fun getGUI(container: RecipeContainer): GUI {
        return guiCache.getOrPut(container) { createGUI(container) }
    }
    
    fun invalidateCache() {
        guiCache.clear()
    }
    
    override fun compareTo(other: RecipeGroup): Int {
        return this.priority.compareTo(other.priority)
    }
    
}
