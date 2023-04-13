package xyz.xenondevs.nova.transformer

import org.bukkit.Bukkit
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.loader.NovaClassLoader

/**
 * The class loader that is responsible for loading all Bukkit and Minecraft classes.
 */
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
            return novaClassLoader.loadClass(name, resolve, false)
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
     * Checks the stacktrace for the NovaClassLoader and PatchedClassLoader to prevent recursion.
     *
     * This method must also not cause any class loads.
     */
    private fun checkNonRecursive(): Boolean {
        val stackTrace = Thread.currentThread().stackTrace
        for (i in 3..stackTrace.lastIndex) { // skip the first three elements: Thread.getStackTrace(), checkNonRecursive(), loadClass()
            val className = stackTrace[i].className
            
            // check whether the stack trace element is NovaClassLoader or PatchedClassLoader
            // if yes, this indicates a recursive call (PatchedClassLoader) or a call that will become recursive (NovaClassLoader)
            if (className == "xyz.xenondevs.nova.loader.NovaClassLoader" || className == "xyz.xenondevs.nova.transformer.PatchedClassLoader")
                return false
        }
        
        return true
    }
    
    /**
     * Checks that the class initiating the class loading process is loaded by the SpigotClassLoader.
     */
    private fun checkSpigotLoader(): Boolean =
        findLoadingClass().classLoader == SPIGOT_CLASS_LOADER
    
    /**
     * Steps through the stack frames to find the first class that triggered a class loading process.
     */
    private fun findLoadingClass(): Class<*> {
        var takeNext = false
        var loadingClass: Class<*>? = null
        
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).forEach {
            var clazz = it.declaringClass
            
            if (takeNext) {
                loadingClass = clazz
                takeNext = false
            }
            
            while (clazz != null) {
                if (clazz == ClassLoader::class.java) {
                    takeNext = true
                    break
                }
                clazz = clazz.superclass
            }
        }
        
        return loadingClass!!
    }
    
}