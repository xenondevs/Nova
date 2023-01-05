package xyz.xenondevs.nova.data.recipe

import org.bukkit.Bukkit
import org.bukkit.inventory.Recipe
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.ui.menu.item.recipes.group.RecipeGroup
import xyz.xenondevs.nova.util.data.getInputStacks
import xyz.xenondevs.nova.util.item.ItemUtils.getId
import kotlin.reflect.full.isSuperclassOf

object RecipeRegistry : Initializable() {
    
    private var BUKKIT_RECIPES: List<Recipe> = ArrayList()
    
    var CREATION_RECIPES: Map<String, Map<RecipeGroup, List<RecipeContainer>>> = HashMap()
        private set
    var USAGE_RECIPES: Map<String, Map<RecipeGroup, Set<RecipeContainer>>> = HashMap()
        private set
    var RECIPES_BY_TYPE: Map<RecipeGroup, List<RecipeContainer>> = HashMap()
    
    override val initializationStage = InitializationStage.POST_WORLD_ASYNC
    override val dependsOn = setOf(RecipeManager)
    
    private val hardcodedRecipes = ArrayList<NovaRecipe>()
    val creationInfo = HashMap<String, String>()
    val usageInfo = HashMap<String, String>()
    
    fun addHardcodedRecipes(recipes: List<NovaRecipe>) {
        check(!isInitialized) { "Recipes are already initialized" }
        hardcodedRecipes += recipes
    }
    
    fun addCreationInfo(info: Map<String, String>) {
        creationInfo += info
    }
    
    fun addUsageInfo(info: Map<String, String>) {
        usageInfo += info
    }
    
    override fun init() {
        LOGGER.info("Indexing recipes")
        BUKKIT_RECIPES = loadBukkitRecipes()
        CREATION_RECIPES = loadCreationRecipes()
        USAGE_RECIPES = loadUsageRecipes()
        RECIPES_BY_TYPE = loadRecipesByGroup()
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
            val group = RecipeTypeRegistry.getType(it).group ?: return@forEach
            val itemKey = getId(it.result)
            map.getOrPut(itemKey) { hashMapOf() }
                .getOrPut(group) { mutableListOf() }
                .add(RecipeContainer(it))
        }
        
        // add all nova machine recipes
        getCreationNovaRecipeSequence().forEach {
            val group = RecipeTypeRegistry.getType(it).group ?: return@forEach
            val itemKey = getId(it.result)
            map.getOrPut(itemKey) { hashMapOf() }
                .getOrPut(group) { mutableListOf() }
                .add(RecipeContainer(it))
        }
        
        return map
    }
    
    private fun loadUsageRecipes(): Map<String, Map<RecipeGroup, Set<RecipeContainer>>> {
        val map = HashMap<String, HashMap<RecipeGroup, HashSet<RecipeContainer>>>()
        
        // add all with bukkit registered recipes
        getBukkitRecipeSequence().forEach { recipe ->
            val group = RecipeTypeRegistry.getType(recipe).group ?: return@forEach
            recipe.getInputStacks().forEach { inputStack ->
                val itemKey = getId(inputStack)
                map.getOrPut(itemKey) { hashMapOf() }
                    .getOrPut(group) { LinkedHashSet() }
                    .add(RecipeContainer(recipe))
            }
        }
        
        // add all nova machine recipes
        getUsageNovaRecipeSequence().forEach { recipe ->
            val group = RecipeTypeRegistry.getType(recipe).group ?: return@forEach
            recipe.getAllInputs().flatMap { it.getInputStacks() }.forEach { inputStack ->
                val itemKey = getId(inputStack)
                map.getOrPut(itemKey) { hashMapOf() }
                    .getOrPut(group) { LinkedHashSet() }
                    .add(RecipeContainer(recipe))
            }
        }
        
        return map
    }
    
    private fun loadRecipesByGroup(): Map<RecipeGroup, List<RecipeContainer>> {
        val map = HashMap<RecipeGroup, MutableList<RecipeContainer>>()
        (getBukkitRecipeSequence() + getAllNovaRecipes()).forEach {
            val group = RecipeTypeRegistry.getType(it).group ?: return@forEach
            map.getOrPut(group) { ArrayList() } += RecipeContainer(it)
        }
        return map
    }
    
    private fun getBukkitRecipeSequence(): Sequence<Recipe> {
        return BUKKIT_RECIPES.asSequence().filter { recipe -> 
            RecipeTypeRegistry.types.any { type -> type.recipeClass.isSuperclassOf(recipe::class) }
        }
    }
    
    private fun getAllNovaRecipes(): Sequence<NovaRecipe> {
        return RecipeManager.novaRecipes.values.asSequence().flatMap { it.values } + hardcodedRecipes.asSequence()
    }
    
    private fun getCreationNovaRecipeSequence(): Sequence<ResultingRecipe> {
        return getAllNovaRecipes().filterIsInstance<ResultingRecipe>()
    }
    
    private fun getUsageNovaRecipeSequence(): Sequence<InputChoiceRecipe> {
        return getAllNovaRecipes().filterIsInstance<InputChoiceRecipe>()
    }
    
}

