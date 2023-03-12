package xyz.xenondevs.nova.integration.worldedit.normal

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.event.extent.EditSessionEvent
import com.sk89q.worldedit.util.eventbus.EventHandler
import com.sk89q.worldedit.util.eventbus.Subscribe

class WEListener {
    
    @Subscribe(priority = EventHandler.Priority.VERY_LATE)
    fun handleEditSession(event: EditSessionEvent) {
        if (event.stage == EditSession.Stage.BEFORE_CHANGE) {
            event.extent = WENovaBlockExtent(event)
        }
    }
    
}