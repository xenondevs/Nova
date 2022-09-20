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
     * The damage shown with advanced tooltips enabled.
     */
    var damage: Int? = null
    
    /**
     * The maximum durability shown with advanced tooltips enabled.
     */
    var maxDurability: Int? = null
    
    private var _durabilityBar: Double? = null
    
    /**
     * The durability percentage of this item from 0 to 1.
     */
    var durabilityBar: Double
        set(value) {
            _durabilityBar = value.coerceIn(0.0..1.0)
        }
        get() {
            val percentage = _durabilityBar
            if (percentage != null)
                return percentage
    
            val damage = damage
            val maxDurability = maxDurability
            return if (damage != null && maxDurability != null) {
                (maxDurability - damage) / maxDurability.toDouble()
            } else 1.0
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