package xyz.xenondevs.nova.recipe

import org.bukkit.Material
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.recipe.PressType.PLATE

object PressRecipe {
    
    private val recipes = HashMap<Pair<Material, PressType>, NovaMaterial>()
    
    init {
        recipes[Material.IRON_INGOT to PLATE] = NovaMaterial.IRON_PLATE
        recipes[Material.GOLD_INGOT to PLATE] = NovaMaterial.GOLD_PLATE
        recipes[Material.NETHERITE_INGOT to PLATE] = NovaMaterial.NETHERITE_PLATE
        recipes[Material.DIAMOND to PLATE] = NovaMaterial.DIAMOND_PLATE
        recipes[Material.EMERALD to PLATE] = NovaMaterial.EMERALD_PLATE
        recipes[Material.REDSTONE to PLATE] = NovaMaterial.REDSTONE_PLATE
        recipes[Material.LAPIS_LAZULI to PLATE] = NovaMaterial.LAPIS_PLATE
    }
    
    fun getOutputFor(material: Material, pressType: PressType) = recipes[material to pressType]!!
    
    fun isPressable(material: Material, pressType: PressType) = recipes.containsKey(material to pressType)
    
}

enum class PressType {
    
    PLATE,
    GEAR
    
}