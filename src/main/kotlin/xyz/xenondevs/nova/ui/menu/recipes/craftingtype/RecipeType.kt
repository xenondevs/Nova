package xyz.xenondevs.nova.ui.menu.recipes.craftingtype

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.item.ItemBuilder
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import xyz.xenondevs.nova.overlay.NovaOverlay
import xyz.xenondevs.nova.recipe.GearPressNovaRecipe
import xyz.xenondevs.nova.recipe.PlatePressNovaRecipe
import xyz.xenondevs.nova.recipe.PulverizerNovaRecipe
import xyz.xenondevs.nova.recipe.RecipeContainer

abstract class RecipeType : Comparable<RecipeType> {
    
    private val guiCache = HashMap<RecipeContainer, GUI>()
    
    abstract val overlay: NovaOverlay
    abstract val icon: ItemBuilder
    abstract val priority: Int
    
    protected abstract fun createGUI(holder: RecipeContainer): GUI
    
    fun getGUI(holder: RecipeContainer): GUI {
        return guiCache.getOrPut(holder) { createGUI(holder) }
    }
    
    override fun compareTo(other: RecipeType): Int {
        return this.priority.compareTo(other.priority)
    }
    
    companion object {
        
        fun of(recipe: Any): RecipeType =
            when (recipe) {
                is ShapelessRecipe, is ShapedRecipe -> TableRecipeType
                is FurnaceRecipe -> SmeltingRecipeType
                is PulverizerNovaRecipe -> PulverizingRecipeType
                is PlatePressNovaRecipe, is GearPressNovaRecipe -> PressingRecipeType
                
                else -> throw UnsupportedOperationException("Unknown recipe class: ${recipe.javaClass.name}")
            }
        
    }
    
}
