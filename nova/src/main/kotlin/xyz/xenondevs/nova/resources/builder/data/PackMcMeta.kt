package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xenondevs.nova.serialization.kotlinx.IntRangeMultiFormatSerializer

@Serializable
internal data class PackMcMeta(
    val pack: Pack,
    val features: Features? = null,
    val filter: Filters? = null,
    val overlays: Overlays? = null,
    val language: Map<String, Language> = emptyMap()
) {
    
    @Serializable
    internal data class Pack(
        val description: String,
        @SerialName("pack_format")
        val packFormat: Int,
        @SerialName("supported_formats")
        @Serializable(with = IntRangeMultiFormatSerializer::class)
        val supportedFormats: IntRange? = null
    )
    
    @Serializable
    internal data class Filters(
        val block: List<Filter>
    ) {
        
        @Serializable
        internal data class Filter(
            val namespace: String,
            val path: String
        )
        
    }
    
    @Serializable
    internal data class Features(
        val enabled: List<String>
    )
    
    @Serializable
    internal data class Overlays(
        val entries: List<Entry>
    ) {
        
        @Serializable
        internal data class Entry(
            val directory: String,
            @Serializable(with = IntRangeMultiFormatSerializer::class)
            val formats: IntRange
        )
        
    }
    
    @Serializable
    internal data class Language(
        val name: String,
        val region: String,
        val bidirectional: Boolean
    )
    
}