package xyz.xenondevs.nova.command.argument

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.world.item.recipe.RecipeRegistry

internal object CreationRecipeArgumentType : KeyedArgumentType<String>() {
    override fun getEntries() = RecipeRegistry.CREATION_RECIPES.keys.asSequence() + RecipeRegistry.creationInfo.keys.asSequence()
    override fun toId(t: String) = Key.key(t)
}