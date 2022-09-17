package xyz.xenondevs.nova.item

import net.md_5.bungee.api.chat.BaseComponent

class ItemDisplayData {
    
    /**
     * The displayed name of this item.
     */
    var name: Array<BaseComponent>? = null
    
    /**
     * The displayed lore of this item.
     */
    var lore: MutableList<Array<BaseComponent>>? = null
    
    /**
     * The durability percentage of this item from 0 to 1.
     */
    var durability: Double = 1.0
        set(value) {
            field = value.coerceIn(0.0..1.0)
        }
    
    /**
     * Adds multiple lore lines to the current lore.
     */
    fun addLore(lore: List<Array<BaseComponent>>) {
        if (this.lore == null)
            this.lore = ArrayList()
        
        this.lore!!.addAll(lore)
    }
    
    /**
     * Adds one lore line to the current lore.
     */
    fun addLore(lore: Array<BaseComponent>) {
        if (this.lore == null)
            this.lore = ArrayList()
        
        this.lore!!.add(lore)
    }
    
}