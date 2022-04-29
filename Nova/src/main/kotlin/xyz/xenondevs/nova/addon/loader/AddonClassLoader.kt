package xyz.xenondevs.nova.addon.loader

import xyz.xenondevs.nova.addon.AddonManager
import java.net.URLClassLoader

internal class AddonClassLoader(private val loader: AddonLoader, parent: ClassLoader) : URLClassLoader(arrayOf(loader.file.toURI().toURL()), parent) {
    
    private var libraryLoader: LibraryClassLoader? = null
    private var addonDependencies: List<AddonClassLoader>? = null
    
    fun setDependencyClassLoaders() {
        libraryLoader = AddonLibraryLoader(loader).createClassLoader()
        addonDependencies = (loader.description.depend + loader.description.softdepend)
            .mapNotNull { AddonManager.loaders[it]?.classLoader }
            .takeUnless(List<*>::isEmpty)
    }
    
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            // check if class is already loaded
            var c: Class<*>? = findLoadedClass(name)
            
            // Load class from this addon or its libraries
            if (c == null) {
                c = loadAddonClass(name)
            }
            
            // Load class from addon dependencies / their libraries
            if (c == null) {
                c = addonDependencies?.firstNotNullOfOrNull { it.loadAddonClass(name) }
            }
            
            // load class from parent (nova classloader)
            if (c == null) {
                c = parent.loadClass(name)
                checkNotNull(c)
            }
            
            if (resolve) {
                resolveClass(c)
            }
            
            return c
        }
    }
    
    private fun loadAddonClass(name: String): Class<*>? {
        var c: Class<*>? = null
        
        val libraryLoader = libraryLoader
        if (libraryLoader != null) {
            c = libraryLoader.runCatching { this.loadClass(name, false) }.getOrNull()
        }
        
        if (c == null) {
            c = runCatching { findClass(name) }.getOrNull()
        }
        
        return c
    }
    
}