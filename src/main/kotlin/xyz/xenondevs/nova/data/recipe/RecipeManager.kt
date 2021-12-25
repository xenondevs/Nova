package xyz.xenondevs.nova.data.recipe

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.*
import org.bukkit.inventory.RecipeChoice.ExactChoice
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.util.customModelData
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.util.removeFirstWhere
import kotlin.experimental.and

class CustomRecipeChoice(private val customChoices: List<Pair<Material, IntArray>>, examples: List<ItemStack>) : ExactChoice(examples) {
    
    override fun test(item: ItemStack): Boolean {
        return customChoices.any { (material, requiredModelDataArray) ->
            item.type == material && requiredModelDataArray.contains(item.customModelData)
        }
    }
    
}

class SingleCustomRecipeChoice(private val material: Material, private val customModelData: Int, example: ItemStack) : ExactChoice(example) {
    
    override fun test(item: ItemStack): Boolean {
        return item.type == material && item.customModelData == customModelData
    }
    
}

object RecipeManager : Listener {
    
    private val shapedRecipes = ArrayList<OptimizedShapedRecipe>()
    private val shapelessRecipes = ArrayList<ShapelessRecipe>()
    private val vanillaRegisteredRecipeKeys = ArrayList<NamespacedKey>()
    val conversionNovaRecipes = HashMap<RecipeType<*>, HashMap<NamespacedKey, ConversionNovaRecipe>>()
    
    private val craftingCache: Cache<CraftingMatrix, ItemStack> =
        CacheBuilder.newBuilder().concurrencyLevel(1).maximumSize(DEFAULT_CONFIG.getLong("crafting_cache.size")!!).build()
    
    fun registerRecipes() {
        LOGGER.info("Loading recipes")
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        
        RecipesLoader.loadRecipes().forEach { recipe ->
            when (recipe) {
                is Recipe -> {
                    vanillaRegisteredRecipeKeys += (recipe as Keyed).key
                    when (recipe) {
                        is ShapedRecipe -> shapedRecipes += OptimizedShapedRecipe(recipe)
                        is ShapelessRecipe -> shapelessRecipes += recipe
                    }
                    Bukkit.addRecipe(recipe)
                }
                
                is ConversionNovaRecipe -> {
                    conversionNovaRecipes.getOrPut(recipe.type) { HashMap() }[recipe.key] = recipe
                }
                
                else -> throw UnsupportedOperationException("Unsupported Recipe Type: ${recipe::class.java}")
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : ConversionNovaRecipe> getRecipeFor(type: RecipeType<T>, input: ItemStack): T? {
        return conversionNovaRecipes[type]?.values?.firstOrNull { it.input.test(input) } as T?
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : ConversionNovaRecipe> getRecipe(type: RecipeType<T>, key: NamespacedKey): T? {
        return conversionNovaRecipes[type]?.get(key) as T?
    }
    
    @EventHandler
    fun handleJoin(event: PlayerJoinEvent) {
        vanillaRegisteredRecipeKeys.forEach(event.player::discoverRecipe)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun handlePrepareItemCraft(event: PrepareItemCraftEvent) {
        event.inventory.result = craftingCache.get(CraftingMatrix(event.inventory.matrix)) {
            
            val predictedRecipe = event.recipe
            if (predictedRecipe != null && (predictedRecipe as Keyed).key.namespace != "nova") {
                // prevent non-nova recipes from using nova items
                if (event.inventory.contents.any { it.novaMaterial != null }) {
                    return@get ItemStack(Material.AIR)
                } else {
                    // Bukkit's calculated result is correct
                    return@get event.inventory.result ?: ItemStack(Material.AIR)
                }
            } else {
                // if the recipe is null or it bukkit thinks it found a nova recipe, we do our own calculations
                // this does two things:
                // 1. calls the custom test method of NovaRecipeChoice (-> ignores irrelevant nbt data)
                // 2. allows for the usage of NovaRecipeChoice / ExactChoice in shapeless crafting recipes
                
                val matrix = event.inventory.matrix
                val recipe = if (matrix.size == 9) {
                    findMatchingShapedRecipe(matrix) ?: findMatchingShapelessRecipe(matrix)
                } else findMatchingShapelessRecipe(matrix)
                
                return@get recipe?.result ?: ItemStack(Material.AIR)
            }
            
        }
    }
    
    private fun findMatchingShapedRecipe(matrix: Array<ItemStack?>): Recipe? {
        // loop over all shaped recipes from nova
        return shapedRecipes.firstOrNull { recipe ->
            // loop over all items in the crafting grid
            matrix.withIndex().all { (index, matrixStack) ->
                // check if the item stack matches with the given recipe choice
                val choice = recipe.choices[index] ?: return@all matrixStack == null
                return@all matrixStack != null && choice.test(matrixStack)
            }
        }?.recipe
    }
    
    private fun findMatchingShapelessRecipe(matrix: Array<ItemStack?>): Recipe? {
        // loop over all shapeless recipes from nova
        return shapelessRecipes.firstOrNull { recipe ->
            val choiceList = recipe.choiceList
            
            // loop over all items in the inventory and remove matching choices from the choice list
            // if there is an item stack that does not have a matching choice or the choice list is not empty
            // at the end of the loop, the recipe doesn't match
            return@firstOrNull matrix.filterNotNull().all { matrixStack ->
                choiceList.removeFirstWhere { it.test(matrixStack) }
            } && choiceList.isEmpty()
        }
    }
    
}

/**
 * Optimizes the recipe matching algorithm by already saving an array of recipe choices in the
 * layout of a crafting inventory.
 */
class OptimizedShapedRecipe(val recipe: ShapedRecipe) {
    
    val choices: Array<RecipeChoice?>
    
    init {
        val flatShape = recipe.shape.joinToString("")
        choices = Array(9) { recipe.choiceMap[flatShape[it]] }
    }
    
}

class CraftingMatrix(matrix: Array<ItemStack?>) {
    
    private val matrix = matrix.map { it?.clone() }
    private val hashCode: Int
    
    init {
        var hash = 1
        for (item in matrix)
            hash = 31 * hash + (item?.hashCodeIgnoredAmount() ?: 0)
        this.hashCode = hash
    }
    
    override fun hashCode(): Int =
        hashCode
    
    override fun equals(other: Any?): Boolean {
        if (other !is CraftingMatrix || other.matrix.size != matrix.size) return false
        
        for (i in matrix.indices) {
            val item = matrix[i]
            val otherItem = other.matrix[i]
            
            if (!(item?.isSimilar(otherItem) ?: (otherItem == null))) return false
        }
        
        return true
    }
    
    @Suppress("DEPRECATION")
    private fun ItemStack.hashCodeIgnoredAmount(): Int {
        var hash = 1
        hash = hash * 31 + type.hashCode()
        hash = hash * 31 + (durability and 0xffff.toShort())
        hash = hash * 31 + if (hasItemMeta()) itemMeta.hashCode() else 0
        return hash
    }
    
}
