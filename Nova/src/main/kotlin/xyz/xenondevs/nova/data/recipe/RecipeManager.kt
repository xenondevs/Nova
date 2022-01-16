package xyz.xenondevs.nova.data.recipe

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
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
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.network.event.impl.ServerboundPlaceRecipePacketEvent
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.data.key
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import java.util.*
import kotlin.experimental.and

interface ItemTest {
    
    val example: ItemStack
    
    fun test(item: ItemStack): Boolean
    
}

class ModelDataTest(private val type: Material, private val data: IntArray, override val example: ItemStack) : ItemTest {
    
    override fun test(item: ItemStack): Boolean {
        return item.type == type && item.customModelData in data
    }
    
}

class ComplexTest(override val example: ItemStack) : ItemTest {
    
    override fun test(item: ItemStack): Boolean {
        val testStack = item.namelessCopyOrSelf
        return example.isSimilar(testStack)
    }
    
}

class CustomRecipeChoice(private val tests: List<ItemTest>) : ExactChoice(tests.map(ItemTest::example)) {
    
    override fun test(item: ItemStack): Boolean {
        return tests.any { it.test(item) }
    }
    
}

private val CRAFTING_CACHE_SIZE = DEFAULT_CONFIG.getLong("crafting.cache_size")!!
private val ALLOW_RESULT_OVERWRITE = DEFAULT_CONFIG.getBoolean("crafting.allow_result_overwrite")

object RecipeManager : Initializable(), Listener {
    
    private val shapedRecipes = HashMap<NamespacedKey, OptimizedShapedRecipe>()
    private val shapelessRecipes = HashMap<NamespacedKey, ShapelessRecipe>()
    private val vanillaRegisteredRecipeKeys = ArrayList<NamespacedKey>()
    val novaRecipes = HashMap<RecipeType<*>, HashMap<NamespacedKey, NovaRecipe>>()
    
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
                    val key = (recipe as Keyed).key
                    
                    when (recipe) {
                        is ShapedRecipe -> shapedRecipes[key] = OptimizedShapedRecipe(recipe)
                        is ShapelessRecipe -> shapelessRecipes[key] = recipe
                    }
                    
                    Bukkit.addRecipe(recipe)
                    vanillaRegisteredRecipeKeys += key
                }
                
                is NovaRecipe -> {
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
    fun <T : NovaRecipe> getRecipe(type: RecipeType<T>, key: NamespacedKey): T? {
        return novaRecipes[type]?.get(key) as T?
    }
    
    @EventHandler
    fun handleJoin(event: PlayerJoinEvent) {
        vanillaRegisteredRecipeKeys.forEach(event.player::discoverRecipe)
    }
    
    @EventHandler
    fun handleRecipePlace(event: ServerboundPlaceRecipePacketEvent) {
        val key = NamespacedKey.fromString(event.packet.recipe.toString())
        if (key in shapedRecipes) {
            runTask { fillCraftingInventory(event.player, shapedRecipes[key]!!) }
            event.isCancelled = true
        } else if (key in shapelessRecipes) {
            runTask { fillCraftingInventory(event.player, shapelessRecipes[key]!!) }
            event.isCancelled = true
        }
    }
    
    private fun fillCraftingInventory(player: Player, recipe: OptimizedShapedRecipe) {
        val craftingInventory = player.openInventory.topInventory as CraftingInventory
        
        // clear previous items
        player.addToInventoryOrDrop(craftingInventory.matrix.filterNotNull())
        craftingInventory.matrix = arrayOfNulls(9)
        
        // check if the player has the required ingredients
        val inventory = player.inventory
        if (inventory.containsAll(recipe.requiredChoices)) {
            // fill inventory
            for (slot in 0 until 9) {
                val choice = recipe.choiceMatrix[slot] ?: continue
                
                val item = inventory.takeFirstOccurrence(choice)
                if (item != null) {
                    // Crafting inventory starts at index 1
                    craftingInventory.setItem(slot + 1, item)
                }
            }
            
        } else {
            // send ghost recipe
            val packet = ClientboundPlaceGhostRecipePacket(player.serverPlayer.containerMenu.containerId, recipe.key)
            player.send(packet)
        }
    }
    
    private fun fillCraftingInventory(player: Player, recipe: ShapelessRecipe) {
        val craftingInventory = player.openInventory.topInventory as CraftingInventory
        
        // clear previous items
        player.addToInventoryOrDrop(craftingInventory.matrix.filterNotNull())
        craftingInventory.matrix = arrayOfNulls(9)
        
        // check if the player has the required ingredients
        val inventory = player.inventory
        if (inventory.containsAll(recipe.choiceList)) {
            // fill inventory
            for ((i, choice) in recipe.choiceList.withIndex()) {
                val item = inventory.takeFirstOccurrence(choice)
                if (item != null) {
                    // Crafting inventory starts at index 1
                    craftingInventory.setItem(i + 1, item)
                }
            }
            
        } else {
            // send ghost recipe
            val packet = ClientboundPlaceGhostRecipePacket(player.serverPlayer.containerMenu.containerId, recipe.key.toString())
            player.send(packet)
        }
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
        return shapedRecipes.values.firstOrNull { recipe ->
            // loop over all items in the crafting grid
            matrix.withIndex().all { (index, matrixStack) ->
                // check if the item stack matches with the given recipe choice
                val choice = recipe.choiceMatrix[index] ?: return@all matrixStack == null
                return@all matrixStack != null && choice.test(matrixStack)
            }
        }?.recipe
    }
    
    private fun findMatchingShapelessRecipe(matrix: Array<ItemStack?>): Recipe? {
        // loop over all shapeless recipes from nova
        return shapelessRecipes.values.firstOrNull { recipe ->
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
    
    val requiredChoices: List<RecipeChoice>
    val choiceMatrix: Array<RecipeChoice?>
    val key: String
    
    init {
        val flatShape = recipe.shape.joinToString("")
        choiceMatrix = Array(9) { recipe.choiceMap[flatShape[it]] }
        requiredChoices = flatShape.mapNotNull { recipe.choiceMap[it] }
        key = (recipe as Keyed).key.toString()
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
