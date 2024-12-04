package xyz.xenondevs.nova.command.argument

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.world.item.recipe.RecipeRegistry

internal object UsageRecipeArgumentType : KeyedArgumentType<String>() {
    override fun getEntries() = RecipeRegistry.USAGE_RECIPES.keys.asSequence()
    override fun toId(t: String) = Key.key(t)
}