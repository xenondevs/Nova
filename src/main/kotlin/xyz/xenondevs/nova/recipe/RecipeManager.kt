package xyz.xenondevs.nova.recipe

import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.*
import org.bukkit.inventory.RecipeChoice.ExactChoice
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.impl.processing.PressType
import xyz.xenondevs.nova.util.customModelData
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.util.removeFirstWhere

class NovaRecipeChoice(private val material: NovaMaterial) : ExactChoice(material.createItemStack()) {
    
    override fun test(item: ItemStack): Boolean {
        val customModelData = item.customModelData
        return choices.any {
            it.type == item.type && (it.customModelData == customModelData || material.legacyItemIds?.contains(customModelData) == true)
        }
    }
    
}

object RecipeManager : Listener {
    
    internal val shapedRecipes = ArrayList<OptimizedShapedRecipe>()
    internal val shapelessRecipes = ArrayList<ShapelessRecipe>()
    
    internal val recipes = ArrayList<NamespacedKey>()
    val pulverizerRecipes = ArrayList<PulverizerNovaRecipe>()
    val platePressRecipes = ArrayList<PlatePressNovaRecipe>()
    val gearPressRecipes = ArrayList<GearPressNovaRecipe>()
    
    fun registerRecipes() {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        NovaConfig.loadRecipes().forEach(NovaRecipe::register)
    }
    
    fun getPulverizerOutputFor(itemStack: ItemStack): ItemStack? =
        pulverizerRecipes.firstOrNull { recipe -> recipe.inputStacks.any { it.isSimilar(itemStack) } }?.resultStack
    
    fun getPressOutputFor(itemStack: ItemStack, type: PressType): ItemStack? {
        return if (type == PressType.PLATE) platePressRecipes.firstOrNull { recipe -> recipe.inputStacks.any { it.isSimilar(itemStack) } }?.resultStack
        else gearPressRecipes.firstOrNull { recipe -> recipe.inputStacks.any { it.isSimilar(itemStack) } }?.resultStack
    }
    
    @EventHandler
    fun handleJoin(event: PlayerJoinEvent) {
        recipes.forEach(event.player::discoverRecipe)
    }
    
    @EventHandler
    fun handlePrepareItemCraft(event: PrepareItemCraftEvent) {
        val predictedRecipe = event.recipe
        if (predictedRecipe != null && (predictedRecipe as Keyed).key.namespace != "nova") {
            // prevent non-nova recipes from using nova items
            if (event.inventory.contents.any { it.novaMaterial != null })
                event.inventory.result = ItemStack(Material.AIR)
        } else {
            // if the recipe is null or it bukkit thinks it found a nova recipe, we do our own calculations
            // this does two things:
            // 1. calls the custom test method of NovaRecipeChoice (-> ignores irrelevant nbt data)
            // 2. allows for the usage of NovaRecipeChoice / ExactChoice in shapeless crafting recipes
            
            val matrix = event.inventory.matrix
            val recipe = findMatchingShapedRecipe(matrix) ?: findMatchingShapelessRecipe(matrix)
            event.inventory.result = recipe?.result ?: ItemStack(Material.AIR)
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
