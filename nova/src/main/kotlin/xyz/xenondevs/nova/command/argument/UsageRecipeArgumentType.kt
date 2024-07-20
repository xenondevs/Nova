package xyz.xenondevs.nova.command.argument

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.recipe.RecipeRegistry

internal object UsageRecipeArgumentType : KeyedArgumentType<String>() {
    override fun getEntries() = RecipeRegistry.USAGE_RECIPES.keys.asSequence()
    override fun toId(t: String) = ResourceLocation.parse(t)
}