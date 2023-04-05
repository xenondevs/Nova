package xyz.xenondevs.nova.util.data

import net.minecraft.world.item.crafting.Ingredient
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.inventory.*
import org.bukkit.inventory.recipe.CookingBookCategory
import org.bukkit.inventory.recipe.CraftingBookCategory
import xyz.xenondevs.nova.item.logic.PacketItems
import xyz.xenondevs.nova.util.NMSUtils.REGISTRY_ACCESS
import xyz.xenondevs.nova.util.NonNullList
import xyz.xenondevs.nova.util.nmsCopy
import net.minecraft.world.item.crafting.BlastingRecipe as MojangBlastFurnaceRecipe
import net.minecraft.world.item.crafting.CampfireCookingRecipe as MojangCampfireRecipe
import net.minecraft.world.item.crafting.CookingBookCategory as MojangCookingBookCategory
import net.minecraft.world.item.crafting.CraftingBookCategory as MojangCraftingBookCategory
import net.minecraft.world.item.crafting.ShapedRecipe as MojangShapedRecipe
import net.minecraft.world.item.crafting.ShapelessRecipe as MojangShapelessRecipe
import net.minecraft.world.item.crafting.SmeltingRecipe as MojangFurnaceRecipe
import net.minecraft.world.item.crafting.SmokingRecipe as MojangSmokerRecipe
import net.minecraft.world.item.crafting.StonecutterRecipe as MojangStonecutterRecipe

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

internal val RecipeChoice?.nmsIngredient: Ingredient
    get() = when {
        this == null -> Ingredient.EMPTY
        this is RecipeChoice.MaterialChoice -> Ingredient(choices.stream().map { Ingredient.ItemValue(ItemStack(it).nmsCopy) })
        this is RecipeChoice.ExactChoice -> Ingredient(choices.stream().map { Ingredient.ItemValue(it.nmsCopy) })
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

internal fun Ingredient.clientsideCopy(): Ingredient {
    val items = items.map { PacketItems.getClientSideStack(null, it) }
    return Ingredient(items.stream().map { Ingredient.ItemValue(it) })
}

internal fun MojangShapedRecipe.clientsideCopy(): MojangShapedRecipe {
    val result = PacketItems.getClientSideStack(null, getResultItem(REGISTRY_ACCESS))
    val ingredients = NonNullList(ingredients.map { it.clientsideCopy() })
    return MojangShapedRecipe(id, group, category(), width, height, ingredients, result)
}

internal fun MojangShapelessRecipe.clientsideCopy(): MojangShapelessRecipe {
    val result = PacketItems.getClientSideStack(null, getResultItem(REGISTRY_ACCESS))
    val ingredients = NonNullList(ingredients.map { it.clientsideCopy() })
    return MojangShapelessRecipe(id, group, category(), result, ingredients)
}

internal fun MojangFurnaceRecipe.clientsideCopy(): MojangFurnaceRecipe {
    val result = PacketItems.getClientSideStack(null, getResultItem(REGISTRY_ACCESS))
    val ingredient = ingredients.first().clientsideCopy()
    return MojangFurnaceRecipe(id, group, category(), ingredient, result, experience, cookingTime)
}

internal fun MojangBlastFurnaceRecipe.clientsideCopy(): MojangBlastFurnaceRecipe {
    val result = PacketItems.getClientSideStack(null, getResultItem(REGISTRY_ACCESS))
    val ingredient = ingredients.first().clientsideCopy()
    return MojangBlastFurnaceRecipe(id, group, category(), ingredient, result, experience, cookingTime)
}

internal fun MojangSmokerRecipe.clientsideCopy(): MojangSmokerRecipe {
    val result = PacketItems.getClientSideStack(null, getResultItem(REGISTRY_ACCESS))
    val ingredient = ingredients.first().clientsideCopy()
    return MojangSmokerRecipe(id, group, category(), ingredient, result, experience, cookingTime)
}

internal fun MojangCampfireRecipe.clientsideCopy(): MojangCampfireRecipe {
    val result = PacketItems.getClientSideStack(null, getResultItem(REGISTRY_ACCESS))
    val ingredient = ingredients.first().clientsideCopy()
    return MojangCampfireRecipe(id, group, category(), ingredient, result, experience, cookingTime)
}

internal fun MojangStonecutterRecipe.clientsideCopy(): MojangStonecutterRecipe {
    val result = PacketItems.getClientSideStack(null, getResultItem(REGISTRY_ACCESS))
    val ingredient = ingredients.first().clientsideCopy()
    return MojangStonecutterRecipe(id, group, ingredient, result)
}