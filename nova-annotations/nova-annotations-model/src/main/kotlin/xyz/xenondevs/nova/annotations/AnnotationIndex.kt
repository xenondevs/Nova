package xyz.xenondevs.nova.annotations

import kotlinx.serialization.Serializable

@Serializable
data class AnnotationIndex(
    val classes: Map<String, Class> = emptyMap()
) {
    @Serializable
    data class Class(
        val annotations: List<Annotation> = emptyList(),
        val methods: List<Method> = emptyList()
    )
    
    @Serializable
    data class Method(
        val name: String,
        val descriptor: String,
        val annotations: List<Annotation>
    )
    
    @Serializable
    data class Annotation(
        val descriptor: String,
        val arguments: Map<String, AnnotationValue> = emptyMap()
    )
    
    companion object {
        const val FILE_NAME = "nova-annotations.json"
    }
}
