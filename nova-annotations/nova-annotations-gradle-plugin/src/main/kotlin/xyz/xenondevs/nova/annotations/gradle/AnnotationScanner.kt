package xyz.xenondevs.nova.annotations.gradle

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import xyz.xenondevs.nova.annotations.AnnotationIndex
import xyz.xenondevs.nova.annotations.AnnotationValue

internal class AnnotationScanner(classAnnotations: Set<String>, methodAnnotations: Set<String>) {
    
    private val classDescriptors = classAnnotations.mapTo(HashSet()) { "L${it.replace('.', '/')};" }
    private val methodDescriptors = methodAnnotations.mapTo(HashSet()) { "L${it.replace('.', '/')};" }
    
    fun scan(bytes: ByteArray): Pair<String, AnnotationIndex.Class> {
        val reader = ClassReader(bytes)
        val visitor = IndexClassVisitor()
        reader.accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        return reader.className to AnnotationIndex.Class(
            visitor.annotations.sortedBy { it.descriptor },
            visitor.methods.sortedWith(compareBy({ it.name }, { it.descriptor }))
        )
    }
    
    private inner class IndexClassVisitor : ClassVisitor(Opcodes.ASM9) {
        
        val annotations = ArrayList<AnnotationIndex.Annotation>()
        val methods = ArrayList<AnnotationIndex.Method>()
        
        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            if (descriptor !in classDescriptors)
                return null
            return IndexAnnotationVisitor(descriptor).also { annotations += it.annotation }
        }
        
        override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            return if (methodDescriptors.isEmpty()) null else IndexMethodVisitor(name, descriptor)
        }
        
        private inner class IndexMethodVisitor(
            private val name: String,
            private val descriptor: String
        ) : MethodVisitor(Opcodes.ASM9) {
            
            private val annotations = ArrayList<AnnotationIndex.Annotation>()
            
            override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                if (descriptor !in methodDescriptors)
                    return null
                return IndexAnnotationVisitor(descriptor).also { annotations += it.annotation }
            }
            
            override fun visitEnd() {
                if (annotations.isNotEmpty())
                    methods += AnnotationIndex.Method(name, descriptor, annotations.sortedBy { it.descriptor })
            }
            
        }
        
    }
    
}

private class IndexAnnotationVisitor(descriptor: String) : ValueVisitor() {
    
    private val arguments = sortedMapOf<String, AnnotationValue>()
    val annotation = AnnotationIndex.Annotation(descriptor, arguments)
    
    override fun addValue(name: String?, value: AnnotationValue) {
        arguments[requireNotNull(name)] = value
    }
    
}

private class ArrayVisitor : ValueVisitor() {
    
    val values = ArrayList<AnnotationValue>()
    
    override fun addValue(name: String?, value: AnnotationValue) {
        values += value
    }
    
}

private abstract class ValueVisitor : AnnotationVisitor(Opcodes.ASM9) {
    
    protected abstract fun addValue(name: String?, value: AnnotationValue)
    
    override fun visit(name: String?, value: Any) {
        addValue(name, toAnnotationValue(value))
    }
    
    override fun visitEnum(name: String?, descriptor: String, value: String) {
        addValue(name, AnnotationValue.EnumConstant(descriptor, value))
    }
    
    override fun visitAnnotation(name: String?, descriptor: String): AnnotationVisitor {
        val visitor = IndexAnnotationVisitor(descriptor)
        addValue(name, AnnotationValue.Nested(visitor.annotation))
        return visitor
    }
    
    override fun visitArray(name: String?): AnnotationVisitor {
        val visitor = ArrayVisitor()
        addValue(name, AnnotationValue.ArrayValue(visitor.values))
        return visitor
    }
    
}

private fun toAnnotationValue(value: Any): AnnotationValue = when (value) {
    is Type -> AnnotationValue.ClassLiteral(value.descriptor)
    is Boolean -> AnnotationValue.Boolean(value)
    is Byte -> AnnotationValue.Byte(value)
    is Short -> AnnotationValue.Short(value)
    is Int -> AnnotationValue.Int(value)
    is Long -> AnnotationValue.Long(value)
    is Float -> AnnotationValue.Float(value)
    is Double -> AnnotationValue.Double(value)
    is String -> AnnotationValue.String(value)
    is Char -> AnnotationValue.Char(value)
    is BooleanArray -> AnnotationValue.ArrayValue(value.map(::toAnnotationValue))
    is ByteArray -> AnnotationValue.ArrayValue(value.map(::toAnnotationValue))
    is ShortArray -> AnnotationValue.ArrayValue(value.map(::toAnnotationValue))
    is IntArray -> AnnotationValue.ArrayValue(value.map(::toAnnotationValue))
    is LongArray -> AnnotationValue.ArrayValue(value.map(::toAnnotationValue))
    is FloatArray -> AnnotationValue.ArrayValue(value.map(::toAnnotationValue))
    is DoubleArray -> AnnotationValue.ArrayValue(value.map(::toAnnotationValue))
    is CharArray -> AnnotationValue.ArrayValue(value.map(::toAnnotationValue))
    else -> error("Unsupported annotation value: ${value.javaClass.name}")
}
