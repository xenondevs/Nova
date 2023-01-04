package xyz.xenondevs.nova.transformer

import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.loader.NovaClassLoader

// The PatchedClassLoader is the parent of the ApplicationClassLoader and sits between the ApplicationClassLoader and the ExtensionClassLoader.
internal class PatchedClassLoader : ClassLoader(NOVA.loader.javaClass.classLoader.parent.parent) {
    
    private val novaClassLoader = NOVA.javaClass.classLoader as NovaClassLoader
    
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // check if class is already loaded
        var c: Class<*>? = synchronized(getClassLoadingLock(name)) { findLoadedClass(name) }
        
        // load class from parent (Extension ClassLoader)
        if (c == null) {
            c = runCatching { parent.loadClass(name) }.getOrNull()
        }
    
        // Load class from Nova & libraries
        if (c == null && checkNonPluginStackTrace()) {
            c = runCatching { novaClassLoader.loadClassNoParent(name) }.getOrNull()
        }
        
        if (c == null) {
            throw ClassNotFoundException(name)
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