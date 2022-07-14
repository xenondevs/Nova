package xyz.xenondevs.nova.transformer

import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.util.reflection.ReflectionUtils

private val NOVA_CLASS_LOADER = Nova::class.java.classLoader

internal class PatchedClassLoader : ClassLoader(NOVA_CLASS_LOADER.parent.parent) {
    
    private val findClass = ReflectionUtils.getMethod(ClassLoader::class.java, true, "findClass", String::class.java)
    
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (name.startsWith("xyz.xenondevs.nova"))
            return findClass.invoke(NOVA_CLASS_LOADER, name) as Class<*>
        
        return super.loadClass(name, resolve)
    }
    
}