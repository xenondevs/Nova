package xyz.xenondevs.nova.util.data

import org.bukkit.inventory.*

fun Recipe.getInputStacks(): List<ItemStack> =
    when (this) {
        
        is ShapedRecipe -> choiceMap.values.mapNotNull { choice -> choice?.getInputStacks() }.flatten()
        is ShapelessRecipe -> choiceList.map { it.getInputStacks() }.flatten()
        is FurnaceRecipe -> inputChoice.getInputStacks()
        is StonecuttingRecipe -> inputChoice.getInputStacks()
        is SmithingRecipe -> base.getInputStacks() + addition.getInputStacks()
        
        else -> throw UnsupportedOperationException("Unsupported Recipe type: ${javaClass.name}")
    }


fun RecipeChoice.getInputStacks(): List<ItemStack> =
    when (this) {
        is RecipeChoice.MaterialChoice -> choices.map(::ItemStack)
        is RecipeChoice.ExactChoice -> choices
        else -> throw UnsupportedOperationException("Unknown RecipeChoice type: ${javaClass.name}")
    }
    