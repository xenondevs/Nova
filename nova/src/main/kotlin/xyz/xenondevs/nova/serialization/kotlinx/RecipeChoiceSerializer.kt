package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.nova.serialization.json.serializer.RecipeDeserializer

/**
 * Serializes [RecipeChoice] from a list of item id strings, or a single string (which is
 * treated as a one-element list). Uses [RecipeDeserializer.parseRecipeChoice] for parsing.
 * Serialization is not supported.
 */
internal object RecipeChoiceSerializer : KSerializer<RecipeChoice> {
    
    private val delegate = ValueOrListSerializer(String.serializer())
    override val descriptor = delegate.descriptor
    
    override fun deserialize(decoder: Decoder): RecipeChoice {
        val list = delegate.deserialize(decoder)
        try {
            return RecipeDeserializer.parseRecipeChoice(list)
        } catch (e: IllegalArgumentException) {
            throw SerializationException(e.message, e)
        }
    }
    
    override fun serialize(encoder: Encoder, value: RecipeChoice) {
        throw UnsupportedOperationException("RecipeChoice serialization is not supported")
    }
    
}
