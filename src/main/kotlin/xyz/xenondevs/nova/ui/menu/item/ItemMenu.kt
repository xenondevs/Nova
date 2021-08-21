package xyz.xenondevs.nova.ui.menu.item

import java.util.*

interface ItemMenu {
    
    fun show()
    
    companion object {
        
        private val itemMenuHistory = HashMap<UUID, LinkedList<ItemMenu>>()
        
        fun addToHistory(uuid: UUID, menu: ItemMenu) {
            val userHistory = itemMenuHistory.getOrPut(uuid) { LinkedList() }
            userHistory += menu
            if (userHistory.size >= 10) userHistory.removeFirst()
        }
        
        fun hasHistory(uuid: UUID): Boolean {
            return itemMenuHistory[uuid]!!.size > 1
        }
        
        fun showPreviousMenu(uuid: UUID) {
            val userHistory = itemMenuHistory[uuid]
            userHistory?.removeLast()
            userHistory?.pollLast()?.show()
        }
        
    }
    
}