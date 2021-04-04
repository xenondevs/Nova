package xyz.xenondevs.nova.recipe

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice.ExactChoice
import org.bukkit.inventory.ShapedRecipe
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

private fun ShapedRecipe.setIngredient(key: Char, material: NovaMaterial) =
    setIngredient(key, NovaRecipeChoice(material))

@Suppress("DEPRECATION")
class NovaRecipeChoice(material: NovaMaterial) : ExactChoice(material.createItemStack())

object NovaRecipes : Listener {
    
    private val recipes = ArrayList<NamespacedKey>()
    
    init {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        
        addRecipes(
            NovaMaterial.FURNACE_GENERATOR.makeRecipe()
                .shape("iii", "ifi", "iri")
                .setIngredient('i', Material.IRON_INGOT)
                .setIngredient('f', Material.FURNACE)
                .setIngredient('r', Material.REDSTONE),
            
            NovaMaterial.POWER_CELL.makeRecipe()
                .shape("ici", "crc", "ici")
                .setIngredient('i', Material.IRON_INGOT)
                .setIngredient('c', NovaMaterial.CABLE)
                .setIngredient('r', Material.REDSTONE_BLOCK),
            
            NovaMaterial.CABLE.makeRecipe(8)
                .shape("iii", "rrr", "iii")
                .setIngredient('i', Material.IRON_INGOT)
                .setIngredient('r', Material.REDSTONE)
        )
    }
    
    private fun addRecipes(vararg recipes: Recipe) {
        val server = Bukkit.getServer()
        recipes.forEach(server::addRecipe)
    }
    
    private fun NovaMaterial.makeRecipe(amount: Int = 1): ShapedRecipe {
        val key = NamespacedKey(NOVA, name)
        recipes += key
        return ShapedRecipe(NamespacedKey(NOVA, name), createItemBuilder().setAmount(amount).build())
    }
    
    @EventHandler
    fun handleJoin(event: PlayerJoinEvent) {
        recipes.forEach(event.player::discoverRecipe)
    }
    
}
