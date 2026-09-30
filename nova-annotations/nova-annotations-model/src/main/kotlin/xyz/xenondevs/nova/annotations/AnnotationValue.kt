package xyz.xenondevs.nova.annotations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface AnnotationValue {
    
    @Serializable
    @SerialName("string")
    data class String(val value: kotlin.String) : AnnotationValue
    
    @Serializable
    @SerialName("boolean")
    data class Boolean(val value: kotlin.Boolean) : AnnotationValue
    
    @Serializable
    @SerialName("byte")
    data class Byte(val value: kotlin.Byte) : AnnotationValue
    
    @Serializable
    @SerialName("short")
    data class Short(val value: kotlin.Short) : AnnotationValue
    
    @Serializable
    @SerialName("int")
    data class Int(val value: kotlin.Int) : AnnotationValue
    
    @Serializable
    @SerialName("long")
    data class Long(val value: kotlin.Long) : AnnotationValue
    
    @Serializable
    @SerialName("float")
    data class Float(val value: kotlin.Float) : AnnotationValue
    
    @Serializable
    @SerialName("double")
    data class Double(val value: kotlin.Double) : AnnotationValue
    
    @Serializable
    @SerialName("char")
    data class Char(val value: kotlin.Char) : AnnotationValue
    
    @Serializable
    @SerialName("class")
    data class ClassLiteral(val descriptor: kotlin.String) : AnnotationValue
    
    @Serializable
    @SerialName("enum")
    data class EnumConstant(val descriptor: kotlin.String, val name: kotlin.String) : AnnotationValue
    
    @Serializable
    @SerialName("annotation")
    data class Nested(val annotation: AnnotationIndex.Annotation) : AnnotationValue
    
    @Serializable
    @SerialName("array")
    data class ArrayValue(val values: List<AnnotationValue>) : AnnotationValue
    
}
