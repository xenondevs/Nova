package xyz.xenondevs.nova.packetentity.ksp

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.io.File
import java.io.InputStream
import java.util.jar.JarFile

private const val SYNCHED_ENTITY_DATA = "net/minecraft/network/syncher/SynchedEntityData"
private const val SYNCHED_ENTITY_DATA_BUILDER = "net/minecraft/network/syncher/SynchedEntityData\$Builder"
private const val ENTITY_DATA_SERIALIZERS = "net/minecraft/network/syncher/EntityDataSerializers"
private const val ENTITY_DATA_ACCESSOR_DESC = "Lnet/minecraft/network/syncher/EntityDataAccessor;"
private const val ENTITY_TYPE = "net/minecraft/world/entity/EntityType"
private const val ENTITY_TYPES = "net/minecraft/world/entity/EntityTypes"

private val BOXING_TYPES = setOf(
    "java/lang/Byte", "java/lang/Integer", "java/lang/Short",
    "java/lang/Long", "java/lang/Float", "java/lang/Double",
    "java/lang/Boolean", "java/lang/Character"
)

internal class EntityDataGenerator {

    fun analyze(classpathEntries: List<File>): AnalysisResult {
        val superclasses = mutableMapOf<String, String>()
        val defineIdCalls = linkedMapOf<String, MutableList<RawDefineIdCall>>()
        val defaults = mutableMapOf<String, DefaultValue>()
        val entityTypes = mutableMapOf<String, String>()

        for (entry in classpathEntries) {
            when {
                entry.isDirectory -> scanDirectory(entry, superclasses, defineIdCalls, defaults, entityTypes)
                entry.extension == "jar" -> scanJar(entry, superclasses, defineIdCalls, defaults, entityTypes)
            }
        }

        return AnalysisResult(computeIndices(superclasses, defineIdCalls, defaults), superclasses, entityTypes)
    }

    private fun scanJar(
        jarFile: File,
        superclasses: MutableMap<String, String>,
        defineIdCalls: MutableMap<String, MutableList<RawDefineIdCall>>,
        defaults: MutableMap<String, DefaultValue>,
        entityTypes: MutableMap<String, String>
    ) {
        JarFile(jarFile).use { jar ->
            for (entry in jar.entries()) {
                if (!entry.name.endsWith(".class"))
                    continue
                jar.getInputStream(entry).use { scanClass(it, superclasses, defineIdCalls, defaults, entityTypes) }
            }
        }
    }

    private fun scanDirectory(
        dir: File,
        superclasses: MutableMap<String, String>,
        defineIdCalls: MutableMap<String, MutableList<RawDefineIdCall>>,
        defaults: MutableMap<String, DefaultValue>,
        entityTypes: MutableMap<String, String>
    ) {
        dir.walkTopDown()
            .filter { it.extension == "class" }
            .forEach { file ->
                file.inputStream().use { scanClass(it, superclasses, defineIdCalls, defaults, entityTypes) }
            }
    }

    private fun scanClass(
        input: InputStream,
        superclasses: MutableMap<String, String>,
        defineIdCalls: MutableMap<String, MutableList<RawDefineIdCall>>,
        defaults: MutableMap<String, DefaultValue>,
        entityTypes: MutableMap<String, String>
    ) {
        val reader = ClassReader(input)
        val visitor = DefineIdClassVisitor(superclasses, defineIdCalls, defaults, entityTypes)
        reader.accept(visitor, ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)
    }

    private fun computeIndices(
        superclasses: Map<String, String>,
        defineIdCalls: Map<String, List<RawDefineIdCall>>,
        defaults: Map<String, DefaultValue>
    ): Map<String, EntityClassData> {
        val ancestorCountCache = mutableMapOf<String, Int>()

        fun ancestorFieldCount(className: String): Int {
            ancestorCountCache[className]?.let { return it }
            val parent = superclasses[className]
            if (parent == null || parent == "java/lang/Object") {
                return 0.also { ancestorCountCache[className] = it }
            }
            val count = ancestorFieldCount(parent) + (defineIdCalls[parent]?.size ?: 0)
            ancestorCountCache[className] = count
            return count
        }

        val result = linkedMapOf<String, EntityClassData>()

        for ([className, calls] in defineIdCalls) {
            val startIndex = ancestorFieldCount(className)
            val fields = calls.mapIndexed { i, call ->
                val defaultValue = defaults["${call.targetClass}/${call.fieldName}"] ?: DefaultValue.Unknown
                EntityDataField(call.fieldName, call.serializerType, startIndex + i, defaultValue)
            }
            result[className] = EntityClassData(
                className = className,
                superClassName = superclasses[className],
                fields = fields
            )
        }

        return result
    }
}

private data class RawDefineIdCall(
    val fieldName: String,
    val serializerType: String,
    val targetClass: String
)

