package xyz.xenondevs.nova.recipe

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice.ExactChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.impl.PressType
import xyz.xenondevs.nova.util.novaMaterial

private val Recipe.key: NamespacedKey
    get() = when (this) {
        is ShapedRecipe -> key
        is ShapelessRecipe -> key
        else -> throw UnsupportedOperationException("Unsupported Recipe Type")
    }

class NovaRecipeChoice(material: NovaMaterial) : ExactChoice(material.createItemStack())

object RecipeManager : Listener {
    
    internal val recipes = ArrayList<NamespacedKey>()
    internal val pulverizerRecipes = ArrayList<PulverizerNovaRecipe>()
    internal val platePressRecipes = ArrayList<PlatePressNovaRecipe>()
    internal val gearPressRecipes = ArrayList<GearPressNovaRecipe>()
    
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
        if (event.recipe != null
            && event.inventory.contents.any { it.novaMaterial != null }
            && !recipes.contains(event.recipe!!.key)) {
            
            event.inventory.result = ItemStack(Material.AIR)
        }
    }
    
}
