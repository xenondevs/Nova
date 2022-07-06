package xyz.xenondevs.nmsutils.advancement

import net.minecraft.advancements.TreeNodePosition
import org.spigotmc.SpigotConfig
import xyz.xenondevs.nmsutils.internal.util.DEDICATED_SERVER
import xyz.xenondevs.nmsutils.internal.util.resourceLocation

object AdvancementLoader {
    
    fun registerAdvancements(vararg advancements: Advancement, ignoreFilters: Boolean = false) =
        registerAdvancements(advancements.asList(), ignoreFilters)
    
    fun registerAdvancements(advancements: Iterable<Advancement>, ignoreFilters: Boolean) {
        // filter advancements
        var filtered: Iterable<Advancement>? = null
        if (!ignoreFilters) {
            if (SpigotConfig.disableAdvancementSaving || SpigotConfig.disabledAdvancements?.contains("*") == true)
                return
            
            val disabledAdvancements = SpigotConfig.disabledAdvancements
            if (disabledAdvancements != null)
                filtered = advancements.filterNot {
                    disabledAdvancements.contains(it.id) || disabledAdvancements.contains(it.id.split(':')[0])
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
            .filter { it.parent == null && it.display?.hidden == false }
            .forEach {
                val advancement = DEDICATED_SERVER.advancements.advancements.advancements[it.id.resourceLocation]
                if (advancement != null)
                    TreeNodePosition.run(advancement)
            }
        // send advancements to players
        DEDICATED_SERVER.playerList.reloadResources()
    }
    
}