package xyz.xenondevs.nova.addon.loader

import xyz.xenondevs.commons.collections.poll
import xyz.xenondevs.nova.addon.AddonLogger
import xyz.xenondevs.nova.loader.library.LibraryLoader
import java.net.URLClassLoader

internal object LibraryLoaderPools {
    
    private lateinit var loaders: Map<AddonLoader, URLClassLoader?>
    
    fun init(loaders: Collection<AddonLoader>) {
        val pools = ArrayList<ArrayList<AddonLoader>>()
        
        val remaining = loaders.toMutableList()
        while (remaining.isNotEmpty()) {
            pools += arrayListOf(remaining.poll()!!)
            
            remaining.removeIf { currentLoader ->
                pools.forEach { pool ->
                    if (pool.any { currentLoader.dependsOn(it) || it.dependsOn(currentLoader) }) {
                        pool += currentLoader
                        return@removeIf true
                    }
                }
                
                return@removeIf false
            }
        }
        
        val loadersMap = HashMap<AddonLoader, URLClassLoader?>()
        pools.forEach { pool ->
            val libraryClassLoader = createPooledLibraryClassLoader(pool)
            pool.forEach { loadersMap[it] = libraryClassLoader }
        }
        
        this.loaders = loadersMap
    }
    
    operator fun get(addonLoader: AddonLoader) = loaders[addonLoader]
    
    private fun AddonLoader.dependsOn(other: AddonLoader): Boolean {
        val id = other.description.id
        
        return id in description.depend || id in description.softdepend
    }
    
    private fun createPooledLibraryClassLoader(loaders: List<AddonLoader>): URLClassLoader? {
        if (loaders.all { it.description.libraries.isEmpty() })
            return null
        
        val urls = LibraryLoader.downloadLibraries(
            loaders.flatMap { it.description.repositories },
            loaders.flatMap { it.description.libraries },
            emptyList(),
            AddonLogger(loaders.joinToString { it.description.name })
        ).toTypedArray()
        
        return URLClassLoader(urls, null)
    }
    
}