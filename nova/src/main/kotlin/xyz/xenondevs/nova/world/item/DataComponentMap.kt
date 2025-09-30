package xyz.xenondevs.nova.world.item

import io.papermc.paper.datacomponent.DataComponentType
import io.papermc.paper.datacomponent.PaperDataComponentType
import net.minecraft.core.component.TypedDataComponent
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.commons.provider.mapNonNull
import xyz.xenondevs.commons.provider.provider
import net.minecraft.core.component.DataComponentMap as MojangDataComponentMap
import net.minecraft.core.component.DataComponentType as MojangDataComponentType
import net.minecraft.util.Unit as MojangUnit

/**
 * Builds a [DataComponentMap] using the given [builderAction].
 */
fun buildDataComponentMap(builderAction: DataComponentMap.Builder.() -> Unit): DataComponentMap {
    val builder = DataComponentMap.Builder()
    builder.builderAction()
    return builder.build()
}

/**
 * Builds a [DataComponentMap] [Provider] lazily using the given [builderAction].
 * 
 * [Providers][Provider] can be used to introduce config-reloadable parts to your item's the data components:
 * ```kotlin
 * val level: Provider<Int> = // ...
 * val dcm: Provider<DataComponentMap> = buildDataComponentMapProvider {
 *     // The enchantable component updates when the level provider updates
 *     this[DataComponentTypes.ENCHANTABLE] = level.map { level -> Enchantable.enchantable(level) }
 * }
 * ```
 */
fun buildDataComponentMapProvider(builderAction: DataComponentMap.ProviderBuilder.() -> Unit): Provider<DataComponentMap> {
    return provider {
        val builder = DataComponentMap.ProviderBuilder()
        builder.builderAction()
        builder.build()
    }.flatten()
}

/**
 * Maps [DataComponentType DataComponentTypes] to their values.
 *
 * @see buildDataComponentMap
 * @see buildDataComponentMapProvider
 */
class DataComponentMap internal constructor(
    internal val handle: MojangDataComponentMap
) {
    
    /**
     * Gets the value under the given [type], or `null` if there is no value.
     */
    operator fun <T : Any> get(type: DataComponentType.Valued<T>): T? {
        return PaperDataComponentType.convertDataComponentValue(handle, type as PaperDataComponentType.ValuedImpl<T, *>)
    }
    
    /**
     * Checks whether the map contains a value for the given [type].
     */
    operator fun contains(type: DataComponentType): Boolean {
        return handle.has(PaperDataComponentType.bukkitToMinecraft<Any>(type))
    }
    
    /**
     * A builder for [DataComponentMap DataComponentMaps].
     */
    class Builder internal constructor() {
        
        private val builder = MojangDataComponentMap.builder()
        
        /**
         * Sets the value of the given [type] to the given [value].
         */
        operator fun <T : Any> set(type: MojangDataComponentType<T>, value: T) {
            builder.set(type, value)
        }
        
        /**
         * Sets the value of the given [type] to the given [value].
         */
        operator fun <T : Any> set(type: DataComponentType.Valued<T>, value: T) {
            type as PaperDataComponentType.ValuedImpl<T, *>
            set(type, value)
        }
        
        private fun <T : Any, NMS : Any> set(type: PaperDataComponentType.ValuedImpl<T, NMS>, value: T) {
            builder.set(
                PaperDataComponentType.bukkitToMinecraft(type),
                type.adapter.toVanilla(value, type.holder)
            )
        }
        
        /**
         * Sets the given [type] without a value.
         */
        fun set(type: DataComponentType.NonValued) {
            builder.set(PaperDataComponentType.bukkitToMinecraft(type), MojangUnit.INSTANCE)
        }
        
        internal fun build() = DataComponentMap(builder.build())
        
    }
    
    /**
     * A builder for [DataComponentMap] [Provider Providers].
     */
    class ProviderBuilder internal constructor() {
        
        private val components = ArrayList<Provider<TypedDataComponent<*>?>>()
        
        /**
         * Sets the value of the given [type] to the given [value].
         */
        operator fun <T : Any> set(type: MojangDataComponentType<T>, value: Provider<T?>) {
            components += value.mapNonNull { TypedDataComponent(type, it) }
        }
        
        /**
         * Sets the value of the given [type] to the given [value].
         */
        operator fun <T : Any> set(type: DataComponentType.Valued<T>, value: Provider<T?>) {
            components += value.mapNonNull { createTypedDataComponent(type as PaperDataComponentType.ValuedImpl<T, *>, it) }
        }
        
        private fun <T : Any, NMS : Any> createTypedDataComponent(type: PaperDataComponentType.ValuedImpl<T, NMS>, value: T) =
            TypedDataComponent<NMS>(
                PaperDataComponentType.bukkitToMinecraft(type),
                type.adapter.toVanilla(value, type.holder)
            )
        
        /**
         * Sets the value of the given [type] to the given [value].
         */
        operator fun <T : Any> set(type: MojangDataComponentType<T>, value: T) {
            components += provider(TypedDataComponent(type, value))
        }
        
        /**
         * Sets the value of the given [type] to the given [value].
         */
        operator fun <T : Any> set(type: DataComponentType.Valued<T>, value: T) {
            set(type, provider(value))
        }
        
        /**
         * Sets the given [type] without a value.
         */
        fun set(type: DataComponentType.NonValued) {
            components += provider(TypedDataComponent(PaperDataComponentType.bukkitToMinecraft(type), MojangUnit.INSTANCE))
        }
        
        /**
         * Builds the [DataComponentMap].
         */
        internal fun build(): Provider<DataComponentMap> = combinedProvider(components) { components ->
            val builder = MojangDataComponentMap.builder()
            for (component in components) {
                if (component == null)
                    continue
                builder.set(component.type as MojangDataComponentType<Any>, component.value as Any)
            }
            return@combinedProvider DataComponentMap(builder.build())
        }
        
    }
    
    companion object {
        
        /**
         * An empty [DataComponentMap].
         */
        val EMPTY = DataComponentMap(MojangDataComponentMap.EMPTY)
        
    }
    
}