package xyz.xenondevs.nova.data.recipe

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice

interface NovaRecipe {
    val key: NamespacedKey
    val type: RecipeType<out NovaRecipe>
}

abstract class ConversionNovaRecipe(
    override val key: NamespacedKey,
    val input: RecipeChoice,
    val result: ItemStack,
    val time: Int
) : NovaRecipe

class PulverizerNovaRecipe(
    key: NamespacedKey,
    input: RecipeChoice,
    result: ItemStack,
    time: Int,
    override val type: RecipeType<PulverizerNovaRecipe> = RecipeType.PULVERIZER
) : ConversionNovaRecipe(key, input, result, time)

class PlatePressNovaRecipe(
    key: NamespacedKey,
    input: RecipeChoice,
    result: ItemStack,
    time: Int,
    override val type: RecipeType<PlatePressNovaRecipe> = RecipeType.PLATE_PRESS
) : ConversionNovaRecipe(key, input, result, time)

class GearPressNovaRecipe(
    key: NamespacedKey,
    input: RecipeChoice,
    result: ItemStack,
    time: Int,
    override val type: RecipeType<GearPressNovaRecipe> = RecipeType.GEAR_PRESS
) : ConversionNovaRecipe(key, input, result, time)