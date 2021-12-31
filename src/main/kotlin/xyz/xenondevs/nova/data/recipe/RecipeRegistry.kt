package xyz.xenondevs.nova.data.recipe

import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.inventory.*
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.command.CommandManager
import xyz.xenondevs.nova.command.impl.NovaRecipeCommand
import xyz.xenondevs.nova.command.impl.NovaUsageCommand
import xyz.xenondevs.nova.ui.menu.item.recipes.craftingtype.RecipeGroup
import xyz.xenondevs.nova.util.data.getInputStacks
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.nova.util.runTask

object RecipeRegistry {
    
    private var BUKKIT_RECIPES: List<Recipe> = ArrayList()
    
    var CREATION_RECIPES: Map<String, Map<RecipeGroup, List<RecipeContainer>>> = HashMap()
        private set
    var USAGE_RECIPES: Map<String, Map<RecipeGroup, Set<RecipeContainer>>> = HashMap()
        private set
    var RECIPES_BY_TYPE: Map<RecipeGroup, List<RecipeContainer>> = HashMap()
    
    fun init() {
        runAsyncTask {
            LOGGER.info("Initializing recipe registry")
            BUKKIT_RECIPES = loadBukkitRecipes()
            CREATION_RECIPES = loadCreationRecipes()
            USAGE_RECIPES = loadUsageRecipes()
            RECIPES_BY_TYPE = loadRecipesByGroup()
            LOGGER.info("Finished initializing recipe registry")
            
            runTask {
                CommandManager.registerCommand(NovaRecipeCommand)
                CommandManager.registerCommand(NovaUsageCommand)
            }
        }
    }
    
    private fun loadBukkitRecipes(): List<Recipe> {
        val list = ArrayList<Recipe>()
        Bukkit.recipeIterator().forEachRemaining(list::add)
        return list
    }
    
    private fun loadCreationRecipes(): Map<String, Map<RecipeGroup, List<RecipeContainer>>> {
        val map = HashMap<String, HashMap<RecipeGroup, MutableList<RecipeContainer>>>()
        
        // add all with bukkit registered recipes
        getBukkitRecipeSequence().forEach {
            val itemKey = getNameKey(it.result)
            map.getOrPut(itemKey) { hashMapOf() }
                .getOrPut(RecipeType.of(it).group) { mutableListOf() }
                .add(RecipeContainer(it))
        }
        
        // add all nova machine recipes
        getConversionNovaRecipeSequence().forEach {
            val itemKey = getNameKey(it.result)
            map.getOrPut(itemKey) { hashMapOf() }
                .getOrPut(RecipeType.of(it).group) { mutableListOf() }
                .add(RecipeContainer(it))
        }
        
        return map
    }
    
    private fun loadUsageRecipes(): Map<String, Map<RecipeGroup, Set<RecipeContainer>>> {
        val map = HashMap<String, HashMap<RecipeGroup, HashSet<RecipeContainer>>>()
        
        // add all with bukkit registered recipes
        getBukkitRecipeSequence().forEach { recipe ->
            val group = RecipeType.of(recipe).group
            recipe.getInputStacks().forEach { inputStack ->
                val itemKey = getNameKey(inputStack)
                map.getOrPut(itemKey) { hashMapOf() }
                    .getOrPut(group) { LinkedHashSet() }
                    .add(RecipeContainer(recipe))
            }
        }
        
        // add all nova machine recipes
        getConversionNovaRecipeSequence().forEach { recipe ->
            val group = RecipeType.of(recipe).group
            recipe.input.getInputStacks().forEach { inputStack ->
                val itemKey = getNameKey(inputStack)
                map.getOrPut(itemKey) { hashMapOf() }
                    .getOrPut(group) { LinkedHashSet() }
                    .add(RecipeContainer(recipe))
            }
        }
        
        return map
    }
    
    private fun loadRecipesByGroup(): Map<RecipeGroup, List<RecipeContainer>> {
        val map = HashMap<RecipeGroup, MutableList<RecipeContainer>>()
        (getBukkitRecipeSequence() + getConversionNovaRecipeSequence()).forEach {
            map.getOrPut(RecipeType.of(it).group) { ArrayList() } += RecipeContainer(it)
        }
        return map
    }
    
    private fun getBukkitRecipeSequence(): Sequence<Recipe> {
        return BUKKIT_RECIPES.asSequence()
            .filter {
                val namespace = (it as Keyed).key.namespace
                (namespace == "minecraft" || namespace == "nova") // do not allow recipes from different plugins to show up
                    && (it is ShapedRecipe || it is ShapelessRecipe || it is FurnaceRecipe || it is StonecuttingRecipe || it is SmithingRecipe)
            }
    }
    
    private fun getConversionNovaRecipeSequence(): Sequence<ConversionNovaRecipe> {
        return RecipeManager.novaRecipes
            .values.asSequence()
            .flatMap { it.values }
            .filterIsInstance<ConversionNovaRecipe>()
    }
    
    fun getNameKey(itemStack: ItemStack): String {
        val novaMaterial = itemStack.novaMaterial
        
        return if (novaMaterial != null) "nova:${novaMaterial.typeName.lowercase()}"
        else "minecraft:${itemStack.type.name.lowercase()}"
    }
    
}

