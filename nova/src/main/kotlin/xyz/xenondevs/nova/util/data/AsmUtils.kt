package xyz.xenondevs.nova.util.data

import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.util.accessWrapper
import xyz.xenondevs.bytebase.util.internalName
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

internal object AsmUtils {
    
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
    
}

