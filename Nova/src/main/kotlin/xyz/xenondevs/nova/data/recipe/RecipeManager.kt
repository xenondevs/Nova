package xyz.xenondevs.nova.data.recipe

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
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.util.customModelData
import xyz.xenondevs.nova.util.data.key
import xyz.xenondevs.nova.util.minecraftServer
import xyz.xenondevs.nova.util.namelessCopyOrSelf
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry

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

private val ALLOW_RESULT_OVERWRITE = DEFAULT_CONFIG.getBoolean("crafting.allow_result_overwrite")

object RecipeManager : Initializable(), Listener {
    
    private val vanillaRegisteredRecipeKeys = ArrayList<NamespacedKey>()
    val novaRecipes = HashMap<RecipeType<*>, HashMap<NamespacedKey, NovaRecipe>>()
    
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
                        is ShapedRecipe -> {
                            val nmsRecipe = NovaShapedRecipe(OptimizedShapedRecipe(recipe))
                            minecraftServer.recipeManager.addRecipe(nmsRecipe)
                        }
                        
                        is ShapelessRecipe -> {
                            val nmsRecipe = NovaShapelessRecipe(recipe)
                            minecraftServer.recipeManager.addRecipe(nmsRecipe)
                        }
                        
                        else -> Bukkit.addRecipe(recipe)
                    }
                }
                
                is NovaRecipe -> novaRecipes.getOrPut(recipe.type) { HashMap() }[recipe.key] = recipe
                
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
    
    @EventHandler(priority = EventPriority.LOWEST)
    fun handlePrepareItemCraft(event: PrepareItemCraftEvent) {
        val recipe = event.recipe ?: return
        val namespace = recipe.key.namespace
        
        if (namespace == "nova") {
            // If this is a Nova recipe result, replace it with a NovaCraftingInventory
            ReflectionRegistry.PREPARE_ITEM_CRAFT_EVENT_MATRIX_FIELD.set(event, NovaCraftingInventory(recipe, event.inventory))
        } else if (event.inventory.contents.any { it.novaMaterial != null }) {
            // prevent non-Nova recipes from using Nova items
            event.inventory.result = ItemStack(Material.AIR)
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
