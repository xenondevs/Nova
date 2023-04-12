package xyz.xenondevs.nova.transformer

import org.bukkit.Bukkit
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.loader.NovaClassLoader

// The class loader responsible for loading all bukkit and minecraft classes
private val SPIGOT_CLASS_LOADER = Bukkit::class.java.classLoader

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
internal class PatchedClassLoader : ClassLoader(SPIGOT_CLASS_LOADER.parent) {
    
    private val novaClassLoader = NOVA.javaClass.classLoader as NovaClassLoader
    
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // Check if class is already loaded
        var c: Class<*>? = synchronized(getClassLoadingLock(name)) { findLoadedClass(name) }
        
        // Load class from parent (ApplicationClassLoader)
        if (c == null) {
            c = runCatching { parent.loadClass(name) }.getOrNull()
        }
        
        // Load class from Nova & libraries
        if (c == null && checkNonRecursive() && checkSpigotLoader()) {
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
     * Looks for the NovaClassLoader in the stack trace to prevent recursion.
     */
    private fun checkNonRecursive(): Boolean =
        Thread.currentThread().stackTrace.none { it.className == "xyz.xenondevs.nova.loader.NovaClassLoader" }
    
    /**
     * Checks that the class initiating the class loading process is loaded by the SpigotClassLoader.
     */
    private fun checkSpigotLoader(): Boolean =
        findLoadingClass().classLoader == SPIGOT_CLASS_LOADER
    
    /**
     * Steps through the stack frames to find the class that triggered the class loading process.
     */
    private fun findLoadingClass(): Class<*> {
        var foundLoadClass = false // used to filter out the methods after loadClass (checkSpigotLoader, findLoadingClass) 
        var clazz: Class<*>? = null
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).forEach {
            if (clazz == null) {
                if (it.methodName == "loadClass") {
                    foundLoadClass = true
                } else if (foundLoadClass) {
                    clazz = it.declaringClass
                }
            }
        }
        
        return clazz ?: throw IllegalStateException("Could not find the loading class")
    }
    
}