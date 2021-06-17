package xyz.xenondevs.nova.recipe

import de.studiocode.invui.item.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.inventory.*
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.novaMaterial

private fun findName(itemStack: ItemStack) =
    itemStack.novaMaterial?.name ?: itemStack.type.name

interface NovaRecipe {
    
    fun register()
    
}

class ShapedNovaRecipe(
    private val result: ItemBuilder,
    private val shape: List<String>,
    private val ingredientMap: Map<Char, RecipeChoice>
) : NovaRecipe {
    
    override fun register() {
        val resultStack = result.build()
        
        val key = NamespacedKey(NOVA, "shapedRecipe.${findName(resultStack)}")
        val recipe = ShapedRecipe(key, resultStack)
        recipe.shape(*shape.toTypedArray())
        ingredientMap.forEach { (key, material) -> recipe.setIngredient(key, material) }
        
        RecipeManager.recipes += key
        Bukkit.getServer().addRecipe(recipe)
    }
    
}

class ShapelessNovaRecipe(
    private val result: ItemBuilder,
    private val ingredientMap: Map<RecipeChoice, Int>
) : NovaRecipe {
    
    override fun register() {
        val resultStack = result.build()
        
        val key = NamespacedKey(NOVA, "shapelessRecipe.${findName(resultStack)}")
        val recipe = ShapelessRecipe(key, resultStack)
        
        ingredientMap.forEach { (material, count) ->
            var amountLeft = count
            while (amountLeft-- > 0) {
                recipe.addIngredient(material)
            }
        }
        
        RecipeManager.recipes += key
        Bukkit.getServer().addRecipe(recipe)
    }
    
}

class FurnaceNovaRecipe(
    private val input: RecipeChoice,
    private val result: ItemBuilder,
    private val experience: Float,
    private val cookingTime: Int
) : NovaRecipe {
    
    override fun register() {
        val resultStack = result.build()
        
        val key = NamespacedKey(NOVA, "furnaceRecipe.${findName(resultStack)}")
        val recipe = FurnaceRecipe(key, resultStack, input, experience, cookingTime)
        
        RecipeManager.recipes += key
        Bukkit.getServer().addRecipe(recipe)
    }
    
}

abstract class ConversionNovaRecipe(input: List<ItemBuilder>, result: ItemBuilder) : NovaRecipe {
    
    val inputStacks: List<ItemStack> = input.map(ItemBuilder::build)
    val resultStack: ItemStack = result.build()
    
}

class PulverizerNovaRecipe(input: List<ItemBuilder>, result: ItemBuilder) : ConversionNovaRecipe(input, result) {
    
    override fun register() {
        RecipeManager.pulverizerRecipes += this
    }
    
}

class PlatePressNovaRecipe(input: List<ItemBuilder>, result: ItemBuilder) : ConversionNovaRecipe(input, result) {
    
    override fun register() {
        RecipeManager.platePressRecipes += this
    }
    
}

class GearPressNovaRecipe(input: List<ItemBuilder>, result: ItemBuilder) : ConversionNovaRecipe(input, result) {
    
    override fun register() {
        RecipeManager.gearPressRecipes += this
    }
    
}