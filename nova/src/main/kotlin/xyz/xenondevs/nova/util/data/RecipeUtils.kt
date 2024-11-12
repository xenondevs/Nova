package xyz.xenondevs.nova.util.data

import net.minecraft.world.item.crafting.Ingredient
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.inventory.CookingRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.SmithingRecipe
import org.bukkit.inventory.StonecuttingRecipe
import org.bukkit.inventory.recipe.CookingBookCategory
import org.bukkit.inventory.recipe.CraftingBookCategory
import xyz.xenondevs.nova.util.unwrap
import java.util.Optional
import net.minecraft.world.item.crafting.CookingBookCategory as MojangCookingBookCategory
import net.minecraft.world.item.crafting.CraftingBookCategory as MojangCraftingBookCategory

val Recipe.key: NamespacedKey
    get() = (this as Keyed).key

fun Recipe.getInputStacks(): List<ItemStack> =
    when (this) {
        
        is ShapedRecipe -> choiceMap.values.mapNotNull { choice -> choice?.getInputStacks() }.flatten()
        is ShapelessRecipe -> choiceList.map { it.getInputStacks() }.flatten()
        is CookingRecipe<*> -> inputChoice.getInputStacks()
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

internal fun RecipeChoice?.toNmsIngredient(): Optional<Ingredient> =
    if (this == null) Optional.empty() else Optional.of(toNmsIngredient())

internal fun RecipeChoice.toNmsIngredient(): Ingredient =
    when (this) {
        is RecipeChoice.MaterialChoice -> Ingredient.ofStacks(choices.map { ItemStack(it).unwrap().copy() })
        is RecipeChoice.ExactChoice -> Ingredient.ofStacks(choices.map { it.unwrap().copy() })
        else -> throw UnsupportedOperationException("Unsupported RecipeChoice type")
    }

internal val CraftingBookCategory.nmsCategory: MojangCraftingBookCategory
    get() = when (this) {
        CraftingBookCategory.BUILDING -> MojangCraftingBookCategory.BUILDING
        CraftingBookCategory.EQUIPMENT -> MojangCraftingBookCategory.EQUIPMENT
        CraftingBookCategory.REDSTONE -> MojangCraftingBookCategory.REDSTONE
        CraftingBookCategory.MISC -> MojangCraftingBookCategory.MISC
    }

internal val CookingBookCategory.nmsCategory: MojangCookingBookCategory
    get() = when (this) {
        CookingBookCategory.FOOD -> MojangCookingBookCategory.FOOD
        CookingBookCategory.BLOCKS -> MojangCookingBookCategory.BLOCKS
        CookingBookCategory.MISC -> MojangCookingBookCategory.MISC
    }