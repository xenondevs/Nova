package xyz.xenondevs.nova.packetentity.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeAliasSpec
import com.squareup.kotlinpoet.ksp.writeTo

private const val GENERATED_PACKAGE = "xyz.xenondevs.nova.packetentity"

private val ENTITY_TYPES = ClassName("net.minecraft.world.entity", "EntityTypes")
private val PACKET_ENTITY = ClassName(GENERATED_PACKAGE, "PacketEntity")
private val PACKET_ENTITY_DSL = ClassName(GENERATED_PACKAGE, "PacketEntityDsl")
private val PASSENGER_PACKET_ENTITY_DSL = ClassName(GENERATED_PACKAGE, "PassengerPacketEntityDsl")
private val PACKET_ENTITY_PASSENGERS_DSL = ClassName(GENERATED_PACKAGE, "PacketEntityPassengersDsl")
private val PACKET_ENTITY_DSL_MARKER = ClassName(GENERATED_PACKAGE, "PacketEntityDslMarker")
private val PACKET_ENTITY_DSL_IMPL = ClassName(GENERATED_PACKAGE, "PacketEntityDslImpl")
private val PASSENGER_PACKET_ENTITY_DSL_IMPL = ClassName(GENERATED_PACKAGE, "PassengerPacketEntityDslImpl")
private val PACKET_ENTITY_STATE = ClassName(GENERATED_PACKAGE, "PacketEntityState")
private val PACKET_ENTITY_ROOT_STATE = ClassName(GENERATED_PACKAGE, "PacketEntityRootState")
private val PACKET_ENTITY_IMPL = ClassName(GENERATED_PACKAGE, "PacketEntityImpl")
private val PACKET_ENTITY_PASSENGER_DATA = ClassName(GENERATED_PACKAGE, "PacketEntityPassengerData")
private val PACKET_ENTITY_PASSENGERS_DSL_IMPL = ClassName(GENERATED_PACKAGE, "PacketEntityPassengersDslImpl")
private val UNIT = ClassName("kotlin", "Unit")

internal class PacketEntityDslGenerator(private val codeGenerator: CodeGenerator) {
    
    fun generate(analysisResult: AnalysisResult) {
        val fileSpec = FileSpec.builder(GENERATED_PACKAGE, "PacketEntityDslFunctions")
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S, %S", "unused", "UNCHECKED_CAST")
                    .build()
            )
        
        for ([_, classData] in analysisResult.entityData) {
            val entityTypeFieldName = analysisResult.entityTypes[classData.className] ?: continue
            fileSpec.addTypeAlias(buildPacketEntityTypeAlias(classData))
            fileSpec.addFunction(buildFunction(classData, entityTypeFieldName))
        }
        
        fileSpec.addType(buildPacketEntityPassengersDsl(analysisResult))
        
