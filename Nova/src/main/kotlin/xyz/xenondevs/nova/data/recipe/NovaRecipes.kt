package xyz.xenondevs.nova.data.recipe

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType

/**
 * The interface for all recipes for Nova blocks.
 */
interface NovaRecipe {
    val key: NamespacedKey
    val type: RecipeType<out NovaRecipe>
}

/**
 * The interface for all recipes that have a result [ItemStack].
 */
interface ResultingRecipe {
    val result: ItemStack
}

/**
 * The interface for all recipes that have one or more input choice(s).
 */
interface InputChoiceRecipe {
    fun getAllInputs(): List<RecipeChoice> = when (this) {
        is SingleInputChoiceRecipe -> listOf(input)
        is MultiInputChoiceRecipe -> inputs
        else -> throw UnsupportedOperationException()
    }
}

/**
 * The interface for all recipes that have only one input choice.
 */
interface SingleInputChoiceRecipe {
    val input: RecipeChoice
}

/**
 * The interface for all recipes that have multiple input choices.
 */
interface MultiInputChoiceRecipe {
    val inputs: List<RecipeChoice>
}

abstract class ConversionNovaRecipe(
    override val key: NamespacedKey,
    override val input: RecipeChoice,
    override val result: ItemStack,
    val time: Int
) : NovaRecipe, ResultingRecipe, SingleInputChoiceRecipe

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

class MechanicalBrewingStandRecipe(
    override val key: NamespacedKey,
    override val inputs: List<RecipeChoice>,
    val result: PotionEffectType,
    val defaultTime: Int,
    val redstoneMultiplier: Double,
    val glowstoneMultiplier: Double,
    val maxDurationLevel: Int,
    val maxAmplifierLevel: Int
) : NovaRecipe, MultiInputChoiceRecipe {
    override val type = RecipeType.MECHANICAL_BREWING_STAND
}