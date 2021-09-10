package xyz.xenondevs.nova.data.recipe

import de.studiocode.invui.item.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.inventory.*
import xyz.xenondevs.nova.util.contentEquals
import xyz.xenondevs.nova.util.novaMaterial

interface NovaRecipe {
    fun register()
}

class ShapedNovaRecipe(
    private val key: NamespacedKey,
    private val result: ItemBuilder,
    private val shape: List<String>,
    private val ingredientMap: Map<Char, RecipeChoice>
) : NovaRecipe {
    
    override fun register() {
        val resultStack = result.get()
        
        val recipe = ShapedRecipe(key, resultStack)
        recipe.shape(*shape.toTypedArray())
        ingredientMap.forEach { (key, material) -> recipe.setIngredient(key, material) }
        
        RecipeManager.vanillaRegisteredRecipeKeys += key
        RecipeManager.shapedRecipes += OptimizedShapedRecipe(recipe)
        Bukkit.getServer().addRecipe(recipe)
    }
    
}

class ShapelessNovaRecipe(
    private val key: NamespacedKey,
    private val result: ItemBuilder,
    private val ingredientMap: Map<RecipeChoice, Int>
) : NovaRecipe {
    
    override fun register() {
        val resultStack = result.get()
        
        val recipe = ShapelessRecipe(key, resultStack)
        
        ingredientMap.forEach { (material, count) ->
            var amountLeft = count
            while (amountLeft-- > 0) {
                recipe.addIngredient(material)
            }
        }
        
        RecipeManager.vanillaRegisteredRecipeKeys += key
        RecipeManager.shapelessRecipes += recipe
        Bukkit.getServer().addRecipe(recipe)
    }
    
}

class FurnaceNovaRecipe(
    private val key: NamespacedKey,
    private val input: RecipeChoice,
    private val result: ItemBuilder,
    private val experience: Float,
    private val cookingTime: Int
) : NovaRecipe {
    
    override fun register() {
        val resultStack = result.get()
        
        val recipe = FurnaceRecipe(key, resultStack, input, experience, cookingTime)
        
        RecipeManager.vanillaRegisteredRecipeKeys += key
        Bukkit.getServer().addRecipe(recipe)
    }
    
}

/**
 * The superclass for all Nova recipes that are not registered via Bukkit's Recipe API.
 *
 * The hashCode of this object is calculated during initialization.
 * Modifying the input or result stacks after initialization will cause issues.
 */
abstract class CustomNovaRecipe(
    val key: NamespacedKey,
    input: List<ItemBuilder>,
    result: ItemBuilder,
    val time: Int
) : NovaRecipe {
    
    val inputStacks: List<ItemStack> = input.map(ItemBuilder::get)
    val resultStack: ItemStack = result.get()
    
    private val hashCode: Int
    
    init {
        var hashCode = time
        hashCode = 31 * hashCode + inputStacks.hashCode()
        this.hashCode = 31 * hashCode + resultStack.hashCode()
    }
    
    override fun equals(other: Any?): Boolean {
        return other is CustomNovaRecipe
            && inputStacks.contentEquals(other.inputStacks)
            && resultStack == other.resultStack
            && time == other.time
    }
    
    override fun hashCode(): Int {
        var result = time
        result = 31 * result + inputStacks.hashCode()
        result = 31 * result + resultStack.hashCode()
        return result
    }
    
}

class PulverizerNovaRecipe(
    key: NamespacedKey,
    input: List<ItemBuilder>,
    result: ItemBuilder,
    time: Int
) : CustomNovaRecipe(key, input, result, time) {
    
    override fun register() {
        RecipeManager.pulverizerRecipes[key] = this
    }
    
}

class PlatePressNovaRecipe(
    key: NamespacedKey,
    input: List<ItemBuilder>,
    result: ItemBuilder,
    time: Int
) : CustomNovaRecipe(key, input, result, time) {
    
    override fun register() {
        RecipeManager.platePressRecipes[key] = this
    }
    
}

class GearPressNovaRecipe(
    key: NamespacedKey,
    input: List<ItemBuilder>,
    result: ItemBuilder,
    time: Int
) : CustomNovaRecipe(key, input, result, time) {
    
    override fun register() {
        RecipeManager.gearPressRecipes[key] = this
    }
    
}