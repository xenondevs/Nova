package xyz.xenondevs.nova.integration.worldedit.fawe

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.event.extent.EditSessionEvent
import com.sk89q.worldedit.util.eventbus.EventHandler.Priority
import com.sk89q.worldedit.util.eventbus.Subscribe

class FAWEListener {
    
    @Subscribe(priority = Priority.VERY_LATE)
    fun handleEditSession(event: EditSessionEvent) {
        if (event.stage == EditSession.Stage.BEFORE_CHANGE) {
            event.extent = FAWENovaBlockExtentFactory.newInstance(event)
        }
    }
    
}