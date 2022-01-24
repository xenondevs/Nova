package xyz.xenondevs.nova.util.data

import net.minecraft.world.item.crafting.Ingredient
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.inventory.*
import xyz.xenondevs.nova.util.nmsStack

val Recipe.key: NamespacedKey
    get() = (this as Keyed).key

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

val RecipeChoice?.nmsIngredient: Ingredient
    get() = when {
        this == null -> Ingredient.EMPTY
        this is RecipeChoice.MaterialChoice -> Ingredient(choices.stream().map { Ingredient.ItemValue(ItemStack(it).nmsStack) })
        this is RecipeChoice.ExactChoice -> Ingredient(choices.stream().map { Ingredient.ItemValue(it.nmsStack) })
        else -> throw UnsupportedOperationException("Unsupported RecipeChoice type")
    }.apply { dissolve() }