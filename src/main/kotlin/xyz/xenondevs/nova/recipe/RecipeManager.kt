@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.recipe

import org.bukkit.Bukkit
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
import xyz.xenondevs.nova.util.novaMaterial

private fun NovaMaterial.makeRecipe(amount: Int = 1): ShapedRecipe {
    val key = NamespacedKey(NOVA, name)
    RecipeManager.recipes += key
    return ShapedRecipe(NamespacedKey(NOVA, name), createItemBuilder().setAmount(amount).build())
}

private val Recipe.key: NamespacedKey
    get() = when (this) {
        is ShapedRecipe -> key
        is ShapelessRecipe -> key
        else -> throw UnsupportedOperationException("Unsupported Recipe Type")
    }

class NovaRecipe(val result: NovaMaterial, val shape: List<String>, val ingredientMap: Map<Char, RecipeChoice>, val amount: Int) {
    fun toShapedRecipe(): ShapedRecipe {
        val recipe = result.makeRecipe(amount)
        recipe.shape(*shape.toTypedArray())
        ingredientMap.forEach { (key, material) -> recipe.setIngredient(key, material) }
        
        return recipe
    }
}

class NovaRecipeChoice(material: NovaMaterial) : ExactChoice(material.createItemStack())

object RecipeManager : Listener {
    
    internal val recipes = ArrayList<NamespacedKey>()
    
    fun registerRecipes() {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        
        val server = Bukkit.getServer()
        NovaConfig.loadRecipes().map(NovaRecipe::toShapedRecipe).forEach(server::addRecipe)
        
        FurnaceRecipes.registerRecipes()
        PressRecipe.registerRecipes()
        PulverizerRecipe.registerRecipes()
    }
    
    private fun addRecipes(recipes: List<Recipe>) {
        val server = Bukkit.getServer()
        recipes.forEach(server::addRecipe)
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
