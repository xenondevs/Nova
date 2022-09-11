package xyz.xenondevs.nova.transformer

import xyz.xenondevs.nova.NOVA

internal class PatchedClassLoader : ClassLoader(NOVA.loader.javaClass.classLoader.parent.parent) {
    
    private val novaClassLoader = NOVA.javaClass.classLoader
    
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            // check if class is already loaded
            var c: Class<*>? = findLoadedClass(name)
            
            // Load class from Nova & libraries
            if (c == null && name.startsWith("xyz.xenondevs.nova") && checkNonPluginStackTrace()) {
                c = runCatching { novaClassLoader.loadClass(name) }.getOrNull()
            }
            
            // load class from parent (nova classloader)
            if (c == null) {
                c = parent.loadClass(name)
            }
            
            // should never be true because parent class loader should throw ClassNotFoundException before
            if (c == null) {
                throw ClassNotFoundException(name)
            }
            
            if (resolve) {
                resolveClass(c)
            }
            
            return c
        }
    }
    
    /**
     * Checks if a class load was not caused by a plugin by analyzing the stack trace
     */
    private fun checkNonPluginStackTrace(): Boolean {
        return Thread.currentThread().stackTrace
            .none { it.className == "org.bukkit.plugin.java.PluginClassLoader" }
    }
    
}