package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal fun googleToKotlinxJson(element: com.google.gson.JsonElement): JsonElement {
    return when (element) {
        is com.google.gson.JsonNull -> JsonNull
        is com.google.gson.JsonPrimitive -> when {
            element.isNumber -> JsonPrimitive(element.asNumber)
            element.isString -> JsonPrimitive(element.asString)
            element.isBoolean -> JsonPrimitive(element.asBoolean)
            else -> JsonPrimitive(element.asString)
        }
        
        is com.google.gson.JsonArray -> JsonArray(element.map(::googleToKotlinxJson))
        is com.google.gson.JsonObject -> JsonObject(element.asMap().mapValues { (_, value) -> googleToKotlinxJson(value) })
        else -> throw UnsupportedOperationException("Unsupported JsonElement type: ${element.javaClass.name}")
    }
}