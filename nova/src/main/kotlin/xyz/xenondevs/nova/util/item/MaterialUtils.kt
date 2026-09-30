package xyz.xenondevs.nova.util.item

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.SoundGroup
import org.bukkit.craftbukkit.util.CraftMagicNumbers
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.DEBLOAT_DEPRECATE
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.FluidType
import kotlin.random.Random

@Deprecated("Prefer BlockType over Material")
val Material?.requiresLight: Boolean
    get() = this != null && !isTransparent && isOccluding

@Deprecated("Prefer BlockType over Material")
val Material.localizedName: String?
    get() = CraftMagicNumbers.getItem(this)?.descriptionId

@Deprecated("Prefer BlockType over Material")
val Material.soundGroup: SoundGroup
    get() = createBlockData().soundGroup

@Deprecated("Prefer BlockType over Material")
fun Material.isTraversable() = isAir || this == Material.WATER || this == Material.BUBBLE_COLUMN || this == Material.LAVA

@Deprecated("Prefer BlockType over Material")
fun Material.playPlaceSoundEffect(location: Location) {
    location.world!!.playSound(location, soundGroup.placeSound, 1f, Random.nextDouble(0.8, 0.95).toFloat())
}
