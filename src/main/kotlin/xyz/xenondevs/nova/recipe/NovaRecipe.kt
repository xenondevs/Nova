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
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.util.novaMaterial

private fun ShapedRecipe.setIngredient(key: Char, material: NovaMaterial) =
    setIngredient(key, NovaRecipeChoice(material))

val Recipe.key: NamespacedKey
    get() = when (this) {
        is ShapedRecipe -> key
        is ShapelessRecipe -> key
        else -> throw UnsupportedOperationException("Unsupported Recipe Type")
    }

@Suppress("DEPRECATION")
class NovaRecipeChoice(material: NovaMaterial) : ExactChoice(material.createItemStack())

object NovaRecipes : Listener {
    
    private val recipes = ArrayList<NamespacedKey>()
    
    fun registerRecipes() {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        
        addRecipes(
            NovaMaterial.FURNACE_GENERATOR.makeRecipe()
                .shape("iii", "ifi", "iri")
                .setIngredient('i', Material.IRON_INGOT)
                .setIngredient('f', Material.FURNACE)
                .setIngredient('r', Material.REDSTONE),
            
            NovaMaterial.BASIC_POWER_CELL.makeRecipe()
                .shape("ici", "crc", "ici")
                .setIngredient('i', Material.IRON_INGOT)
                .setIngredient('c', NovaMaterial.BASIC_CABLE)
                .setIngredient('r', Material.REDSTONE_BLOCK),
            
            NovaMaterial.PULVERIZER.makeRecipe()
                .shape("iii", "igi", "ipi")
                .setIngredient('i', Material.IRON_INGOT)
                .setIngredient('g', NovaMaterial.IRON_GEAR)
                .setIngredient('p', NovaMaterial.BASIC_POWER_CELL),
            
            NovaMaterial.BASIC_CABLE.makeRecipe(8)
                .shape("iii", "rrr", "iii")
                .setIngredient('i', Material.IRON_INGOT)
                .setIngredient('r', Material.REDSTONE)
        )
        
        FurnaceRecipes.registerRecipes()
        PressRecipe.registerRecipes()
        PulverizerRecipe.registerRecipes()
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
    
    @EventHandler
    fun handlePrepareItemCraft(event: PrepareItemCraftEvent) {
        if (event.recipe != null
            && event.inventory.contents.any { it.novaMaterial != null }
            && !recipes.contains(event.recipe!!.key)) {
            
            event.inventory.result = ItemStack(Material.AIR)
        }
    }
    
}
