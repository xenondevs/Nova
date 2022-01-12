package xyz.xenondevs.nova.data.recipe

import com.google.gson.JsonParser
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.inventory.*
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.command.CommandManager
import xyz.xenondevs.nova.command.impl.NovaRecipeCommand
import xyz.xenondevs.nova.command.impl.NovaUsageCommand
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.ui.menu.item.recipes.group.RecipeGroup
import xyz.xenondevs.nova.ui.menu.item.recipes.group.hardcoded.HardcodedRecipes
import xyz.xenondevs.nova.util.ItemUtils.getId
import xyz.xenondevs.nova.util.data.getInputStacks
import xyz.xenondevs.nova.util.data.getResourceAsStream
import xyz.xenondevs.nova.util.runTask

object RecipeRegistry : Initializable() {
    
    private val ITEM_INFO = JsonParser.parseReader(getResourceAsStream("item_info.json")!!.reader()).asJsonObject
    val CREATION_INFO = ITEM_INFO.get("creation").asJsonObject.entrySet().associate { it.key to it.value.asString }
    val USAGE_INFO = ITEM_INFO.get("usage").asJsonObject.entrySet().associate { it.key to it.value.asString }
    
    private var BUKKIT_RECIPES: List<Recipe> = ArrayList()
    
    var CREATION_RECIPES: Map<String, Map<RecipeGroup, List<RecipeContainer>>> = HashMap()
        private set
    var USAGE_RECIPES: Map<String, Map<RecipeGroup, Set<RecipeContainer>>> = HashMap()
        private set
    var RECIPES_BY_TYPE: Map<RecipeGroup, List<RecipeContainer>> = HashMap()
    
    override val inMainThread = false
    override val dependsOn = RecipeManager
    
    override fun init() {
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
    
    private fun loadBukkitRecipes(): List<Recipe> {
        val list = ArrayList<Recipe>()
        Bukkit.recipeIterator().forEachRemaining(list::add)
        return list
    }
    
    private fun loadCreationRecipes(): Map<String, Map<RecipeGroup, List<RecipeContainer>>> {
        val map = HashMap<String, HashMap<RecipeGroup, MutableList<RecipeContainer>>>()
        
        // add all with bukkit registered recipes
        getBukkitRecipeSequence().forEach {
            val itemKey = getId(it.result)
            map.getOrPut(itemKey) { hashMapOf() }
                .getOrPut(RecipeType.of(it).group) { mutableListOf() }
                .add(RecipeContainer(it))
        }
        
        // add all nova machine recipes
        getCreationNovaRecipeSequence().forEach {
            val itemKey = getId(it.result)
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
                val itemKey = getId(inputStack)
                map.getOrPut(itemKey) { hashMapOf() }
                    .getOrPut(group) { LinkedHashSet() }
                    .add(RecipeContainer(recipe))
            }
        }
        
        // add all nova machine recipes
        getUsageNovaRecipeSequence().forEach { recipe ->
            val group = RecipeType.of(recipe).group
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
            map.getOrPut(RecipeType.of(it).group) { ArrayList() } += RecipeContainer(it)
        }
        return map
    }
    
    private fun getBukkitRecipeSequence(): Sequence<Recipe> {
        return BUKKIT_RECIPES.asSequence()
            .filter {
                val key = (it as Keyed).key
                val namespace = key.namespace
                
                (namespace == "minecraft" || namespace == "nova" || CustomItemServiceManager.hasRecipe(key)) // do not allow recipes from unsupported plugins to show up
                    && (it is ShapedRecipe || it is ShapelessRecipe || it is FurnaceRecipe || it is StonecuttingRecipe || it is SmithingRecipe)
            }
    }
    
    private fun getAllNovaRecipes(): Sequence<NovaRecipe> {
        return RecipeManager.novaRecipes.values.asSequence().flatMap { it.values } + HardcodedRecipes.recipes.asSequence()
    }
    
    private fun getCreationNovaRecipeSequence(): Sequence<ResultingRecipe> {
        return getAllNovaRecipes().filterIsInstance<ResultingRecipe>()
    }
    
    private fun getUsageNovaRecipeSequence(): Sequence<InputChoiceRecipe> {
        return getAllNovaRecipes().filterIsInstance<InputChoiceRecipe>()
    }
    
}

