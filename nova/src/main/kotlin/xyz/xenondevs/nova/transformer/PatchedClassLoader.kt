package xyz.xenondevs.nova.transformer

import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.loader.NovaClassLoader


/**
 * The [PatchedClassLoader] is a class loader that is injected in the class loading hierarchy.
 * It is the parent of the SpigotClassLoader and is used as a bridge to load classes referenced in patches.
 *
 * Hierarchy:
 * NovaClassLoader -> PluginClassLoader -> SpigotClassLoader -> PatchedClassLoader -> ApplicationClassLoader -> PlatformClassLoader -> BootClassLoader
 *
 * If the ApplicationClassLoader (and parents) cannot find the requested class and the class load was triggered by
 * the SpigotClassLoader (so a class referenced in Bukkit / NMS code, added in a patch), the class loading process is
 * restarted at the [NovaClassLoader].
 */
internal class PatchedClassLoader : ClassLoader(NOVA.loader.javaClass.classLoader.parent.parent) {
    
    private val novaClassLoader = NOVA.javaClass.classLoader as NovaClassLoader
    
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // Check if class is already loaded
        var c: Class<*>? = synchronized(getClassLoadingLock(name)) { findLoadedClass(name) }
        
        // Load class from parent (ApplicationClassLoader)
        if (c == null) {
            c = runCatching { parent.loadClass(name) }.getOrNull()
        }
        
        // Load class from Nova & libraries
        if (c == null && checkNonRecursiveStackTrace()) {
            // Restarts the class loading process at the NovaClassLoader
            return novaClassLoader.loadClass(name, resolve)
        }
        
        if (c == null) {
            throw ClassNotFoundException(name)
        }
        
        // Resolve class
        if (resolve) {
            synchronized(getClassLoadingLock(name)) { resolveClass(c) }
        }
        
        return c
    }
    
    /**
     * Checks that a class load was not caused by Nova or another plugin.
     */
    private fun checkNonRecursiveStackTrace(): Boolean {
        return Thread.currentThread().stackTrace
            .none {
                it.className == "org.bukkit.plugin.java.PluginClassLoader"
                    || it.className == "xyz.xenondevs.nova.loader.NovaClassLoader"
            }
    }
    
}