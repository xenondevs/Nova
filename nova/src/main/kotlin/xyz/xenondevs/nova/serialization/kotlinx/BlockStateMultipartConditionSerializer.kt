package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import xyz.xenondevs.nova.resources.builder.data.BlockStateDefinition.MultipartCase.Condition

internal object BlockStateMultipartConditionSerializer : JsonContentPolymorphicSerializer<Condition>(Condition::class) {
    
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Condition> {
        return when {
            "AND" in element.jsonObject -> Condition.AndCondition.serializer()
            "OR" in element.jsonObject -> Condition.OrCondition.serializer()
            else -> Condition.State.serializer()
        }
    }

}