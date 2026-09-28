@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Retrieves a contextual serializer for the given [T] and, if [T] is not registered
 * for contextual serialization, falls back to the default serializer.
 *
 * This overload works with full type information, including type arguments and nullability.
 * Variance of [T]'s arguments is not taken into account. Star projections are prohibited.
 *
 * @throws SerializationException if a serializer cannot be created.
 * @throws IllegalArgumentException if any of [T]'s arguments contains a star projection.
 */
inline fun <reified T> SerializersModule.contextualSerializer(): KSerializer<T> =
    contextualSerializer(typeOf<T>()) as KSerializer<T>

/**
 * Retrieves a contextual serializer for the given [type] and, if [type] is not registered
 * for contextual serialization, falls back to the default serializer.
 *
 * This overload works with full type information, including type arguments and nullability.
 * Variance of [type]'s arguments is not taken into account. Star projections are prohibited.
 *
 * @throws SerializationException if a serializer cannot be created.
 * @throws IllegalArgumentException if any of [type]'s arguments contains a star projection.
 */
fun SerializersModule.contextualSerializer(type: KType): KSerializer<Any?> {
    val kClass = type.classifier as KClass<Any>
    val argumentTypes = type.arguments.map {
        requireNotNull(it.type) { "Star projections are not supported: $type" }
    }
    val argumentSerializers = argumentTypes.map(::contextualSerializer)
    
    val serializer = getContextual(kClass, argumentSerializers)
        ?: if (kClass == Array::class) {
            ArraySerializer(
                argumentTypes.single().classifier as KClass<Any>,
                argumentSerializers.single() as KSerializer<Any>,
            )
        } else {
            serializer(kClass, argumentSerializers, false) as KSerializer<Any>
        }
    
    return (if (type.isMarkedNullable) serializer.nullable else serializer) as KSerializer<Any?>
}
