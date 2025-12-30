package xyz.xenondevs.nova.util.item

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.SoundGroup
import org.bukkit.Tag
import org.bukkit.craftbukkit.util.CraftMagicNumbers
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.FluidType
import kotlin.random.Random

@Suppress("DEPRECATION")
val Material?.requiresLight: Boolean
    get() = this != null && !isTransparent && isOccluding

val Material.fluidType: FluidType?
    get() {
        val fluidType = when (this) {
            Material.WATER, Material.BUBBLE_COLUMN -> FluidType.WATER
            Material.LAVA -> FluidType.LAVA
            else -> null
        }
        return fluidType
    }

val Material.localizedName: String?
    get() = CraftMagicNumbers.getItem(this)?.descriptionId

val Material.soundGroup: SoundGroup
    get() = createBlockData().soundGroup

fun Material.isGlass() = name.endsWith("GLASS") || name.endsWith("GLASS_PANE")

fun Material.isTraversable() = isAir || this == Material.WATER || this == Material.BUBBLE_COLUMN || this == Material.LAVA

fun Material.isFluid() = this == Material.WATER || this == Material.BUBBLE_COLUMN || this == Material.LAVA

fun Material.isCauldron() = this == Material.CAULDRON || this == Material.WATER_CAULDRON || this == Material.LAVA_CAULDRON || this == Material.POWDER_SNOW_CAULDRON

fun Material.hasNoBreakParticles() = this == Material.BARRIER || this == Material.STRUCTURE_VOID || this == Material.LIGHT

fun Material.toItemStack(amount: Int = 1): ItemStack = ItemStack.of(this).also { it.amount = amount }

internal fun Material.isBucket() = name.contains("BUCKET")

@Deprecated("Corresponding tag exists", ReplaceWith("Tag.REPLACEABLE.isTagged(this)", "org.bukkit.Tag"))
fun Material.isReplaceable(): Boolean =
    Tag.REPLACEABLE.isTagged(this)

fun Material.playPlaceSoundEffect(location: Location) {
    location.world!!.playSound(location, soundGroup.placeSound, 1f, Random.nextDouble(0.8, 0.95).toFloat())
}
