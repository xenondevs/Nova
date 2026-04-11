package xyz.xenondevs.nova.network.ksp

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

internal class PacketIdsGenerator(private val codeGenerator: CodeGenerator) {
    
    private val getPacketId = MemberName("xyz.xenondevs.nova.network", "getPacketId")
    
    private val packetTypeSourceClasses = listOf(
        "net.minecraft.network.protocol.game.GamePacketTypes",
        "net.minecraft.network.protocol.common.CommonPacketTypes",
        "net.minecraft.network.protocol.cookie.CookiePacketTypes",
        "net.minecraft.network.protocol.ping.PingPacketTypes",
    )
    
    fun generate(resolver: Resolver) {
        val sourceClasses = packetTypeSourceClasses.mapNotNull { resolver.getClassDeclarationByName(resolver.getKSNameFromString(it)) }
        if (sourceClasses.isEmpty())
            return
        
        val objectBuilder = TypeSpec.objectBuilder("PacketIds")
            .addKdoc("Contains the packet IDs of all packets in the protocol.")
        
        for (sourceClass in sourceClasses) {
            val sourceClassName = ClassName(sourceClass.packageName.asString(), sourceClass.simpleName.asString())
            for (property in sourceClass.packetTypeProperties()) {
                val fieldName = property.simpleName.asString()
                val prop = PropertySpec.builder("PLAY_$fieldName", Int::class)
                    .initializer("%M(%T.$fieldName)", getPacketId, sourceClassName)
                    .addAnnotation(JvmField::class)
                    .build()
                objectBuilder.addProperty(prop)
            }
        }
        
        val fileSpec = FileSpec.builder("xyz.xenondevs.nova.network.packet", "PacketIds")
            .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "unused").build())
            .addType(objectBuilder.build())
            .build()
        
        fileSpec.writeTo(codeGenerator, Dependencies(false))
    }
    
    private fun KSClassDeclaration.packetTypeProperties(): Sequence<KSPropertyDeclaration> =
        getDeclaredProperties()
            .filter { Modifier.PUBLIC in it.modifiers && Modifier.JAVA_STATIC in it.modifiers }
            .filter { (it.type.element as? KSClassifierReference)?.referencedName() == "PacketType" }
    
}
