package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.serialization.json.serializer.RecipeDeserializer
import xyz.xenondevs.nova.ui.menu.explorer.recipes.group.RecipeGroup
import xyz.xenondevs.nova.world.item.recipe.NovaRecipe
import xyz.xenondevs.nova.world.item.recipe.RecipeType
import kotlin.reflect.KClass

@Deprecated(REGISTRIES_DEPRECATION)
interface RecipeTypeRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun <T : NovaRecipe> registerRecipeType(name: String, recipeClass: KClass<T>, group: RecipeGroup<in T>, deserializer: RecipeDeserializer<T>?): RecipeType<T> =
        addon.registerRecipeType(name, recipeClass, group, deserializer)
    
}