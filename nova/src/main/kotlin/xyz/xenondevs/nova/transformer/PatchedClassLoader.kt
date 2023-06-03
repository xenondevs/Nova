package xyz.xenondevs.nova.transformer

import org.bukkit.Bukkit
import xyz.xenondevs.commons.reflection.toMethodHandle
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.loader.NovaClassLoader
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getMethod

/**
 * The class loader that is responsible for loading all Bukkit and Minecraft classes.
 */
private val SPIGOT_CLASS_LOADER = Bukkit::class.java.classLoader

private val CLASS_LOADER_FIND_LOADED_CLASS_METHOD = getMethod(ClassLoader::class, true, "findLoadedClass", String::class)
private val CLASS_LOADER_FIND_CLASS_METHOD = getMethod(ClassLoader::class, true, "findClass", String::class)
private val CLASS_LOADER_RESOLVE_CLASS_METHOD = getMethod(ClassLoader::class, true, "resolveClass", Class::class)

private val SPIGOT_CLASS_LOADER_FIND_LOADED_CLASS = CLASS_LOADER_FIND_LOADED_CLASS_METHOD.toMethodHandle(SPIGOT_CLASS_LOADER)
private val SPIGOT_CLASS_LOADER_FIND_CLASS = CLASS_LOADER_FIND_CLASS_METHOD.toMethodHandle(SPIGOT_CLASS_LOADER)
private val SPIGOT_CLASS_LOADER_RESOLVE_CLASS = CLASS_LOADER_RESOLVE_CLASS_METHOD.toMethodHandle(SPIGOT_CLASS_LOADER)

/**
 * The [PatchedClassLoader] is a class loader that is injected in the class loading hierarchy.
 * It is the parent of the SpigotClassLoader and is used as a bridge to load classes referenced in patches.
 *
 * Hierarchy:
 * NovaClassLoader -> PluginClassLoader -> SpigotClassLoader (URLClassLoader) -> PatchedClassLoader -> ApplicationClassLoader -> PlatformClassLoader -> BootClassLoader
 *
 * If the ApplicationClassLoader (and parents) cannot find the requested class and the class load was triggered by
 * the SpigotClassLoader (so from a class that was potentially patched), the [PatchedClassLoader] will:
 *
 * 1. Look for the class in the SpigotClassLoader. This fixes issues with classes from Nova's libraries being loaded
 * when they're actually part of the server's libraries (e.g. kyori-adventure on Paper servers).
 * 2. Try to load the class using the NovaClassLoader. The NovaClassLoader will then not check its parent to prevent recursion.
 */
internal class PatchedClassLoader : ClassLoader(SPIGOT_CLASS_LOADER.parent) {
    
    private val novaClassLoader = NOVA.javaClass.classLoader as NovaClassLoader
    
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // load class from parent (ApplicationClassLoader)
        var c: Class<*>? = runCatching { parent.loadClass(name) }.getOrNull()
        
        if (c == null && checkNonRecursive() && checkSpigotLoader()) {
            // first, check if the class is in the SpigotClassLoader
             c = runCatching { findClassInSpigotLoader(name, resolve) }.getOrNull()
            
            // if the class is not in the SpigotClassLoader, try to load it from Nova 
            if (c == null) {
                c = runCatching { novaClassLoader.loadClass(name, resolve, false) }.getOrNull()
            }
        }
        
        if (c == null)
            throw ClassNotFoundException(name)
        
        return c
    }
    
    private fun findClassInSpigotLoader(name: String, resolve: Boolean): Class<*> {
        var c: Class<*>? = SPIGOT_CLASS_LOADER_FIND_LOADED_CLASS.invoke(name) as Class<*>?
        
        if (c == null) {
            // throws InvocationTargetException with ClassNotFoundException as cause if class is not found
            c = SPIGOT_CLASS_LOADER_FIND_CLASS.invoke(name) as Class<*>
        }
        
        if (resolve) {
            SPIGOT_CLASS_LOADER_RESOLVE_CLASS.invoke(c)
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