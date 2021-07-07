package xyz.xenondevs.nova.util

import de.studiocode.invui.item.ItemBuilder
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.SoundGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.RecipeChoice.MaterialChoice
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.recipe.NovaRecipeChoice
import kotlin.math.absoluteValue
import kotlin.random.Random

fun Material.isGlass() = name.endsWith("GLASS") || name.endsWith("GLASS_PANE")

fun Material.toItemStack(amount: Int = 1): ItemStack = ItemBuilder(this).setAmount(amount).build()

fun Material.isTraversable() = isAir || name == "WATER" || name == "LAVA"

fun Material.isBreakable() = blastResistance < 3600000.0f

/**
 * The break speed for a specific material, always positive.
 */
val Material.breakSpeed: Double
    get() = 1.0 / hardness.absoluteValue

val ItemStack.novaMaterial: NovaMaterial?
    get() = NovaMaterial.values().find {
        val itemStack = it.createItemStack()
        
        this.type == itemStack.type
            && hasItemMeta()
            && itemMeta!!.hasCustomModelData()
            && itemMeta!!.customModelData == itemStack.itemMeta!!.customModelData
    }

val Material.soundGroup: SoundGroup
    get() = createBlockData().soundGroup

fun Material.playPlaceSoundEffect(location: Location) {
    location.world!!.playSound(location, soundGroup.placeSound, 1f, Random.nextDouble(0.8, 0.95).toFloat())
}

@Suppress("LiftReturnOrAssignment", "CascadeIf")
object MaterialUtils {
    
    fun getRecipeChoice(name: String): RecipeChoice {
        if (name.startsWith("nova:")) {
            val material = NovaMaterial.valueOf(name.drop(5).uppercase())
            return NovaRecipeChoice(material)
        } else if (name.startsWith("minecraft:")) {
            val material = Material.valueOf(name.drop(10).uppercase())
            return MaterialChoice(material)
        } else throw IllegalArgumentException("Invalid item name: $name")
    }
    
}