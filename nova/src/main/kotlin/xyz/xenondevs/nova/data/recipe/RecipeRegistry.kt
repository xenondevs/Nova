package xyz.xenondevs.nova.data.recipe

import org.bukkit.Bukkit
import org.bukkit.inventory.Recipe
import xyz.xenondevs.nova.initialize.Dispatcher
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistries.RECIPE_TYPE
import xyz.xenondevs.nova.ui.menu.explorer.recipes.group.RecipeGroup
import xyz.xenondevs.nova.util.data.getInputStacks
import xyz.xenondevs.nova.util.item.ItemUtils.getId
import kotlin.reflect.full.isSuperclassOf

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dispatcher = Dispatcher.ASYNC,
    dependsOn = [RecipeManager::class]
)
object RecipeRegistry {
    
    private var BUKKIT_RECIPES: List<Recipe> = ArrayList()
    private var isInitialized = false
    
    var CREATION_RECIPES: Map<String, Map<RecipeGroup<*>, List<RecipeContainer>>> = HashMap()
        private set
    var USAGE_RECIPES: Map<String, Map<RecipeGroup<*>, Set<RecipeContainer>>> = HashMap()
        private set
    var RECIPES_BY_TYPE: Map<RecipeGroup<*>, List<RecipeContainer>> = HashMap()
    
    private val fakeRecipes = ArrayList<NovaRecipe>()
    val creationInfo = HashMap<String, String>()
    val usageInfo = HashMap<String, String>()
    
    @Deprecated("Misleading name, does not register any recipes.", ReplaceWith("addFakeRecipes(recipes)"))
    fun addHardcodedRecipes(recipes: List<NovaRecipe>) = addFakeRecipes(recipes)
    
    fun addFakeRecipes(recipes: List<NovaRecipe>) {
        check(!isInitialized) { "Recipes are already initialized" }
        fakeRecipes += recipes
    }
    
    fun addFakeRecipe(recipe: NovaRecipe) {
        check(!isInitialized) { "Recipes are already initialized" }
        fakeRecipes += recipe
    }
    
    fun addCreationInfo(info: Map<String, String>) {
        creationInfo += info
    }
    
    fun addUsageInfo(info: Map<String, String>) {
        usageInfo += info
    }
    
    @InitFun
    internal fun indexRecipes() {
        BUKKIT_RECIPES = loadBukkitRecipes()
        CREATION_RECIPES = loadCreationRecipes()
        USAGE_RECIPES = loadUsageRecipes()
        RECIPES_BY_TYPE = loadRecipesByGroup()
        isInitialized = true
    }
    
    private fun loadBukkitRecipes(): List<Recipe> {
        val list = ArrayList<Recipe>()
        Bukkit.recipeIterator().forEachRemaining(list::add)
        return list
    }
    
    private fun loadCreationRecipes(): Map<String, Map<RecipeGroup<*>, List<RecipeContainer>>> {
        val map = HashMap<String, HashMap<RecipeGroup<*>, MutableList<RecipeContainer>>>()
        
        // add all with bukkit registered recipes
        getBukkitRecipeSequence().forEach {
            val group = RecipeType.of(it)?.group ?: return@forEach
            val itemKey = getId(it.result)
            map.getOrPut(itemKey) { hashMapOf() }
                .getOrPut(group) { mutableListOf() }
                .add(RecipeContainer(it))
        }
        
        // add all nova machine recipes
        getCreationNovaRecipeSequence().forEach { recipe ->
            val group = RecipeType.of(recipe)?.group ?: return@forEach
            recipe.getAllResults().forEach { resultStack ->
                val itemKey = getId(resultStack)
                map.getOrPut(itemKey) { hashMapOf() }
                    .getOrPut(group) { mutableListOf() }
                    .add(RecipeContainer(recipe))
            }
        }
        
        return map
    }
    
    private fun loadUsageRecipes(): Map<String, Map<RecipeGroup<*>, Set<RecipeContainer>>> {
        val map = HashMap<String, HashMap<RecipeGroup<*>, HashSet<RecipeContainer>>>()
        
        // add all with bukkit registered recipes
        getBukkitRecipeSequence().forEach { recipe ->
            val group = RecipeType.of(recipe)?.group ?: return@forEach
            recipe.getInputStacks().forEach { inputStack ->
                val itemKey = getId(inputStack)
                map.getOrPut(itemKey) { hashMapOf() }
                    .getOrPut(group) { LinkedHashSet() }
                    .add(RecipeContainer(recipe))
            }
        }
        
        // add all nova machine recipes
        getUsageNovaRecipeSequence().forEach { recipe ->
            val group = RecipeType.of(recipe)?.group ?: return@forEach
            recipe.getAllInputs().flatMap { it.getInputStacks() }.forEach { inputStack ->
                val itemKey = getId(inputStack)
                map.getOrPut(itemKey) { hashMapOf() }
                    .getOrPut(group) { LinkedHashSet() }
                    .add(RecipeContainer(recipe))
            }
        }
        
        return map
    }
    
    private fun loadRecipesByGroup(): Map<RecipeGroup<*>, List<RecipeContainer>> {
        val map = HashMap<RecipeGroup<*>, MutableList<RecipeContainer>>()
        (getBukkitRecipeSequence() + getAllNovaRecipes()).forEach {
            val group = RecipeType.of(it)?.group ?: return@forEach
            map.getOrPut(group) { ArrayList() } += RecipeContainer(it)
        }
        return map
    }
    
    private fun getBukkitRecipeSequence(): Sequence<Recipe> {
        return BUKKIT_RECIPES.asSequence().filter { recipe ->
            RECIPE_TYPE.any { type -> type.recipeClass.isSuperclassOf(recipe::class) }
        }
    }
    
    private fun getAllNovaRecipes(): Sequence<NovaRecipe> {
        return RecipeManager.novaRecipes.values.asSequence().flatMap { it.values } + fakeRecipes.asSequence()
    }
    
    private fun getCreationNovaRecipeSequence(): Sequence<ResultRecipe> {
        return getAllNovaRecipes().filterIsInstance<ResultRecipe>()
    }
    
    private fun getUsageNovaRecipeSequence(): Sequence<InputChoiceRecipe> {
        return getAllNovaRecipes().filterIsInstance<InputChoiceRecipe>()
    }
    
}

