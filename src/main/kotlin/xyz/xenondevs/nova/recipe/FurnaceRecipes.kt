package xyz.xenondevs.nova.recipe

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice.MaterialChoice
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

object FurnaceRecipes {
    
    fun registerRecipes() {
    
    }
    
    fun addRecipe(name: String, input: NovaMaterial, result: ItemStack, xp: Float, burnTicks: Int) {
        val recipe = FurnaceRecipe(NamespacedKey(NOVA, name), result, NovaRecipeChoice(input), xp, burnTicks)
        NOVA.server.addRecipe(recipe)
    }
    
    fun addRecipe(name: String, input: Material, result: ItemStack, xp: Float, burnTicks: Int) {
        val recipe = FurnaceRecipe(NamespacedKey(NOVA, name), result, MaterialChoice(input), xp, burnTicks)
        NOVA.server.addRecipe(recipe)
    }
    
}