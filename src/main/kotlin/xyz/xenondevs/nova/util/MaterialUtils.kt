package xyz.xenondevs.nova.util

import de.studiocode.invui.item.ItemBuilder
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.SoundGroup
import org.bukkit.craftbukkit.v1_18_R1.util.CraftMagicNumbers
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.RecipeChoice.MaterialChoice
import xyz.xenondevs.nova.data.recipe.NovaRecipeChoice
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import kotlin.math.absoluteValue
import kotlin.random.Random

fun Material.isGlass() = name.endsWith("GLASS") || name.endsWith("GLASS_PANE")

fun Material.toItemStack(amount: Int = 1): ItemStack = ItemBuilder(this).setAmount(amount).get()

fun Material.isTraversable() = isAir || name == "WATER" || name == "LAVA"

fun Material.isBreakable() = blastResistance < 3600000.0f

/**
 * The break speed for a specific material, always positive.
 */
val Material.breakSpeed: Double
    get() = 1.0 / hardness.absoluteValue

val Material.localizedName: String?
    get() = CraftMagicNumbers.getItem(this)?.descriptionId

val ItemStack.novaMaterial: NovaMaterial?
    get() {
        val customModelData = customModelData
        val material = NovaMaterialRegistry.getOrNull(customModelData)
        if (material != null && material.item.material == type) return material
        return null
    }

val ItemStack.customModelData: Int
    get() {
        if (hasItemMeta()) {
            val itemMeta = itemMeta!!
            if (itemMeta.hasCustomModelData()) return itemMeta.customModelData
        }
        
        return 0
    }

val Material.soundGroup: SoundGroup
    get() = createBlockData().soundGroup

fun Material.playPlaceSoundEffect(location: Location) {
    location.world!!.playSound(location, soundGroup.placeSound, 1f, Random.nextDouble(0.8, 0.95).toFloat())
}

@Suppress("LiftReturnOrAssignment", "CascadeIf")
object MaterialUtils {
    
    fun getRecipeChoice(name: String): RecipeChoice {
        try {
            if (name.startsWith("nova:")) {
                val material = NovaMaterialRegistry.get(name.drop(5).uppercase())
                return NovaRecipeChoice(material)
            } else if (name.startsWith("minecraft:")) {
                val material = Material.valueOf(name.drop(10).uppercase())
                return MaterialChoice(material)
            } else throw IllegalArgumentException("Invalid item name: $name")
        } catch (ex: Exception) {
            throw IllegalArgumentException("Unknown item $name", ex)
        }
    }
    
}