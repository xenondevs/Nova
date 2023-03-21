package xyz.xenondevs.nova.util.data

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import xyz.xenondevs.bytebase.util.toMap
import java.io.File
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import kotlin.reflect.KClass

object JarUtils {
    
    fun <A : Annotation> searchForAnnotatedClasses(file: File, annotationClass: KClass<A>): Map<String, Map<String, Any?>> {
        val result = Object2ObjectOpenHashMap<String, Map<String, Any?>>()
        val classDesc = Type.getDescriptor(annotationClass.java)
        
        loopClasses(file, filter = { it.name.endsWith(".class") }) { _, ins ->
            val clazz = ClassNode().apply { ClassReader(ins).accept(this, ClassReader.SKIP_CODE) }
            val annotation = clazz.visibleAnnotations?.firstOrNull { it.desc == classDesc } ?: return@loopClasses
            result[clazz.name] = annotation.toMap()
        }
        
        return result
    }
    
    fun loopClasses(file: File, filter: (JarEntry) -> Boolean = { true }, action: (JarEntry, InputStream) -> Unit) {
        JarInputStream(file.inputStream()).use { jis ->
            generateSequence(jis::getNextJarEntry).filter(filter).forEach { entry ->
                action(entry, jis)
            }
        }
    }
    
}