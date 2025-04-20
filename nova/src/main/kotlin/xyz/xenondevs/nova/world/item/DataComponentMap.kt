package xyz.xenondevs.nova.world.item

import io.papermc.paper.datacomponent.DataComponentType
import io.papermc.paper.datacomponent.PaperDataComponentType
import net.minecraft.core.component.TypedDataComponent
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.mapNonNull
import xyz.xenondevs.commons.provider.provider
import net.minecraft.core.component.DataComponentMap as MojangDataComponentMap
import net.minecraft.core.component.DataComponentType as MojangDataComponentType
import net.minecraft.util.Unit as MojangUnit

/**
 * Builds a [DataComponentMap] using the given [builderAction].
 */
fun buildDataComponentMap(builderAction: DataComponentMap.Builder.() -> Unit): DataComponentMap {
    val builder = DataComponentMap.builder()
    builder.builderAction()
    return builder.build()
}

/**
 * Builds a [DataComponentMap] [Provider] using the given [builderAction].
 */
fun buildDataComponentMapProvider(builderAction: DataComponentMap.ProviderBuilder.() -> Unit): Provider<DataComponentMap> {
    val builder = DataComponentMap.providerBuilder()
    builder.builderAction()
    return builder.build()
}

/**
 * Maps [DataComponentType DataComponentTypes] to their values.
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
        operator fun <T : Any> set(type: MojangDataComponentType<T>, value: T): Builder {
            builder.set(type, value)
            return this
        }
        
        /**
         * Sets the value of the given [type] to the given [value].
         */
        operator fun <T : Any> set(type: DataComponentType.Valued<T>, value: T): Builder {
            builder.set(
                PaperDataComponentType.bukkitToMinecraft(type),
                (type as PaperDataComponentType.ValuedImpl<T, *>).adapter.toVanilla(value)
            )
            return this
        }
        
        /**
         * Sets the given [type] without a value.
         */
        fun set(type: DataComponentType.NonValued): Builder {
            builder.set(PaperDataComponentType.bukkitToMinecraft(type), MojangUnit.INSTANCE)
            return this
        }
        
        fun build() = DataComponentMap(builder.build())
        
    }
    
    /**
     * A builder for [DataComponentMap] [Provider Providers].
     */
    class ProviderBuilder internal constructor() {
        
        private val components = ArrayList<Provider<TypedDataComponent<*>?>>()
        
        /**
         * Sets the value of the given [type] to the given [value].
         */
        operator fun <T : Any> set(type: MojangDataComponentType<T>, value: Provider<T?>): ProviderBuilder {
            components += value.mapNonNull { TypedDataComponent(type, it) }
            return this
        }
        
        /**
         * Sets the value of the given [type] to the given [value].
         */
        operator fun <T : Any> set(type: DataComponentType.Valued<T>, value: Provider<T?>): ProviderBuilder {
            components += value.mapNonNull { value ->
                TypedDataComponent(
                    PaperDataComponentType.bukkitToMinecraft(type),
                    (type as PaperDataComponentType.ValuedImpl<T, *>).adapter.toVanilla(value)
                )
            }
            return this
        }
        
        /**
         * Sets the value of the given [type] to the given [value].
         */
        operator fun <T : Any> set(type: MojangDataComponentType<T>, value: T): ProviderBuilder {
            components += provider(TypedDataComponent(type, value))
            return this
        }
        
        /**
         * Sets the value of the given [type] to the given [value].
         */
        operator fun <T : Any> set(type: DataComponentType.Valued<T>, value: T): ProviderBuilder {
            set(type, provider(value))
            return this
        }
        
        /**
         * Sets the given [type] without a value.
         */
        fun set(type: DataComponentType.NonValued): ProviderBuilder {
            components += provider(TypedDataComponent(PaperDataComponentType.bukkitToMinecraft(type), MojangUnit.INSTANCE))
            return this
        }
        
        /**
         * Builds the [DataComponentMap].
         */
        fun build(): Provider<DataComponentMap> = combinedProvider(components) { components ->
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
        
        /**
         * Creates a new [Builder].
         */
        fun builder() = Builder()
        
        /**
         * Creates a new [ProviderBuilder].
         */
        fun providerBuilder() = ProviderBuilder()
        
    }
    
}