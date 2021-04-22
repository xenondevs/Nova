package xyz.xenondevs.nova.recipe

import org.bukkit.Material
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.recipe.PressType.GEAR
import xyz.xenondevs.nova.recipe.PressType.PLATE

object PressRecipe {
    
    private val recipes = HashMap<Pair<Material, PressType>, NovaMaterial>()
    
    fun registerRecipes() {
        recipes[Material.IRON_INGOT to PLATE] = NovaMaterial.IRON_PLATE
        recipes[Material.GOLD_INGOT to PLATE] = NovaMaterial.GOLD_PLATE
        recipes[Material.NETHERITE_INGOT to PLATE] = NovaMaterial.NETHERITE_PLATE
        recipes[Material.DIAMOND to PLATE] = NovaMaterial.DIAMOND_PLATE
        recipes[Material.EMERALD to PLATE] = NovaMaterial.EMERALD_PLATE
        recipes[Material.REDSTONE to PLATE] = NovaMaterial.REDSTONE_PLATE
        recipes[Material.LAPIS_LAZULI to PLATE] = NovaMaterial.LAPIS_PLATE
        recipes[Material.IRON_INGOT to GEAR] = NovaMaterial.IRON_GEAR
        recipes[Material.GOLD_INGOT to GEAR] = NovaMaterial.GOLD_GEAR
        recipes[Material.NETHERITE_INGOT to GEAR] = NovaMaterial.NETHERITE_GEAR
        recipes[Material.DIAMOND to GEAR] = NovaMaterial.DIAMOND_GEAR
        recipes[Material.EMERALD to GEAR] = NovaMaterial.EMERALD_GEAR
        recipes[Material.REDSTONE to GEAR] = NovaMaterial.REDSTONE_GEAR
        recipes[Material.LAPIS_LAZULI to GEAR] = NovaMaterial.LAPIS_GEAR
    }
    
    fun getOutputFor(material: Material, pressType: PressType) = recipes[material to pressType]!!
    
    fun isPressable(material: Material, pressType: PressType) = recipes.containsKey(material to pressType)
    
}

enum class PressType {
    
    PLATE,
    GEAR
    
}