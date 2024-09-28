package xyz.xenondevs.nova.util.advancement

import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementTree
import net.minecraft.advancements.TreeNodePosition
import net.minecraft.server.ServerAdvancementManager
import org.spigotmc.SpigotConfig
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.reflection.ReflectionUtils

private val SERVER_ADVANCEMENT_MANAGER_TREE_FIELD = ReflectionUtils.getField(ServerAdvancementManager::class, "tree")

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
        val advancementTree = SERVER_ADVANCEMENT_MANAGER_TREE_FIELD.get(advancementManager) as AdvancementTree? ?: AdvancementTree()
        advancementTree.addAll(filtered)
        for (root in advancementTree.roots()) {
            if (root.holder().value().display().isPresent) {
                TreeNodePosition.run(root)
            }
        }
        
        // set new advancements
        advancementManager.advancements = allAdvancements
        SERVER_ADVANCEMENT_MANAGER_TREE_FIELD.set(advancementManager, advancementTree)
    }
    
}