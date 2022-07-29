package xyz.xenondevs.nova.addon.loader

import xyz.xenondevs.nova.util.pollFirst

internal object LibraryLoaderPools {
    
    private lateinit var loaders: Map<AddonLoader, LibraryClassLoader?>
    
    fun init(loaders: Collection<AddonLoader>) {
        val pools = ArrayList<ArrayList<AddonLoader>>()
        
        val remaining = loaders.toMutableList()
        while (remaining.isNotEmpty()) {
            pools += arrayListOf(remaining.pollFirst()!!)
            
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
        
        val loadersMap = HashMap<AddonLoader, LibraryClassLoader?>()
        pools.forEach { pool ->
            val libraryClassLoader = AddonLibraryLoader(pool).createClassLoader()
            pool.forEach { loadersMap[it] = libraryClassLoader }
        }
        
        this.loaders = loadersMap
    }
    
    operator fun get(addonLoader: AddonLoader) = loaders[addonLoader]
    
    private fun AddonLoader.dependsOn(other: AddonLoader): Boolean {
        val id = other.description.id
        
        return id in description.depend || id in description.softdepend
    }
    
}