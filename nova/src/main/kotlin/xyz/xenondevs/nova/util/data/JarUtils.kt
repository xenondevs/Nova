package xyz.xenondevs.nova.util.data

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import xyz.xenondevs.bytebase.util.toMap
import java.io.InputStream
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import kotlin.io.path.inputStream
import kotlin.reflect.KClass

internal data class AnnotationSearchResult(
    val classes: Map<KClass<out Annotation>, Map<String, List<Map<String, Any?>>>>,
    val functions: Map<KClass<out Annotation>, Map<String, Map<String, List<Map<String, Any?>>>>>
)

internal object JarUtils {
    
    // TODO: find annotated classes during build and write them to a file 
    
    fun findAnnotatedClasses(
        file: Path,
        classAnnotations: List<KClass<out Annotation>>,
        functionAnnotations: List<KClass<out Annotation>>,
        path: String = ""
    ): AnnotationSearchResult {
        val classes = HashMap<KClass<out Annotation>, MutableMap<String, MutableList<Map<String, Any?>>>>()
        val functions = HashMap<KClass<out Annotation>, MutableMap<String, MutableMap<String, MutableList<Map<String, Any?>>>>>()
        val classAnnotationDescriptors = classAnnotations.map { Type.getDescriptor(it.java) }
        val functionAnnotationDescriptors = functionAnnotations.map { Type.getDescriptor(it.java) }
        
        loopClasses(file, filter = { it.name.endsWith(".class") && it.name.startsWith(path) }) { _, ins ->
            val classNode = ClassNode().apply { ClassReader(ins).accept(this, ClassReader.SKIP_CODE) }
            
            classNode.visibleAnnotations?.forEach { annotation ->
                val i = classAnnotationDescriptors.indexOf(annotation.desc)
                if (i == -1)
                    return@forEach
                
                classes
                    .getOrPut(classAnnotations[i], ::HashMap)
                    .getOrPut(classNode.name, ::ArrayList)
                    .add(annotation.toMap())
            }
            
            for (method in classNode.methods) {
                method.visibleAnnotations?.forEach { annotation ->
                    val i = functionAnnotationDescriptors.indexOf(annotation.desc)
                    if (i == -1)
                        return@forEach
                    
                    functions
                        .getOrPut(functionAnnotations[i], ::HashMap)
                        .getOrPut(classNode.name, ::HashMap)
                        .getOrPut(method.name, ::ArrayList)
                        .add(annotation.toMap())
                }
            }
        }
        
        return AnnotationSearchResult(classes, functions)
    }
    
    private fun loopClasses(file: Path, filter: (JarEntry) -> Boolean = { true }, action: (JarEntry, InputStream) -> Unit) {
        JarInputStream(file.inputStream()).use { jis ->
            generateSequence(jis::getNextJarEntry)
                .filter(filter)
                .forEach { entry -> action(entry, jis) }
        }
    }
    
}