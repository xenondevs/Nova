package xyz.xenondevs.nova.data.recipe

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice

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
