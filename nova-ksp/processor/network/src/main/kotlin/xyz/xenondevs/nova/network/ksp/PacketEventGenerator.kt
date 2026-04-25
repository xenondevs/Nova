@file:OptIn(KspExperimental::class)

package xyz.xenondevs.nova.network.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

internal class PacketEventGenerator(private val codeGenerator: CodeGenerator) {
    
    private val eventPackage = "xyz.xenondevs.nova.network.event"
    private val playerClass = ClassName("org.bukkit.entity", "Player")
    private val packetEventClass = ClassName(eventPackage, "PacketEvent")
    private val playerPacketEventClass = ClassName(eventPackage, "PlayerPacketEvent")
    private val packetEventManagerClass = ClassName(eventPackage, "PacketEventManager")
    
    private val protocolPackage = "net.minecraft.network.protocol"
    private val gamePackage = "net.minecraft.network.protocol.game"
    private val packetPackages = listOf(
        "common", "configuration", "cookie", "game", "handshake", "login", "ping", "status"
    ).map { "$protocolPackage.$it" }
    
    private lateinit var clientboundPacketListenerType: KSType
    
    fun generate(resolver: Resolver) {
        clientboundPacketListenerType = resolver.getClassDeclarationByName("net.minecraft.network.ClientboundPacketListener")!!.asStarProjectedType()
        
        val recordPackets = findRecordPacketClasses(resolver)
        if (recordPackets.isEmpty())
            return
        
        val clientbound = recordPackets.filter { it.isClientboundPacket() }
        val serverbound = recordPackets.filter { !it.isClientboundPacket() }
        
        if (clientbound.isNotEmpty())
            generateEventFile("GeneratedClientboundPacketEvents", "$eventPackage.clientbound", clientbound)
        if (serverbound.isNotEmpty())
            generateEventFile("GeneratedServerboundPacketEvents", "$eventPackage.serverbound", serverbound)
        
        generateRegistrationFunction(recordPackets)
    }
    
    private fun findRecordPacketClasses(resolver: Resolver): List<KSClassDeclaration> =
        packetPackages.flatMap { resolver.getDeclarationsFromPackage(it) }
            .filter { it.packageName.asString().startsWith("net.minecraft.network.protocol") }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.isJavaRecord() && it.isPacket() }
            .toList()
    
    private fun KSClassDeclaration.isJavaRecord(): Boolean =
        superTypes.any { it.resolve().declaration.qualifiedName?.asString() == "java.lang.Record" }
    
    private fun KSClassDeclaration.isPacket(): Boolean =
        superTypes.any { it.resolve().declaration.qualifiedName?.asString() == "net.minecraft.network.protocol.Packet" }
    
    private fun KSClassDeclaration.isGamePacket(): Boolean =
        isPacket() && packageName.asString() == gamePackage
    
    private fun KSClassDeclaration.isClientboundPacket(): Boolean {
        val packetSupertype = superTypes.firstOrNull { it.resolve().declaration.qualifiedName?.asString() == "net.minecraft.network.protocol.Packet" }?.resolve()
            ?: return false // not a packet
        val packetListenerType = packetSupertype.arguments.single().type?.resolve()
            ?: return false
        return clientboundPacketListenerType.isAssignableFrom(packetListenerType)
    }
    
    private fun generateEventFile(
        fileName: String,
        packageName: String,
        packets: List<KSClassDeclaration>
    ) {
        val fileBuilder = FileSpec.builder(packageName, fileName)
        
        for (packetClass in packets) {
            fileBuilder.addType(generateEventClass(packetClass))
        }
        
        fileBuilder.build().writeTo(codeGenerator, Dependencies(false))
    }
    
    private fun generateEventClass(packetClass: KSClassDeclaration): TypeSpec {
        val packetClassName = packetClass.toClassName()
        val eventName = packetClass.simpleName.asString() + "Event"
        val components = packetClass.primaryConstructor?.parameters ?: emptyList()
        val isGamePacket = packetClass.isGamePacket()
        
        val classBuilder = TypeSpec.classBuilder(eventName)
        
        if (isGamePacket) {
            classBuilder
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("player", playerClass)
                        .addParameter("packet", packetClassName)
                        .build()
                )
                .superclass(playerPacketEventClass.parameterizedBy(packetClassName))
                .addSuperclassConstructorParameter("player, packet")
        } else {
            classBuilder
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("packet", packetClassName)
                        .build()
                )
                .superclass(packetEventClass.parameterizedBy(packetClassName))
                .addSuperclassConstructorParameter("packet")
        }
        
        for (component in components) {
            val name = component.name!!.asString()
            val type = component.type.toTypeName().toImmutableCollections()
            
            classBuilder.addProperty(
                PropertySpec.builder(name, type)
                    .mutable(true)
                    .initializer("packet.$name")
                    .setter(
                        FunSpec.setterBuilder()
                            .addParameter("value", type)
                            .addStatement("field = value")
                            .addStatement("changed = true")
                            .build()
                    )
                    .build()
            )
        }
        
        if (components.isNotEmpty()) {
            classBuilder.addFunction(
                FunSpec.builder("buildChangedPacket")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(packetClassName)
                    .addStatement(
                        "return %T(%L)",
                        packetClassName,
                        components.joinToString(", ") { it.name!!.asString() }
                    )
                    .build()
            )
        }
        
        return classBuilder.build()
    }
    
    private fun generateRegistrationFunction(packets: List<KSClassDeclaration>) {
        val funBuilder = FunSpec.builder("registerGeneratedPacketEvents")
            .addModifiers(KModifier.INTERNAL)
            .receiver(packetEventManagerClass)
        
        for (packetClass in packets) {
            val eventName = packetClass.simpleName.asString() + "Event"
            val direction = if (packetClass.isClientboundPacket()) "clientbound" else "serverbound"
            val eventClassName = ClassName("$eventPackage.$direction", eventName)
            
            if (packetClass.isGamePacket()) {
                funBuilder.addStatement("registerPlayerEventType(::%T)", eventClassName)
            } else {
                funBuilder.addStatement("registerEventType(::%T)", eventClassName)
            }
        }
        
        FileSpec.builder(eventPackage, "GeneratedPacketEventRegistration")
            .addFunction(funBuilder.build())
            .build()
            .writeTo(codeGenerator, Dependencies(false))
    }
    
    private val mutableToImmutable = mapOf(
        ClassName("kotlin.collections", "MutableList") to ClassName("kotlin.collections", "List"),
        ClassName("kotlin.collections", "MutableSet") to ClassName("kotlin.collections", "Set"),
        ClassName("kotlin.collections", "MutableMap") to ClassName("kotlin.collections", "Map"),
        ClassName("kotlin.collections", "MutableCollection") to ClassName("kotlin.collections", "Collection"),
        ClassName("kotlin.collections", "MutableIterable") to ClassName("kotlin.collections", "Iterable"),
        ClassName("kotlin.collections", "MutableIterator") to ClassName("kotlin.collections", "Iterator"),
    )
    
    private fun TypeName.toImmutableCollections(): TypeName = when (this) {
        is ParameterizedTypeName -> {
            val mapped = mutableToImmutable[rawType] ?: rawType
            mapped.parameterizedBy(typeArguments.map { it.toImmutableCollections() }).copy(nullable = isNullable)
        }
        
        is WildcardTypeName -> this
        is ClassName -> mutableToImmutable[this]?.copy(nullable = isNullable) ?: this
        else -> this
    }
    
}
