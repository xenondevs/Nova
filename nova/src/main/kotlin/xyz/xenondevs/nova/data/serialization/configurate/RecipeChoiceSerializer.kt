package xyz.xenondevs.nova.data.serialization.configurate

import io.leangen.geantyref.TypeToken
import org.bukkit.inventory.RecipeChoice
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import xyz.xenondevs.nova.data.serialization.json.serializer.RecipeDeserializer
import xyz.xenondevs.nova.util.data.getList
import java.lang.reflect.Type

internal object RecipeChoiceSerializer : TypeSerializer<RecipeChoice> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): RecipeChoice {
        val list = node.getList<String>() ?: throw NoSuchElementException("Missing value")
        return RecipeDeserializer.parseRecipeChoice(list)
    }
    
    override fun serialize(type: Type?, obj: RecipeChoice?, node: ConfigurationNode?) =
        throw UnsupportedOperationException()
    
}