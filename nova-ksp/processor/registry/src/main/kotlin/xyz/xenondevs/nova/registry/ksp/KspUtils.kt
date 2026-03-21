@file:OptIn(KspExperimental::class)

package xyz.xenondevs.nova.registry.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier

internal fun KSClassDeclaration.publicStaticPropertiesOfType(typeName: String): Sequence<KSPropertyDeclaration> =
    getDeclaredProperties()
        .filter { Modifier.PUBLIC in it.modifiers && Modifier.JAVA_STATIC in it.modifiers }
        .filter { (it.type.element as? KSClassifierReference)?.referencedName() == typeName }

internal fun KSPropertyDeclaration.primaryTypeArgument(): KSType =
    type.element!!.typeArguments.first().type!!.resolve()

internal fun KSClassDeclaration.nonDeprecatedRegistryKeyProperties(): Sequence<KSPropertyDeclaration> =
    publicStaticPropertiesOfType("RegistryKey")
        .filter { property ->
            // Excludes registry keys whose type's getKey method is deprecated (these can exist without a key).
            val keyTypeDecl = property.primaryTypeArgument().declaration as KSClassDeclaration
            val getKeyMethod = keyTypeDecl.getAllFunctions().first { it.simpleName.asString() == "getKey" }
            !getKeyMethod.isAnnotationPresent(Deprecated::class)
        }