private data class PendingDefineId(
    val targetClass: String,
    val serializerType: String
)

//<editor-fold desc="ASM visitors" defaultstate="collapsed">

private class DefineIdClassVisitor(
    private val superclasses: MutableMap<String, String>,
    private val defineIdCalls: MutableMap<String, MutableList<RawDefineIdCall>>,
    private val defaults: MutableMap<String, DefaultValue>,
    private val entityTypes: MutableMap<String, String>
) : ClassVisitor(Opcodes.ASM9) {

    private var className: String? = null

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        className = name
        if (superName != null) {
            superclasses[name] = superName
        }
    }

    override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor? {
        if (className == ENTITY_TYPES && descriptor == "L$ENTITY_TYPE;" && signature != null) {
            val entityClass = Regex("L$ENTITY_TYPE<L([^;]+);>;").matchEntire(signature)?.groupValues?.get(1)
            if (entityClass != null) {
                entityTypes[entityClass] = name
            }
        }

        return null
    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        return if (name == "<clinit>") DefineIdMethodVisitor(defineIdCalls) else DefaultValueMethodVisitor(defaults)
    }
}

private class DefineIdMethodVisitor(
    private val defineIdCalls: MutableMap<String, MutableList<RawDefineIdCall>>
) : MethodVisitor(Opcodes.ASM9) {

    private var lastClassType: String? = null
    private var lastSerializer: String? = null
    private var pendingCall: PendingDefineId? = null

    override fun visitLdcInsn(value: Any?) {
        if (value is Type && value.sort == Type.OBJECT) {
            lastClassType = value.internalName
        }
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        when (opcode) {
            Opcodes.GETSTATIC -> {
                if (owner == ENTITY_DATA_SERIALIZERS) {
                    lastSerializer = name
                }
            }
            Opcodes.PUTSTATIC -> {
                val pending = pendingCall
                if (pending != null && descriptor == ENTITY_DATA_ACCESSOR_DESC) {
                    defineIdCalls
                        .getOrPut(pending.targetClass) { mutableListOf() }
                        .add(RawDefineIdCall(name, pending.serializerType, pending.targetClass))
                    pendingCall = null
                }
            }
        }
    }

    override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
        if (opcode == Opcodes.INVOKESTATIC && owner == SYNCHED_ENTITY_DATA && name == "defineId") {
            val classType = lastClassType
            val serializer = lastSerializer
            if (classType != null && serializer != null) {
                pendingCall = PendingDefineId(classType, serializer)
            }
            lastClassType = null
            lastSerializer = null
        }
    }
}

