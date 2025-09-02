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
        @SerialName("uvlock")
        val uvLock: Boolean = false,
        val weight: Int = 1
    )
    
    /**
     * Gets the [models][Model] to be applied when the given [properties] are present.
     * If both [variants] and [multipart] are defined, [variants] take precedence.
     * Only if there is no matching variant, [multipart] is checked.
     */
    fun getModels(properties: Map<String, String>): List<List<Model>> {
        val models = ArrayList<List<Model>>()
        
        // variants take precedence over multipart
        for ((variant, variantModels) in variants) {
            if (properties.containsAll(variant.properties)) {
                models += variantModels
            }
        }
        
        if (models.isNotEmpty())
            return models
        
        // only if no variant matched, check multipart
        for (case in multipart) {
            if (case.matches(properties)) {
                models += case.apply
            }
        }
        
        return models
    }
    
}