package xyz.xenondevs.nova.util.data

import org.objectweb.asm.Type
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.asm.InsnBuilder
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.util.accessWrapper
import xyz.xenondevs.bytebase.util.copy
import xyz.xenondevs.bytebase.util.internalName
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

internal object AsmUtils {
    
    fun listNonOverriddenMethods(subClass: ClassWrapper, superClass: ClassWrapper): List<MethodNode> {
        val methods = ArrayList<MethodNode>()
        
        for (method in superClass.methods) {
            if (method.name == "<init>" || method.name == "<clinit>" || method.accessWrapper.isPrivate() || method.accessWrapper.isStatic() || method.accessWrapper.isFinal())
                continue
            
            if (subClass.getMethod(method.name, method.desc) == null)
                methods += method
        }
        
        return methods
    }
    
    fun listNonOverriddenMethods(clazz: ClassWrapper, vararg ignored: KClass<*>): List<MethodNode> {
        val ignoredNames = HashSet<String>()
        for (ignoredClass in ignored) {
            ignoredNames += ignoredClass.internalName
            for (superClass in ignoredClass.superclasses) {
                ignoredNames += superClass.internalName
            }
        }
        
        return listNonOverriddenMethods(clazz, ignored.mapTo(HashSet()) { it.internalName })
    }
    
    fun listNonOverriddenMethods(clazz: ClassWrapper, ignored: Set<String>): List<MethodNode> {
        val methods = ArrayList<MethodNode>()
        
        for (superClass in clazz.superClasses) {
            if (superClass.name in ignored)
                continue
            
            for (method in superClass.methods) {
                if (method.name == "<init>" || method.name == "<clinit>" || method.accessWrapper.isPrivate() || method.accessWrapper.isStatic() || method.accessWrapper.isFinal())
                    continue
                
                if (clazz.getMethod(method.name, method.desc) == null)
                    methods += method
                
            }
        }
        
        return methods
    }
    
    fun createDelegateInstructions(
        loadDelegate: InsnList,
        callDelegate: InsnList,
        delegateTo: MethodNode,
        paramIdxStart: Int = if (delegateTo.accessWrapper.isStatic()) 0 else 1
    ): InsnList {
        val methodType = Type.getMethodType(delegateTo.desc)
        return buildInsnList {
            addLabel()
            add(loadDelegate.copy())
            loadMethodParametersToStack(paramIdxStart, methodType)
            add(callDelegate.copy())
            addLabel()
            returnMethodResult(methodType)
        }
    }
    
    private fun InsnBuilder.loadMethodParametersToStack(idxStart: Int, methodType: Type) {
        var idx = idxStart
        
        for (argType in methodType.argumentTypes) {
            when (argType.sort) {
                Type.BOOLEAN -> iLoad(idx)
                Type.CHAR -> iLoad(idx)
                Type.BYTE -> iLoad(idx)
                Type.SHORT -> iLoad(idx)
                Type.INT -> iLoad(idx)
                Type.FLOAT -> fLoad(idx)
                Type.LONG -> lLoad(idx)
                Type.DOUBLE -> dLoad(idx)
                Type.ARRAY -> aLoad(idx)
                Type.OBJECT -> aLoad(idx)
                else -> throw IllegalArgumentException("Unknown arg type: $argType")
            }
            idx += argType.size
        }
    }
    
    private fun InsnBuilder.returnMethodResult(methodType: Type) {
        val returnType = methodType.returnType
        when (returnType.sort) {
            Type.VOID -> _return()
            Type.BOOLEAN -> ireturn()
            Type.CHAR -> ireturn()
            Type.BYTE -> ireturn()
            Type.SHORT -> ireturn()
            Type.INT -> ireturn()
            Type.FLOAT -> freturn()
            Type.LONG -> lreturn()
            Type.DOUBLE -> dreturn()
            Type.ARRAY -> areturn()
            Type.OBJECT -> areturn()
            else -> throw IllegalArgumentException("Unknown return type: $returnType")
        }
    }
    
}

