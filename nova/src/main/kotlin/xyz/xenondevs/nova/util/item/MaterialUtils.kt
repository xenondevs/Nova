package xyz.xenondevs.nova.util.item

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.SoundGroup
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

/**
 * More reliable function compared to the Spigot API function [Material.isInteractable].
 * From https://www.spigotmc.org/threads/check-if-a-block-is-interactable.535861/
 * @author LoneDev
 */
@Suppress("DEPRECATION")
fun Material.isActuallyInteractable(): Boolean {
    return if (!isInteractable) false else when (this) {
        Material.ACACIA_STAIRS, Material.ANDESITE_STAIRS, Material.BIRCH_STAIRS, Material.BLACKSTONE_STAIRS,
        Material.BRICK_STAIRS, Material.COBBLESTONE_STAIRS, Material.CRIMSON_STAIRS, Material.DARK_OAK_STAIRS,
        Material.DARK_PRISMARINE_STAIRS, Material.DIORITE_STAIRS, Material.END_STONE_BRICK_STAIRS, Material.GRANITE_STAIRS,
        Material.JUNGLE_STAIRS, Material.MOSSY_COBBLESTONE_STAIRS, Material.MOSSY_STONE_BRICK_STAIRS, Material.NETHER_BRICK_STAIRS,
        Material.OAK_STAIRS, Material.POLISHED_ANDESITE_STAIRS, Material.POLISHED_BLACKSTONE_BRICK_STAIRS, Material.POLISHED_BLACKSTONE_STAIRS,
        Material.POLISHED_DIORITE_STAIRS, Material.POLISHED_GRANITE_STAIRS, Material.PRISMARINE_BRICK_STAIRS, Material.PRISMARINE_STAIRS,
        Material.PURPUR_STAIRS, Material.QUARTZ_STAIRS, Material.RED_NETHER_BRICK_STAIRS, Material.RED_SANDSTONE_STAIRS,
        Material.SANDSTONE_STAIRS, Material.SMOOTH_QUARTZ_STAIRS, Material.SMOOTH_RED_SANDSTONE_STAIRS, Material.SMOOTH_SANDSTONE_STAIRS,
        Material.SPRUCE_STAIRS, Material.STONE_BRICK_STAIRS, Material.STONE_STAIRS, Material.WARPED_STAIRS,
        Material.ACACIA_FENCE, Material.BIRCH_FENCE, Material.CRIMSON_FENCE, Material.DARK_OAK_FENCE,
        Material.JUNGLE_FENCE, Material.MOVING_PISTON, Material.NETHER_BRICK_FENCE, Material.OAK_FENCE,
        Material.PUMPKIN, Material.REDSTONE_ORE, Material.REDSTONE_WIRE, Material.SPRUCE_FENCE,
        Material.WARPED_FENCE -> false
        
        else -> true
    }
}

fun Material.isReplaceable(): Boolean =
    when (this) {
        Material.AIR, Material.CAVE_AIR, Material.VOID_AIR, Material.WATER, Material.LAVA,
        Material.SHORT_GRASS, Material.TALL_GRASS, Material.FERN, Material.DEAD_BUSH, Material.VINE,
        Material.CRIMSON_ROOTS, Material.WARPED_ROOTS, Material.NETHER_SPROUTS, Material.SEAGRASS, Material.FIRE,
        Material.SOUL_FIRE, Material.SNOW, Material.STRUCTURE_VOID, Material.LIGHT -> true
        
        else -> false
    }

fun Material.playPlaceSoundEffect(location: Location) {
    location.world!!.playSound(location, soundGroup.placeSound, 1f, Random.nextDouble(0.8, 0.95).toFloat())
}