        fileSpec.build().writeTo(codeGenerator, Dependencies(false))
    }
    
    private fun buildFunction(classData: EntityClassData, entityTypeFieldName: String): FunSpec {
        val functionName = toFunctionName(classData.className)
        val packetEntityType = ClassName(GENERATED_PACKAGE, toPacketEntityName(classData.className))
        val metadataInterface = ClassName(GENERATED_PACKAGE, toMetadataDslName(classData.className))
        val metadataImpl = ClassName(GENERATED_PACKAGE, "${toMetadataDslName(classData.className)}Impl")
        val metadataState = ClassName(GENERATED_PACKAGE, toMetadataStateName(classData.className))
        val metadataInterfaceView = ClassName(GENERATED_PACKAGE, toMetadataName(classData.className))
        val metadataViewImpl = ClassName(GENERATED_PACKAGE, "${toMetadataName(classData.className)}Impl")
        val dslType = PACKET_ENTITY_DSL.parameterizedBy(metadataInterface)
        val implType = PACKET_ENTITY_DSL_IMPL.parameterizedBy(metadataInterface)
        
        return FunSpec.builder(functionName)
            .addParameter(functionName, LambdaTypeName.get(receiver = dslType, returnType = UNIT))
            .returns(packetEntityType)
            .addStatement("val metadataState = %T()", metadataState)
            .addStatement("val packetEntityState = %T(metadataState)", PACKET_ENTITY_ROOT_STATE.parameterizedBy(metadataState))
            .addStatement("val packetEntityDsl = %T(packetEntityState, %T(metadataState))", implType, metadataImpl)
            .addStatement("(packetEntityDsl as %T).%L()", dslType, functionName)
            .addStatement("return %T(%T.%L, packetEntityState, %T(metadataState))", PACKET_ENTITY_IMPL.parameterizedBy(metadataInterfaceView), ENTITY_TYPES, entityTypeFieldName, metadataViewImpl)
            .build()
    }
    
    private fun buildPacketEntityTypeAlias(classData: EntityClassData): TypeAliasSpec {
        val metadataInterface = ClassName(GENERATED_PACKAGE, toMetadataName(classData.className))
        return TypeAliasSpec.builder(
            toPacketEntityName(classData.className),
            PACKET_ENTITY.parameterizedBy(metadataInterface)
        ).build()
    }
    
    private fun buildPassengerFunction(classData: EntityClassData, entityTypeFieldName: String): FunSpec {
        val functionName = toPassengerFunctionName(classData.className)
        val metadataInterface = ClassName(GENERATED_PACKAGE, toMetadataDslName(classData.className))
        val metadataImpl = ClassName(GENERATED_PACKAGE, "${toMetadataDslName(classData.className)}Impl")
        val metadataState = ClassName(GENERATED_PACKAGE, toMetadataStateName(classData.className))
        val metadataInterfaceView = ClassName(GENERATED_PACKAGE, toMetadataName(classData.className))
        val metadataViewImpl = ClassName(GENERATED_PACKAGE, "${toMetadataName(classData.className)}Impl")
        val dslType = PASSENGER_PACKET_ENTITY_DSL.parameterizedBy(metadataInterface)
        val implType = PASSENGER_PACKET_ENTITY_DSL_IMPL.parameterizedBy(metadataInterface)
        
        return FunSpec.builder(functionName)
            .addParameter(functionName, LambdaTypeName.get(receiver = dslType, returnType = UNIT))
            .addStatement("val metadataState = %T()", metadataState)
            .addStatement("val packetEntityState = %T(metadataState)", PACKET_ENTITY_STATE.parameterizedBy(metadataState))
            .addStatement("val passengersDsl = this as %T", PACKET_ENTITY_PASSENGERS_DSL_IMPL)
            .addStatement("val packetEntityDsl = %T(packetEntityState, %T(metadataState))", implType, metadataImpl)
            .addStatement("(packetEntityDsl as %T).%L()", dslType, functionName)
            .addStatement("passengersDsl.add(%T(%T.%L, packetEntityState, %T(metadataState)))", PACKET_ENTITY_PASSENGER_DATA.parameterizedBy(metadataInterfaceView), ENTITY_TYPES, entityTypeFieldName, metadataViewImpl)
            .build()
    }
    
    private fun buildPacketEntityPassengersDsl(analysisResult: AnalysisResult): TypeSpec {
        val builder = TypeSpec.interfaceBuilder("PacketEntityPassengersDsl")
            .addModifiers(KModifier.SEALED)
            .addAnnotation(PACKET_ENTITY_DSL_MARKER)
        
        for ([_, classData] in analysisResult.entityData) {
            val entityTypeFieldName = analysisResult.entityTypes[classData.className] ?: continue
            builder.addFunction(buildPassengerFunction(classData, entityTypeFieldName))
        }
        
        return builder.build()
    }
}

private fun toMetadataDslName(internalClassName: String): String {
    val simpleName = internalClassName.substringAfterLast('/').substringAfterLast('$')
    return "${simpleName}MetadataDsl"
}

private fun toMetadataName(internalClassName: String): String {
    val simpleName = internalClassName.substringAfterLast('/').substringAfterLast('$')
    return "${simpleName}Metadata"
}

private fun toMetadataStateName(internalClassName: String): String {
    val simpleName = internalClassName.substringAfterLast('/').substringAfterLast('$')
    return "${simpleName}MetadataState"
}

private fun toPacketEntityName(internalClassName: String): String {
    val simpleName = internalClassName.substringAfterLast('/').substringAfterLast('$')
    return "Packet$simpleName"
}

private fun toFunctionName(internalClassName: String): String =
    "packet${internalClassName.substringAfterLast('/').substringAfterLast('$')}"

private fun toPassengerFunctionName(internalClassName: String): String =
    internalClassName.substringAfterLast('/').substringAfterLast('$')
        .replaceFirstChar { it.lowercase() }
