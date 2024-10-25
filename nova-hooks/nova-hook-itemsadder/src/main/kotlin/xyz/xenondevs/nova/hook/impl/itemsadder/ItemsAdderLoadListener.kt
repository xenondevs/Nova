@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.hook.impl.itemsadder

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent
import dev.lone.itemsadder.api.ItemsAdder
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import xyz.xenondevs.nova.integration.LoadListener
import xyz.xenondevs.nova.util.unregisterEvents
import java.util.concurrent.CompletableFuture

internal object ItemsAdderLoadListener : LoadListener, Listener {
    
    override val loaded = CompletableFuture<Boolean>()
    
    init {
        if (ItemsAdder.areItemsLoaded()) {
            loaded.complete(true)
        } else {
            Bukkit.getPluginManager().registerEvents(this, Bukkit.getPluginManager().getPlugin("Nova")!!)
        }
    }
    
    @EventHandler
    private fun handleItemsAdderLoadData(event: ItemsAdderLoadDataEvent) {
        if (event.cause == ItemsAdderLoadDataEvent.Cause.FIRST_LOAD) {
            loaded.complete(true)
            unregisterEvents()
        }
    }
    
}