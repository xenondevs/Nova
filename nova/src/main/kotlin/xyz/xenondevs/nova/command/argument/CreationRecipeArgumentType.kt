package xyz.xenondevs.nova.command.argument

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.recipe.RecipeRegistry

internal object CreationRecipeArgumentType : KeyedArgumentType<String>() {
    override fun getEntries() = RecipeRegistry.CREATION_RECIPES.keys.asSequence() + RecipeRegistry.creationInfo.keys.asSequence()
    override fun toId(t: String) = ResourceLocation.parse(t)
}