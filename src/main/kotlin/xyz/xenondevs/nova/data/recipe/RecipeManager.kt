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
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.impl.processing.PressType
import xyz.xenondevs.nova.util.customModelData
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.util.removeFirstWhere
import kotlin.experimental.and

class NovaRecipeChoice(private val material: NovaMaterial) : ExactChoice(material.createItemStack()) {
    
    private val requiredType: Material
    private val requiredModelData: Int
    
    init {
        val itemStack = material.createItemStack()
        requiredType = itemStack.type
        requiredModelData = itemStack.customModelData
    }
    
    override fun test(item: ItemStack): Boolean {
        val customModelData = item.customModelData
        return item.type == requiredType && (customModelData == requiredModelData || material.legacyItemIds?.contains(customModelData) == true)
    }
    
}

object RecipeManager : Listener {
    
    val shapedRecipes = ArrayList<OptimizedShapedRecipe>()
    val shapelessRecipes = ArrayList<ShapelessRecipe>()
    
    val vanillaRegisteredRecipeKeys = ArrayList<NamespacedKey>()
    val pulverizerRecipes = HashMap<NamespacedKey, PulverizerNovaRecipe>()
    val platePressRecipes = HashMap<NamespacedKey, PlatePressNovaRecipe>()
    val gearPressRecipes = HashMap<NamespacedKey, GearPressNovaRecipe>()
    
    private val craftingCache: Cache<CraftingMatrix, ItemStack> =
        CacheBuilder.newBuilder().concurrencyLevel(1).maximumSize(DEFAULT_CONFIG.getLong("crafting_cache.size")!!).build()
    
    fun registerRecipes() {
        LOGGER.info("Loading recipes")
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        RecipesLoader.loadRecipes().forEach(NovaRecipe::register)
    }
    
    fun getPulverizerRecipeFor(itemStack: ItemStack): PulverizerNovaRecipe? =
        pulverizerRecipes.values.firstOrNull { recipe -> recipe.inputStacks.any { it.isSimilar(itemStack) } }
    
    fun getPressRecipeFor(itemStack: ItemStack, type: PressType): CustomNovaRecipe? {
        return if (type == PressType.PLATE) platePressRecipes.values.firstOrNull { recipe -> recipe.inputStacks.any { it.isSimilar(itemStack) } }
        else gearPressRecipes.values.firstOrNull { recipe -> recipe.inputStacks.any { it.isSimilar(itemStack) } }
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
                    return@get event.inventory.result
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
