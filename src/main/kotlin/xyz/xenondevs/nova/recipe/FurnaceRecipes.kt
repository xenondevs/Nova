package xyz.xenondevs.nova.recipe

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.BlastingRecipe
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice.MaterialChoice
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

// TODO: fix shift clicking
object FurnaceRecipes {
    
    fun registerRecipes() {
        addOreRecipe("nova.smeltIronDust", NovaMaterial.IRON_DUST, ItemStack(Material.IRON_INGOT, 1))
        addOreRecipe("nova.smeltGoldDust", NovaMaterial.GOLD_DUST, ItemStack(Material.GOLD_INGOT, 1))
        addOreRecipe("nova.smeltDiamondDust", NovaMaterial.DIAMOND_DUST, ItemStack(Material.DIAMOND, 1))
        addOreRecipe("nova.smeltNetheriteDust", NovaMaterial.NETHERITE_DUST, ItemStack(Material.NETHERITE_INGOT, 1))
        addOreRecipe("nova.smeltEmeraldDust", NovaMaterial.EMERALD_DUST, ItemStack(Material.EMERALD, 1))
        addOreRecipe("nova.smeltLapisDust", NovaMaterial.LAPIS_DUST, ItemStack(Material.LAPIS_LAZULI, 1))
        addOreRecipe("nova.smeltCoalDust", NovaMaterial.COAL_DUST, ItemStack(Material.COAL, 1))
    }
    
    fun addFurnaceRecipe(name: String, input: NovaMaterial, result: ItemStack, xp: Float, burnTicks: Int) {
        val recipe = FurnaceRecipe(NamespacedKey(NOVA, name), result, NovaRecipeChoice(input), xp, burnTicks)
        NOVA.server.addRecipe(recipe)
    }
    
    fun addFurnaceRecipe(name: String, input: Material, result: ItemStack, xp: Float, burnTicks: Int) {
        val recipe = FurnaceRecipe(NamespacedKey(NOVA, name), result, MaterialChoice(input), xp, burnTicks)
        NOVA.server.addRecipe(recipe)
    }
    
    fun addBlastingRecipe(name: String, input: NovaMaterial, result: ItemStack, xp: Float, burnTicks: Int) {
        val recipe = BlastingRecipe(NamespacedKey(NOVA, "$name.blasting"), result, NovaRecipeChoice(input), xp, burnTicks)
        NOVA.server.addRecipe(recipe)
    }
    
    fun addBlastingRecipe(name: String, input: Material, result: ItemStack, xp: Float, burnTicks: Int) {
        val recipe = BlastingRecipe(NamespacedKey(NOVA, "$name.blasting"), result, MaterialChoice(input), xp, burnTicks)
        NOVA.server.addRecipe(recipe)
    }
    
    fun addOreRecipe(name: String, input: NovaMaterial, result: ItemStack) {
        addFurnaceRecipe(name, input, result, 1f, 200)
        addBlastingRecipe("$name.blasting", input, result, 1f, 100)
    }
    
    fun addOreRecipe(name: String, input: Material, result: ItemStack) {
        addFurnaceRecipe(name, input, result, 1f, 200)
        addBlastingRecipe("$name.blasting", input, result, 1f, 100)
    }
    
}