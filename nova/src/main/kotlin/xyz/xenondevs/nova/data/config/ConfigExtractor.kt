package xyz.xenondevs.nova.data.config

import org.spongepowered.configurate.CommentedConfigurationNode
import xyz.xenondevs.nova.util.data.walk
import java.nio.file.Path
import kotlin.io.path.exists

internal object ConfigExtractor {
    
    private val storedConfigs: MutableMap<String, CommentedConfigurationNode> = loadStoredConfigs()
    
    fun extract(configPath: String, destFile: Path, fileInZip: Path): CommentedConfigurationNode {
        val internalCfg = Configs.createLoader(fileInZip).load()
        val storedCfg = storedConfigs[configPath]
        
        val loader = Configs.createLoader(destFile)
        val cfg: CommentedConfigurationNode
        if (!destFile.exists() || storedCfg == null) {
            cfg = internalCfg.copy()
            storedConfigs[configPath] = internalCfg.copy()
        } else cfg = updateExistingConfig(loader.load(), storedCfg, internalCfg)
        
        loader.save(cfg)
        return cfg
    }
    
    private fun updateExistingConfig(
        cfg: CommentedConfigurationNode,
        storedCfg: CommentedConfigurationNode,
        internalCfg: CommentedConfigurationNode
    ): CommentedConfigurationNode {
        internalCfg.walk().forEach { internalNode ->
            val path = internalNode.path()
            val configuredNode = cfg.node(path)
            
            val internalValue = internalNode.raw()
            if (configuredNode.virtual()) {
                // add new key
                configuredNode.raw(internalValue)
                storedCfg.node(path).set(internalValue)
            } else {
                // update value if unchanged by user
                if (internalNode.childrenMap().isNotEmpty()) // update only terminal nodes
                    return@forEach
                
                val storedNode = storedCfg.node(path)
                if (internalNode != configuredNode && storedNode == configuredNode) {
                    configuredNode.raw(internalValue)
                    storedNode.raw(internalValue)
                }
            }
            
            // reset comments
            configuredNode.comment(internalNode.comment())
        }
        
        // remove keys that were once extracted but are no longer in the internal config
        cfg.walk().forEach { node ->
            val path = node.path()
            if (internalCfg.node(path).virtual() && !storedCfg.node(path).virtual()) {
                cfg.removeChild(path)
                storedCfg.removeChild(path)
            }
        }
        
        return cfg
    }
    
    private fun loadStoredConfigs(): MutableMap<String, CommentedConfigurationNode> {
        return PermanentStorage.retrieve<HashMap<String, String>>("storedConfigs", ::HashMap)
            .mapValuesTo(HashMap()) { (_, cfgStr) -> 
                Configs.createBuilder().buildAndLoadString(cfgStr) as CommentedConfigurationNode
            }
    }
    
    internal fun saveStoredConfigs() {
        PermanentStorage.store(
            "storedConfigs",
            storedConfigs.mapValues { (_, cfg) -> Configs.createBuilder().buildAndSaveString(cfg) }
        )
    }
    
}