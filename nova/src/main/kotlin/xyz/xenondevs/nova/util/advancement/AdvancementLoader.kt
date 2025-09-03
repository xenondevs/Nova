package xyz.xenondevs.nova.util.advancement

import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementTree
import net.minecraft.advancements.TreeNodePosition
import org.spigotmc.SpigotConfig
import xyz.xenondevs.nova.util.MINECRAFT_SERVER

object AdvancementLoader {
    
    fun registerAdvancements(vararg advancements: AdvancementHolder, ignoreFilters: Boolean = false) =
        registerAdvancements(advancements.asList(), ignoreFilters)
    
    fun registerAdvancements(advancements: Iterable<AdvancementHolder>, ignoreFilters: Boolean) {
        // filter advancements
        var filtered: List<AdvancementHolder> = advancements.toList()
        if (!ignoreFilters) {
            if (SpigotConfig.disableAdvancementSaving || SpigotConfig.disabledAdvancements?.contains("*") == true)
                return
            
            val disabledAdvancements = SpigotConfig.disabledAdvancements
            if (disabledAdvancements != null)
                filtered = advancements.filterNot {
                    it.id.toString() in disabledAdvancements || it.id.namespace in disabledAdvancements
                }
        }
        
        // combine with existing advancements and build tree
        val advancementManager = MINECRAFT_SERVER.advancements
        val allAdvancements = HashMap(MINECRAFT_SERVER.advancements.advancements)
        filtered.forEach { allAdvancements[it.id] = it }
        val advancementTree = advancementManager.tree ?: AdvancementTree()
        advancementTree.addAll(filtered)
        for (root in advancementTree.roots()) {
            if (root.holder().value().display().isPresent) {
                TreeNodePosition.run(root)
            }
        }
        
        // set new advancements
        advancementManager.advancements = allAdvancements
        advancementManager.tree = advancementTree
    }
    
}