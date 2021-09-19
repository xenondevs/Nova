package xyz.xenondevs.nova.ui.menu.item.recipes.craftingtype

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.ItemWrapper
import de.studiocode.invui.util.SlotUtils
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import xyz.xenondevs.nova.data.recipe.RecipeContainer
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.overlay.CustomCharacters
import xyz.xenondevs.nova.util.intValue

object TableRecipeType : RecipeType() {
    
    override val priority = 0
    override val overlay = CustomCharacters.CRAFTING_RECIPE
    override val icon = ItemWrapper(ItemStack(Material.CRAFTING_TABLE))
    
    override fun createGUI(holder: RecipeContainer): GUI {
        require(holder.isCraftingRecipe)
        val recipe = holder.recipe as Recipe
        
        val gui = GUIBuilder(GUIType.NORMAL, 9, 3)
            .setStructure("" +
                ". . . . . . . . ." +
                ". . . . . . . r ." +
                ". . . . . . . . .")
            .addIngredient('r', createRecipeChoiceItem(listOf(recipe.result)))
            .build()
        
        if (recipe is ShapedRecipe) {
            val shape = recipe.shape
            for ((rowNumber, row) in shape.withIndex()) {
                if (row.isBlank()) continue
                
                for ((charNumber, char) in row.toCharArray().withIndex()) {
                    val choiceItem = recipe.choiceMap[char]?.let(::createRecipeChoiceItem)
                    if (choiceItem != null) gui.setItem(
                        charNumber + 1 + (row.length == 1).intValue,
                        rowNumber + (shape.size < 3).intValue,
                        choiceItem
                    )
                }
            }
        } else if (recipe is ShapelessRecipe) {
            val choiceItems = recipe.choiceList.map(::createRecipeChoiceItem)
            if (choiceItems.size == 1) {
                gui.setItem(2, 1, choiceItems[0])
            } else {
                SlotUtils.getSlotsRect(1, 0, 3, 3, 9)
                    .take(choiceItems.size)
                    .withIndex()
                    .forEach { (index, slot) -> gui.setItem(slot, choiceItems[index]) }
            }
        }
        
        return gui
    }
    
}