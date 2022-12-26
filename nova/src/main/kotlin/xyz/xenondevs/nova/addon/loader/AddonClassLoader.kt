package xyz.xenondevs.nova.addon.loader

import xyz.xenondevs.nova.addon.AddonManager
import java.net.URLClassLoader

internal class AddonClassLoader(private val loader: AddonLoader, parent: ClassLoader) : URLClassLoader(arrayOf(loader.file.toURI().toURL()), parent) {
    
    private var libraryLoader: URLClassLoader? = null
    private var addonDependencies: List<AddonClassLoader>? = null
    
    fun setDependencyClassLoaders() {
        libraryLoader = LibraryLoaderPools[loader]
        addonDependencies = (loader.description.depend + loader.description.softdepend)
            .mapNotNull { AddonManager.loaders[it]?.classLoader }
            .takeUnless(List<*>::isEmpty)
    }
    
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            // check if class is already loaded
            var c: Class<*>? = findLoadedClass(name)
            
            // Load class from this addon
            if (c == null) {
                c = loadAddonClass(name)
            }
            
            // Load class from libraries (includes dependency libraries)
            if (c == null) {
                c = loadLibraryClass(name)
            }
            
            // Load class from addon dependencies
            if (c == null) {
                c = addonDependencies?.firstNotNullOfOrNull { runCatching { it.loadClass(name, true) }.getOrNull() }
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
    
    private fun loadAddonClass(name: String): Class<*>? {
        return runCatching { findClass(name) }.getOrNull()
    }
    
    private fun loadLibraryClass(name: String): Class<*>? {
        val libraryLoader = libraryLoader
        if (libraryLoader != null) {
            return libraryLoader.runCatching { this.loadClass(name) }.getOrNull()
        }
        
        return null
    }
    
}