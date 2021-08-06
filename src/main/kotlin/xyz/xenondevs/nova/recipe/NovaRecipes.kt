package xyz.xenondevs.nova.recipe

import de.studiocode.invui.item.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.inventory.*
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.contentEquals
import xyz.xenondevs.nova.util.novaMaterial

private fun findName(itemStack: ItemStack) =
    itemStack.novaMaterial?.name ?: itemStack.type.name

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
        val resultStack = result.build()
        
        val recipe = ShapedRecipe(key, resultStack)
        recipe.shape(*shape.toTypedArray())
        ingredientMap.forEach { (key, material) -> recipe.setIngredient(key, material) }
        
        RecipeManager.recipes += key
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
        val resultStack = result.build()
        
        val recipe = ShapelessRecipe(key, resultStack)
        
        ingredientMap.forEach { (material, count) ->
            var amountLeft = count
            while (amountLeft-- > 0) {
                recipe.addIngredient(material)
            }
        }
        
        RecipeManager.recipes += key
        RecipeManager.shapelessRecipes += recipe
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

/**
 * The superclass for all Nova recipes that are not registered via Bukkit's Recipe API.
 *
 * The hashCode of this object is calculated during initialization.
 *
 * DO NOT EDIT INPUT STACKS OR THE RESULT STACK AFTER CREATING A NEW INSTANCE OF THIS CLASS.
 */
abstract class ConversionNovaRecipe(input: List<ItemBuilder>, result: ItemBuilder) : NovaRecipe {
    
    val inputStacks: List<ItemStack> = input.map(ItemBuilder::build)
    val resultStack: ItemStack = result.build()
    
    private val hashCode: Int
    
    init {
        val hashCode = inputStacks.hashCode()
        this.hashCode = 31 * hashCode + resultStack.hashCode()
    }
    
    override fun equals(other: Any?): Boolean {
        return other is ConversionNovaRecipe && inputStacks.contentEquals(other.inputStacks) && resultStack == other.resultStack
    }
    
    override fun hashCode() = hashCode
    
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