private class DefaultValueMethodVisitor(
    private val defaults: MutableMap<String, DefaultValue>
) : MethodVisitor(Opcodes.ASM9) {

    private val stack = mutableListOf<DefaultValue>()
    private var lastAccessorOwner: String? = null
    private var lastAccessorName: String? = null

    private fun push(value: DefaultValue) = stack.add(value)

    private fun pop(): DefaultValue =
        if (stack.isNotEmpty()) stack.removeLast() else DefaultValue.Unknown

    private fun popN(n: Int): List<DefaultValue> =
        (0 until n).map { pop() }.reversed()

    override fun visitInsn(opcode: Int) {
        when (opcode) {
            Opcodes.ICONST_M1 -> push(DefaultValue.IntConst(-1))
            Opcodes.ICONST_0 -> push(DefaultValue.IntConst(0))
            Opcodes.ICONST_1 -> push(DefaultValue.IntConst(1))
            Opcodes.ICONST_2 -> push(DefaultValue.IntConst(2))
            Opcodes.ICONST_3 -> push(DefaultValue.IntConst(3))
            Opcodes.ICONST_4 -> push(DefaultValue.IntConst(4))
            Opcodes.ICONST_5 -> push(DefaultValue.IntConst(5))
            Opcodes.FCONST_0 -> push(DefaultValue.FloatConst(0.0f))
            Opcodes.FCONST_1 -> push(DefaultValue.FloatConst(1.0f))
            Opcodes.FCONST_2 -> push(DefaultValue.FloatConst(2.0f))
            Opcodes.DCONST_0 -> push(DefaultValue.DoubleConst(0.0))
            Opcodes.DCONST_1 -> push(DefaultValue.DoubleConst(1.0))
            Opcodes.LCONST_0 -> push(DefaultValue.LongConst(0L))
            Opcodes.LCONST_1 -> push(DefaultValue.LongConst(1L))
            Opcodes.ACONST_NULL -> push(DefaultValue.NullValue)
            Opcodes.I2B -> {
                val v = pop()
                push(if (v is DefaultValue.IntConst) DefaultValue.ByteConst(v.value) else DefaultValue.Unknown)
            }
            Opcodes.I2L -> {
                val v = pop()
                push(if (v is DefaultValue.IntConst) DefaultValue.LongConst(v.value.toLong()) else DefaultValue.Unknown)
            }
            Opcodes.I2F -> {
                val v = pop()
                push(if (v is DefaultValue.IntConst) DefaultValue.FloatConst(v.value.toFloat()) else DefaultValue.Unknown)
            }
            Opcodes.I2D -> {
                val v = pop()
                push(if (v is DefaultValue.IntConst) DefaultValue.DoubleConst(v.value.toDouble()) else DefaultValue.Unknown)
            }
            Opcodes.L2F -> {
                val v = pop()
                push(if (v is DefaultValue.LongConst) DefaultValue.FloatConst(v.value.toFloat()) else DefaultValue.Unknown)
            }
            Opcodes.F2D -> {
                val v = pop()
                push(if (v is DefaultValue.FloatConst) DefaultValue.DoubleConst(v.value.toDouble()) else DefaultValue.Unknown)
            }
            Opcodes.DUP -> {
                val v = pop()
                push(v)
                push(v)
            }
            else -> push(DefaultValue.Unknown)
        }
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        when (opcode) {
            Opcodes.BIPUSH, Opcodes.SIPUSH -> push(DefaultValue.IntConst(operand))
            else -> push(DefaultValue.Unknown)
        }
    }

    override fun visitLdcInsn(value: Any?) {
        when (value) {
            is Int -> push(DefaultValue.IntConst(value))
            is Float -> push(DefaultValue.FloatConst(value))
            is Long -> push(DefaultValue.LongConst(value))
            is Double -> push(DefaultValue.DoubleConst(value))
            is String -> push(DefaultValue.StringConst(value))
            else -> push(DefaultValue.Unknown)
        }
    }

    override fun visitVarInsn(opcode: Int, varIndex: Int) {
        when (opcode) {
            Opcodes.ALOAD, Opcodes.ILOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.LLOAD ->
                push(DefaultValue.Unknown)
            Opcodes.ASTORE, Opcodes.ISTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.LSTORE ->
                pop()
        }
    }

    override fun visitTypeInsn(opcode: Int, type: String) {
        when (opcode) {
            Opcodes.NEW -> push(DefaultValue.ConstructorCall(type, emptyList()))
            Opcodes.CHECKCAST -> {}
            else -> push(DefaultValue.Unknown)
        }
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        when (opcode) {
            Opcodes.GETSTATIC -> {
                if (descriptor == ENTITY_DATA_ACCESSOR_DESC) {
                    lastAccessorOwner = owner
                    lastAccessorName = name
                }
                push(DefaultValue.StaticField(owner, name))
            }
            Opcodes.GETFIELD -> {
                pop()
                push(DefaultValue.Unknown)
            }
            Opcodes.PUTSTATIC -> pop()
            Opcodes.PUTFIELD -> { pop(); pop() }
        }
    }

    override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
        val argCount = Type.getArgumentTypes(descriptor).size

        if (opcode == Opcodes.INVOKEVIRTUAL && owner == SYNCHED_ENTITY_DATA_BUILDER && name == "define") {
            val args = popN(argCount)
            pop()
            val accOwner = lastAccessorOwner
            val accName = lastAccessorName
            if (accOwner != null && accName != null) {
                defaults["$accOwner/$accName"] = args.last()
            }
            lastAccessorOwner = null
            lastAccessorName = null
            return
        }

        if (opcode == Opcodes.INVOKESTATIC && name == "valueOf" && owner in BOXING_TYPES) {
            return
        }

        when (opcode) {
            Opcodes.INVOKESTATIC -> {
                val args = popN(argCount)
                if (args.none { it is DefaultValue.Unknown }) {
                    push(DefaultValue.StaticCall(owner, name, args))
                } else {
                    push(DefaultValue.Unknown)
                }
            }
            Opcodes.INVOKESPECIAL -> {
                if (name == "<init>") {
                    val args = popN(argCount)
                    pop()
                    val dupRef = pop()
                    if (dupRef is DefaultValue.ConstructorCall && args.none { it is DefaultValue.Unknown }) {
                        push(DefaultValue.ConstructorCall(dupRef.type, args))
                    } else {
                        push(DefaultValue.Unknown)
                    }
                } else {
                    popN(argCount)
                    pop()
                    push(DefaultValue.Unknown)
                }
            }
            Opcodes.INVOKEVIRTUAL, Opcodes.INVOKEINTERFACE -> {
                val args = popN(argCount)
                val receiver = pop()
                if (receiver !is DefaultValue.Unknown && args.none { it is DefaultValue.Unknown }) {
                    push(DefaultValue.InstanceCall(receiver, name, args))
                } else {
                    push(DefaultValue.Unknown)
                }
            }
        }
    }
}

//</editor-fold>
