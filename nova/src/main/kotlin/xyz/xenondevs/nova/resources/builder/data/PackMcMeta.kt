package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xenondevs.nova.serialization.json.serializer.PackVersionMultiFormatSerializer
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
        @SerialName("min_format")
        val minFormat: PackFormatConstraint? = null,
        @SerialName("max_format")
        val maxFormat: PackFormatConstraint? = null,
        @SerialName("pack_format")
        val packFormat: Int? = null,
        @SerialName("supported_formats")
        @Serializable(with = IntRangeMultiFormatSerializer::class)
        val supportedFormats: IntRange? = null
    )
    
    @Serializable(with = PackVersionMultiFormatSerializer::class)
    internal data class PackFormatConstraint(
        val major: Int,
        val minor: Int?
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
            @SerialName("min_format")
            val minFormat: PackFormatConstraint? = null,
            @SerialName("max_format")
            val maxFormat: PackFormatConstraint? = null,
            @Serializable(with = IntRangeMultiFormatSerializer::class)
            val formats: IntRange? = null
        ) {
            
            fun contains(major: Int, minor: Int): Boolean {
                if (minFormat != null && maxFormat != null) {
                    return (major > minFormat.major || (major == minFormat.major && (minFormat.minor == null || minor >= minFormat.minor))) &&
                        (major < maxFormat.major || (major == maxFormat.major && (maxFormat.minor == null || minor <= maxFormat.minor)))
                } else if (formats != null) {
                    return major in formats
                }
                
                return false
            }
            
        }
        
    }
    
    @Serializable
    internal data class Language(
        val name: String,
        val region: String,
        val bidirectional: Boolean
    )
    
}