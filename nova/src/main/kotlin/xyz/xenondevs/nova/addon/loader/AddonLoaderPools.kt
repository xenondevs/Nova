package xyz.xenondevs.nova.addon.loader

import xyz.xenondevs.commons.collections.CollectionUtils
import xyz.xenondevs.nova.addon.AddonLogger
import xyz.xenondevs.nova.addon.library.LibraryLoader
import xyz.xenondevs.nova.addon.library.NovaLibraryLoader
import java.net.URL
import java.net.URLClassLoader

internal object AddonLoaderPools {
    
    fun createPooledClassLoaders(loaders: Collection<AddonLoader>): Map<AddonLoader, URLClassLoader> =
        createPooledClassLoaders(poolLoaders(loaders))
    
    @JvmName("createPooledClassLoaders1")
    private fun createPooledClassLoaders(pools: Collection<Collection<AddonLoader>>): Map<AddonLoader, URLClassLoader> {
        val map = HashMap<AddonLoader, URLClassLoader>()
        
        for (pool in pools) {
            val urls = ArrayList<URL>()
            pool.forEach { urls += it.file.toURI().toURL() }
            
            if (pool.any { it.libraries.isNotEmpty() }) {
                LibraryLoader.downloadLibraries(
                    pool.flatMap { it.repositories },
                    pool.flatMap { it.libraries },
                    NovaLibraryLoader.getAllExclusions() + pool.flatMap { it.exclusions },
                    AddonLogger(pool.joinToString { it.description.name })
                ).forEach { urls += it.toURI().toURL() }
            }
            
            val classLoader = URLClassLoader(urls.toTypedArray(), this::class.java.classLoader)
            pool.forEach { map[it] = classLoader }
        }
        
        return map
    }
    
    private fun poolLoaders(loaders: Collection<AddonLoader>): Set<Set<AddonLoader>> {
        val dependencies: Map<AddonLoader, Set<AddonLoader>> = loaders.associateWith { loader ->
            val dependencyIds = loader.description.depend + loader.description.softdepend
            dependencyIds.mapTo(HashSet()) { depId -> loaders.first { candidate -> candidate.description.id == depId } }
        }
        
        return CollectionUtils.poolDependencies(loaders) { dependencies[it]!! }
    }
    
}
