package xyz.xenondevs.nova.packetentity.ksp

internal sealed interface DefaultValue {
    data class IntConst(val value: Int) : DefaultValue
    data class LongConst(val value: Long) : DefaultValue
    data class FloatConst(val value: Float) : DefaultValue
    data class DoubleConst(val value: Double) : DefaultValue
    data class StringConst(val value: String) : DefaultValue
    data class ByteConst(val value: Int) : DefaultValue
    data class BoolConst(val value: Boolean) : DefaultValue
    data class StaticField(val owner: String, val name: String) : DefaultValue
    data class StaticCall(val owner: String, val name: String, val args: List<DefaultValue>) : DefaultValue
    data class ConstructorCall(val type: String, val args: List<DefaultValue>) : DefaultValue
    data class InstanceCall(val receiver: DefaultValue, val method: String, val args: List<DefaultValue>) : DefaultValue
    data object NullValue : DefaultValue
    data object Unknown : DefaultValue
}

internal data class EntityDataField(
    val fieldName: String,
    val serializerType: String,
    val index: Int,
    val defaultValue: DefaultValue
)

internal data class EntityClassData(
    val className: String,
    val superClassName: String?,
    val fields: List<EntityDataField>
)

internal data class AnalysisResult(
    val entityData: Map<String, EntityClassData>,
    val superclasses: Map<String, String>,
    val entityTypes: Map<String, String>
)
