package xyz.xenondevs.nova.util.data

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import xyz.xenondevs.nova.annotations.AnnotationIndex
import xyz.xenondevs.nova.annotations.AnnotationValue
import java.io.InputStream
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import kotlin.io.path.exists
import kotlin.io.path.inputStream

internal object JarUtils {
    
    private val indexes = ConcurrentHashMap<Path, AnnotationIndex>()
    
    fun getAnnotationIndex(file: Path): AnnotationIndex =
        indexes.computeIfAbsent(file.toAbsolutePath().normalize()) { path ->
            path.useZip { root ->
                val index = root.resolve(AnnotationIndex.FILE_NAME)
                if (index.exists())
                    return@computeIfAbsent index.readJson<AnnotationIndex>()
            }
            
            scanAnnotationIndex(path)
        }
    
    // Compatibility with addons built before annotation indexes were introduced. TODO: Remove in 0.26
    private fun scanAnnotationIndex(file: Path): AnnotationIndex {
        val classes = HashMap<String, AnnotationIndex.Class>()
        loopClasses(file, filter = { it.name.endsWith(".class") }) { _, stream ->
            val node = ClassNode().apply {
                ClassReader(stream).accept(this, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
            }
            val annotations = (node.visibleAnnotations.orEmpty() + node.invisibleAnnotations.orEmpty()).map { it.toIndexAnnotation() }
            val methods = node.methods.mapNotNull { method ->
                val methodAnnotations = (method.visibleAnnotations.orEmpty() + method.invisibleAnnotations.orEmpty()).map { it.toIndexAnnotation() }
                if (methodAnnotations.isEmpty()) null
                else AnnotationIndex.Method(method.name, method.desc, methodAnnotations)
            }
            if (annotations.isNotEmpty() || methods.isNotEmpty())
                classes[node.name] = AnnotationIndex.Class(annotations, methods)
        }
        return AnnotationIndex(classes)
    }
    
    private fun AnnotationNode.toIndexAnnotation(): AnnotationIndex.Annotation =
        AnnotationIndex.Annotation(desc, values.orEmpty().chunked(2).associate { [name, value] ->
            name as String to value.toAnnotationValue()
        })
    
    private fun Any.toAnnotationValue(): AnnotationValue = when (this) {
        is String -> AnnotationValue.String(this)
        is Boolean -> AnnotationValue.Boolean(this)
        is Byte -> AnnotationValue.Byte(this)
        is Short -> AnnotationValue.Short(this)
        is Int -> AnnotationValue.Int(this)
        is Long -> AnnotationValue.Long(this)
        is Float -> AnnotationValue.Float(this)
        is Double -> AnnotationValue.Double(this)
        is Char -> AnnotationValue.Char(this)
        is Type -> AnnotationValue.ClassLiteral(descriptor)
        is Array<*> -> AnnotationValue.EnumConstant(this[0] as String, this[1] as String)
        is AnnotationNode -> AnnotationValue.Nested(toIndexAnnotation())
        is List<*> -> AnnotationValue.ArrayValue(map { requireNotNull(it).toAnnotationValue() })
        else -> error("Unsupported annotation value: ${javaClass.name}")
    }
    
    private fun loopClasses(file: Path, filter: (JarEntry) -> Boolean = { true }, action: (JarEntry, InputStream) -> Unit) {
        JarInputStream(file.inputStream()).use { jis ->
            generateSequence(jis::getNextJarEntry)
                .filter(filter)
                .forEach { entry -> action(entry, jis) }
        }
    }
    
}
