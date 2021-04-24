package xyz.xenondevs.nova.recipe

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.NovaMaterial

object PulverizerRecipe {
    
    private val recipes = HashMap<Material, ItemStack>()
    
    fun registerRecipes() {
        recipes[Material.IRON_ORE] = NovaMaterial.IRON_DUST.createItemStack(2)
        recipes[Material.IRON_INGOT] = NovaMaterial.IRON_DUST.createItemStack()
        recipes[Material.GOLD_ORE] = NovaMaterial.GOLD_DUST.createItemStack(2)
        recipes[Material.GOLD_INGOT] = NovaMaterial.GOLD_DUST.createItemStack()
        recipes[Material.DIAMOND_ORE] = NovaMaterial.DIAMOND_DUST.createItemStack(2)
        recipes[Material.DIAMOND] = NovaMaterial.DIAMOND_DUST.createItemStack()
        recipes[Material.ANCIENT_DEBRIS] = NovaMaterial.NETHERITE_DUST.createItemStack(2)
        recipes[Material.NETHERITE_INGOT] = NovaMaterial.NETHERITE_DUST.createItemStack()
        recipes[Material.EMERALD_ORE] = NovaMaterial.EMERALD_DUST.createItemStack(2)
        recipes[Material.EMERALD] = NovaMaterial.EMERALD_DUST.createItemStack()
        recipes[Material.REDSTONE_ORE] = ItemStack(Material.REDSTONE, 10)
        recipes[Material.LAPIS_ORE] = NovaMaterial.LAPIS_DUST.createItemStack(2)
        recipes[Material.LAPIS_LAZULI] = NovaMaterial.LAPIS_DUST.createItemStack()
        // recipes[Material.COAL_ORE] = NovaMaterial.COAL_DUST.createItemStack(2)
        // recipes[Material.COAL] = NovaMaterial.COAL_DUST.createItemStack()
    }
    
    fun isPulverizable(material: Material) = recipes.containsKey(material)
    
    fun getOutputFor(material: Material) = recipes[material]!!
}