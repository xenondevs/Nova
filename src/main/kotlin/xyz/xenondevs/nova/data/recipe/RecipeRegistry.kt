package xyz.xenondevs.nova.data.recipe

import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.inventory.*
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.command.CommandManager
import xyz.xenondevs.nova.command.impl.NovaRecipeCommand
import xyz.xenondevs.nova.ui.menu.item.recipes.craftingtype.RecipeType
import xyz.xenondevs.nova.util.data.plus
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.nova.util.runTask
import java.util.stream.Stream

object RecipeRegistry {
    
    private var BUKKIT_RECIPES: List<Recipe> = ArrayList()
    
    var CREATION_RECIPES: Map<String, Map<RecipeType, List<RecipeContainer>>> = HashMap()
        private set
    var USAGE_RECIPES: Map<String, Map<RecipeType, Set<RecipeContainer>>> = HashMap()
        private set
    
    fun init() {
        runAsyncTask {
            LOGGER.info("Initializing recipe registry")
            BUKKIT_RECIPES = loadBukkitRecipes()
            CREATION_RECIPES = loadCreationRecipes()
            USAGE_RECIPES = loadUsageRecipes()
            LOGGER.info("Finished initializing recipe registry")
            
            runTask {
                CommandManager.registerCommand(NovaRecipeCommand)
            }
        }
    }
    
    private fun loadBukkitRecipes(): List<Recipe> {
        val list = ArrayList<Recipe>()
        Bukkit.recipeIterator().forEachRemaining(list::add)
        return list
    }
    
    private fun loadCreationRecipes(): Map<String, Map<RecipeType, List<RecipeContainer>>> {
        val map = HashMap<String, HashMap<RecipeType, MutableList<RecipeContainer>>>()
        
        // add all with bukkit registered recipes
        getBukkitRecipeStream().forEach {
            val itemKey = getNameKey(it.result)
            map.getOrPut(itemKey) { hashMapOf() }
                .getOrPut(RecipeType.of(it)) { mutableListOf() }
                .add(RecipeContainer(it))
        }
        
        // add all nova machine recipes
        getNovaRecipeStream().forEach {
            val itemKey = getNameKey(it.resultStack)
            map.getOrPut(itemKey) { hashMapOf() }
                .getOrPut(RecipeType.of(it)) { mutableListOf() }
                .add(RecipeContainer(it))
        }
        
        return map
    }
    
    private fun loadUsageRecipes(): Map<String, Map<RecipeType, Set<RecipeContainer>>> {
        val map = HashMap<String, HashMap<RecipeType, HashSet<RecipeContainer>>>()
        
        // add all with bukkit registered recipes
        getBukkitRecipeStream().forEach { recipe ->
            recipe.getInputStacks().forEach { inputStack ->
                val itemKey = getNameKey(inputStack)
                map.getOrPut(itemKey) { hashMapOf() }
                    .getOrPut(RecipeType.of(recipe)) { LinkedHashSet() }
                    .add(RecipeContainer(recipe))
            }
        }
        
        // add all nova machine recipes
        getNovaRecipeStream().forEach { recipe ->
            recipe.inputStacks.forEach { inputStack ->
                val itemKey = getNameKey(inputStack)
                map.getOrPut(itemKey) { hashMapOf() }
                    .getOrPut(RecipeType.of(recipe)) { LinkedHashSet() }
                    .add(RecipeContainer(recipe))
            }
        }
        
        return map
    }
    
    private fun getBukkitRecipeStream(): Stream<Recipe> {
        return BUKKIT_RECIPES.stream()
            .filter {
                val namespace = (it as Keyed).key.namespace
                (namespace == "minecraft" || namespace == "nova") // do not allow recipes from different plugins to show up
                    && (it is ShapedRecipe || it is ShapelessRecipe || it is FurnaceRecipe) // TODO: allow more recipe types
            }
    }
    
    private fun getNovaRecipeStream(): Stream<out ConversionNovaRecipe> {
        return (RecipeManager.pulverizerRecipes.stream()
            + RecipeManager.platePressRecipes.stream()
            + RecipeManager.gearPressRecipes.stream())
    }
    
    private fun Recipe.getInputStacks(): List<ItemStack> =
        when (this) {
            
            is ShapedRecipe -> choiceMap.values.mapNotNull { choice -> choice?.getInputStacks() }.flatten()
            is ShapelessRecipe -> choiceList.map { it.getInputStacks() }.flatten()
            is FurnaceRecipe -> inputChoice.getInputStacks()
            
            else -> throw UnsupportedOperationException("Unsupported Recipe type: ${this.javaClass.name}")
        }
    
    
    private fun RecipeChoice.getInputStacks(): List<ItemStack> =
        when (this) {
            is RecipeChoice.MaterialChoice -> choices.map(::ItemStack)
            is RecipeChoice.ExactChoice -> choices
            else -> throw UnsupportedOperationException("Unknown RecipeChoice type: ${this.javaClass.name}")
        }
    
    
    fun getNameKey(itemStack: ItemStack): String {
        val novaMaterial = itemStack.novaMaterial
        
        return if (novaMaterial != null) "nova:${novaMaterial.typeName.lowercase()}"
        else "minecraft:${itemStack.type.name.lowercase()}"
    }
    
}

class RecipeContainer(val recipe: Any) {
    
    val result = if (recipe is Recipe) recipe.result else (recipe as ConversionNovaRecipe).resultStack
    val isCraftingRecipe = recipe is ShapedRecipe || recipe is ShapelessRecipe
    val isSmeltingRecipe = recipe is FurnaceRecipe
    val isPulverizingRecipe = recipe is PulverizerNovaRecipe
    val isPressingRecipe = recipe is GearPressNovaRecipe || recipe is PlatePressNovaRecipe
    
    override fun equals(other: Any?): Boolean {
        if (other is RecipeContainer) {
            val otherRecipe = other.recipe
            return if (recipe is Recipe && otherRecipe is Recipe) {
                (recipe as Keyed).key.toString() == (otherRecipe as Keyed).key.toString()
            } else recipe == otherRecipe
        }
        
        return this === other
    }
    
    override fun hashCode(): Int {
        return if (recipe is Recipe) (recipe as Keyed).key.hashCode()
        else recipe.hashCode()
    }
    
}
