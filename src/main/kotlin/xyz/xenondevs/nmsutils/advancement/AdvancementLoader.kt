package xyz.xenondevs.nmsutils.advancement

import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementTree
import net.minecraft.advancements.TreeNodePosition
import org.spigotmc.SpigotConfig
import xyz.xenondevs.nmsutils.internal.util.DEDICATED_SERVER
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry

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
        val advancementManager = DEDICATED_SERVER.advancements
        val allAdvancements = HashMap(DEDICATED_SERVER.advancements.advancements)
        filtered.forEach { allAdvancements[it.id] = it }
        val advancementTree = AdvancementTree()
        advancementTree.addAll(filtered)
        for (root in advancementTree.roots()) {
            if (root.holder().value().display().isPresent) {
                // TODO: open positioning up to the api
                TreeNodePosition.run(root)
            }
        }
        
        // set new advancements
        advancementManager.advancements = allAdvancements
        ReflectionRegistry.SERVER_ADVANCEMENT_MANAGER_TREE_FIELD.set(advancementManager, advancementTree)
    }
    
}