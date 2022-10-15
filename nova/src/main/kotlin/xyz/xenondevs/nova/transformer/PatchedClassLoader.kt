package xyz.xenondevs.nova.transformer

import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.loader.NovaClassLoader

internal class PatchedClassLoader : ClassLoader(NOVA.loader.javaClass.classLoader.parent.parent) {
    
    private val novaClassLoader = NOVA.javaClass.classLoader as NovaClassLoader
    
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // check if class is already loaded
        var c: Class<*>? = synchronized(getClassLoadingLock(name)) { findLoadedClass(name) }
        
        // Load class from Nova & libraries
        if (c == null && name.startsWith("xyz.xenondevs.nova") && checkNonPluginStackTrace()) {
            c = runCatching { novaClassLoader.loadClassNoParent(name) }.getOrNull()
        }
        
        // load class from parent (nova classloader)
        if (c == null) {
            c = parent.loadClass(name) ?: throw ClassNotFoundException(name)
        }
        
        // resolve class
        if (resolve) {
            synchronized(getClassLoadingLock(name)) { resolveClass(c) }
        }
        
        return c
    }
    
    /**
     * Checks if a class load was not caused by a plugin by analyzing the stack trace
     */
    private fun checkNonPluginStackTrace(): Boolean {
        return Thread.currentThread().stackTrace
            .none { it.className == "org.bukkit.plugin.java.PluginClassLoader" }
    }
    
}