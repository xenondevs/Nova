package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xenondevs.commons.collections.containsAll
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.serialization.kotlinx.BlockStateMultipartConditionSerializer
import xyz.xenondevs.nova.serialization.kotlinx.BlockStateMultipartStateConditionSerializer
import xyz.xenondevs.nova.serialization.kotlinx.BlockStateVariantAsStringSerializer
import xyz.xenondevs.nova.serialization.kotlinx.ValueOrListSerializer

/**
 * A [block state definition](https://minecraft.wiki/w/Tutorial:Models#Block_states).
 */
@Serializable
data class BlockStateDefinition(
    val variants: Map<Variant, @Serializable(with = ValueOrListSerializer::class) List<Model>> = emptyMap(),
    val multipart: List<MultipartCase> = emptyList()
) {
    
    init {
        require(variants.isEmpty() != multipart.isEmpty()) { "BlockStateDefinition must have either 'variants' or 'multipart'" }
    }
    
    @Serializable(with = BlockStateVariantAsStringSerializer::class)
    data class Variant(val properties: Map<String, String>)
    
    @Serializable
    data class MultipartCase(
        @SerialName("when")
        val condition: Condition? = null,
        @Serializable(with = ValueOrListSerializer::class)
        val apply: List<Model>
    ) {
        
        @Serializable(with = BlockStateMultipartConditionSerializer::class)
        sealed interface Condition {
            
            @Serializable
            data class AndCondition(@SerialName("AND") val terms: List<Condition>) : Condition {
                override fun matches(properties: Map<String, String>): Boolean = terms.all { it.matches(properties) }
            }
            
            @Serializable
            data class OrCondition(@SerialName("OR") val terms: List<Condition>) : Condition {
                override fun matches(properties: Map<String, String>): Boolean = terms.any { it.matches(properties) }
            }
            
            @Serializable(with = BlockStateMultipartStateConditionSerializer::class)
            data class State(val properties: Map<String, Set<String>>) : Condition {
                
                override fun matches(properties: Map<String, String>): Boolean {
                    for ((key, values) in this.properties) {
                        val actualValue = properties[key] ?: return false
                        if (actualValue !in values) return false
                    }
                    return true
                }
                
            }
            
            /**
             * Checks whether this condition matches the given [properties].
             */
            fun matches(properties: Map<String, String>): Boolean
            
        }
        
        /**
         * Checks whether this multipart case matches the given [properties].
         */
        fun matches(properties: Map<String, String>): Boolean {
            return condition?.matches(properties) ?: true
        }
        
    }
    
    @Serializable
    data class Model(
        val model: ResourcePath<ResourceType.Model>,
        val x: Int = 0,
        val y: Int = 0,
        val uvlock: Boolean = false,
        val weight: Int = 1
    )
    
    /**
     * Gets the [models][Model] to be applied when the given [properties] are present.
     */
    fun getModels(properties: Map<String, String>): List<Model> {
        if (variants.isNotEmpty()) {
            // TODO: determine if this is the correct behavior
            return variants.entries
                .firstOrNull { (variant, _) -> properties.containsAll(variant.properties) }
                ?.value ?: emptyList()
        } else if (multipart.isNotEmpty()) {
            return multipart.flatMap { if (it.matches(properties)) it.apply else emptyList() }
        }
        
        return emptyList()
    }
    
}