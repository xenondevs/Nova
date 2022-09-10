package xyz.xenondevs.nova.transformer

import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import java.lang.reflect.InvocationTargetException

internal class PatchedClassLoader : ClassLoader(NOVA.loader.javaClass.classLoader.parent.parent) {
    
    private val novaClassLoader = NOVA.javaClass.classLoader
    private val findClass = ReflectionUtils.getMethod(ClassLoader::class.java, true, "findClass", String::class.java)
    
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (name.startsWith("xyz.xenondevs.nova")) {
            try {
                return findClass.invoke(novaClassLoader, name) as Class<*>
            } catch (e: InvocationTargetException) {
                throw e.cause ?: Exception()
            }
        }
        
        return parent.loadClass(name)
    }
    
}