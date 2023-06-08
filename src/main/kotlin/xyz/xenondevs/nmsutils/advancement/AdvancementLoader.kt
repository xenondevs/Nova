package xyz.xenondevs.nmsutils.advancement

import net.minecraft.advancements.TreeNodePosition
import org.spigotmc.SpigotConfig
import xyz.xenondevs.nmsutils.internal.util.DEDICATED_SERVER

object AdvancementLoader {
    
    fun registerAdvancements(vararg advancements: Advancement, ignoreFilters: Boolean = false) =
        registerAdvancements(advancements.asList(), ignoreFilters)
    
    fun registerAdvancements(advancements: Iterable<Advancement>, ignoreFilters: Boolean) {
        // filter advancements
        var filtered: List<Advancement>? = null
        if (!ignoreFilters) {
            if (SpigotConfig.disableAdvancementSaving || SpigotConfig.disabledAdvancements?.contains("*") == true)
                return
            
            val disabledAdvancements = SpigotConfig.disabledAdvancements
            if (disabledAdvancements != null)
                filtered = advancements.filterNot {
                    it.id.toString() in disabledAdvancements || it.id.namespace in disabledAdvancements
                }
        }
        
        // convert advancements
        val converted = (filtered ?: advancements).associate(Advancement::toNMS)
        // register advancements
        DEDICATED_SERVER.advancements.advancements.add(converted)
        // set positioning
        // TODO: open positioning up to the api
        advancements
            .asSequence()
            .filter { it.parent == null && it.display?.isHidden == false }
            .forEach {
                val advancement = DEDICATED_SERVER.advancements.advancements.advancements[it.id]
                if (advancement != null)
                    TreeNodePosition.run(advancement)
            }
    }
    
}