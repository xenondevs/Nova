package xyz.xenondevs.nova.data.recipe

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType

interface NovaRecipe {
    val key: NamespacedKey
    val result: ItemStack
}

interface SerializableNovaRecipe : NovaRecipe {
    val type: RecipeType<out SerializableNovaRecipe>
}

abstract class ConversionNovaRecipe(
    override val key: NamespacedKey,
    val input: RecipeChoice,
    override val result: ItemStack,
    val time: Int
) : SerializableNovaRecipe

class PulverizerRecipe(
    key: NamespacedKey,
    input: RecipeChoice,
    result: ItemStack,
    time: Int,
) : ConversionNovaRecipe(key, input, result, time) {
    override val type = RecipeType.PULVERIZER
}

class PlatePressRecipe(
    key: NamespacedKey,
    input: RecipeChoice,
    result: ItemStack,
    time: Int
) : ConversionNovaRecipe(key, input, result, time) {
    override val type = RecipeType.PLATE_PRESS
}

class GearPressRecipe(
    key: NamespacedKey,
    input: RecipeChoice,
    result: ItemStack,
    time: Int
) : ConversionNovaRecipe(key, input, result, time) {
    override val type = RecipeType.GEAR_PRESS
}

class FluidInfuserRecipe(
    override val key: NamespacedKey,
    val mode: InfuserMode,
    val fluidType: FluidType,
    val fluidAmount: Long,
    input: RecipeChoice,
    result: ItemStack,
    time: Int
) : ConversionNovaRecipe(key, input, result, time) {
    override val type = RecipeType.FLUID_INFUSER
    
    enum class InfuserMode {
        INSERT,
        EXTRACT
    }
}