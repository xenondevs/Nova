package xyz.xenondevs.nova.util.data

import org.objectweb.asm.Type
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.asm.InsnBuilder
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.util.accessWrapper
import xyz.xenondevs.bytebase.util.copy

internal object AsmUtils {
    
    fun listNonOverriddenMethods(subClass: ClassWrapper, superClass: ClassWrapper): List<MethodNode> {
        val methods = ArrayList<MethodNode>()
        
        for(method in superClass.methods) {
            if (method.name == "<init>" || method.name == "<clinit>" || method.accessWrapper.isPrivate() || method.accessWrapper.isStatic() || method.accessWrapper.isFinal())
                continue
            
            if (subClass.getMethod(method.name, method.desc) == null)
                methods += method
        }
        
        return methods
    }
    
    fun createDelegateInstructions(loadDelegate: InsnList, callDelegate: InsnList, delegateTo: MethodNode): InsnList {
        val methodType = Type.getMethodType(delegateTo.desc)
        return buildInsnList { 
            addLabel()
            add(loadDelegate.copy())
            loadMethodParametersToStack(delegateTo.accessWrapper.isStatic(), methodType)
            add(callDelegate.copy())
            addLabel()
            returnMethodResult(methodType)
        }
    }
    
    private fun InsnBuilder.loadMethodParametersToStack(static: Boolean, methodType: Type) {
        var idx = if (static) 0 else 1
        
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