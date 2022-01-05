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
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.util.customModelData
import xyz.xenondevs.nova.util.data.key
import xyz.xenondevs.nova.util.namelessCopyOrSelf
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.removeFirstWhere
import java.util.*
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

class ComplexChoice(choices: List<ItemStack>) : ExactChoice(choices) {
    
    override fun test(item: ItemStack): Boolean {
        val testStack = item.namelessCopyOrSelf
        return choices.any { it.isSimilar(testStack) }
    }
    
}

private val CRAFTING_CACHE_SIZE = DEFAULT_CONFIG.getLong("crafting.cache_size")!!
private val ALLOW_RESULT_OVERWRITE = DEFAULT_CONFIG.getBoolean("crafting.allow_result_overwrite")

object RecipeManager : Initializable(), Listener {
    
    private val shapedRecipes = ArrayList<OptimizedShapedRecipe>()
    private val shapelessRecipes = ArrayList<ShapelessRecipe>()
    private val vanillaRegisteredRecipeKeys = ArrayList<NamespacedKey>()
    val novaRecipes = HashMap<RecipeType<*>, HashMap<NamespacedKey, SerializableNovaRecipe>>()
    
    private val craftingCache: Cache<CraftingMatrix, Optional<Recipe>> =
        CacheBuilder.newBuilder().concurrencyLevel(1).maximumSize(CRAFTING_CACHE_SIZE).build()
    
    override val inMainThread = true
    override val dependsOn = CustomItemServiceManager
    
    override fun init() {
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
                    novaRecipes.getOrPut(recipe.type) { HashMap() }[recipe.key] = recipe
                }
                
                else -> throw UnsupportedOperationException("Unsupported Recipe Type: ${recipe::class.java}")
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : ConversionNovaRecipe> getConversionRecipeFor(type: RecipeType<T>, input: ItemStack): T? {
        return novaRecipes[type]?.values?.firstOrNull { (it as ConversionNovaRecipe).input.test(input) } as T?
    }
    
    fun getFluidInfuserInsertRecipeFor(fluidType: FluidType, input: ItemStack): FluidInfuserRecipe? {
        return novaRecipes[RecipeType.FLUID_INFUSER]?.values?.asSequence()
            ?.map { it as FluidInfuserRecipe }
            ?.firstOrNull { recipe ->
                recipe.mode == FluidInfuserRecipe.InfuserMode.INSERT
                    && recipe.fluidType == fluidType
                    && recipe.input.test(input)
            }
    }
    
    fun getFluidInfuserExtractRecipeFor(input: ItemStack): FluidInfuserRecipe? {
        return novaRecipes[RecipeType.FLUID_INFUSER]?.values?.asSequence()
            ?.map { it as FluidInfuserRecipe }
            ?.firstOrNull { recipe ->
                recipe.mode == FluidInfuserRecipe.InfuserMode.EXTRACT
                    && recipe.input.test(input)
            }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : SerializableNovaRecipe> getRecipe(type: RecipeType<T>, key: NamespacedKey): T? {
        return novaRecipes[type]?.get(key) as T?
    }
    
    @EventHandler
    fun handleJoin(event: PlayerJoinEvent) {
        vanillaRegisteredRecipeKeys.forEach(event.player::discoverRecipe)
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    fun handlePrepareItemCraft(event: PrepareItemCraftEvent) {
        val predictedRecipe = event.recipe
        val recipe = craftingCache.get(CraftingMatrix(event.inventory.matrix)) {
            if (predictedRecipe != null && (predictedRecipe as Keyed).key.namespace != "nova") {
                // prevent non-nova recipes from using nova items
                if (event.inventory.contents.any { it.novaMaterial != null }) {
                    return@get Optional.empty()
                } else {
                    // Bukkit's calculated result is correct
                    return@get Optional.of(predictedRecipe)
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
                
                return@get Optional.ofNullable(recipe)
            }
            
        }.orElse(null)
        
        // Set the resulting item stack
        event.inventory.result = recipe?.result ?: ItemStack(Material.AIR)
        
        // If this is a Nova-calculated result, replace it with a NovaCraftingInventory
        if (recipe?.key?.namespace == "nova" || predictedRecipe?.key?.namespace == "nova") {
            ReflectionRegistry.PREPARE_ITEM_CRAFT_EVENT_MATRIX_FIELD.set(event, NovaCraftingInventory(recipe, event.inventory))
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

/**
 * A crafting inventory that is set to display the new recipe and prevent subsequent
 * changes to the resulting item.
 */
class NovaCraftingInventory(
    val result: Recipe?,
    val inventory: CraftingInventory
) : CraftingInventory by inventory {
    
    override fun getRecipe(): Recipe? {
        return result
    }
    
    override fun setResult(newResult: ItemStack?) {
        if (ALLOW_RESULT_OVERWRITE)
            inventory.result = newResult
    }
    
}
