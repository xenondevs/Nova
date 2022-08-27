package xyz.xenondevs.nova.transformer

import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import java.lang.reflect.InvocationTargetException

private val PLUGIN_CLASS_LOADER = NOVA.loader.javaClass.classLoader

internal class PatchedClassLoader : ClassLoader(PLUGIN_CLASS_LOADER.parent.parent) {
    
    private val findClass = ReflectionUtils.getMethod(ClassLoader::class.java, true, "findClass", String::class.java)
    
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (name.startsWith("xyz.xenondevs.nova")) {
            try {
                return findClass.invoke(PLUGIN_CLASS_LOADER, name) as Class<*>
            } catch (e: InvocationTargetException) {
                throw e.cause ?: Exception()
            }
        }
        
        return super.loadClass(name, resolve)
    }
    
}