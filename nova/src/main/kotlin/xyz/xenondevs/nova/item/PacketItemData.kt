package xyz.xenondevs.nova.item

import net.md_5.bungee.api.chat.BaseComponent
import net.minecraft.nbt.CompoundTag
import xyz.xenondevs.nova.item.vanilla.HideableFlag

/**
 * Stores data that should be added when creating the clientside item.
 *
 * Note that all values contained in this class are only for display purposes and do not affect gameplay.
 */
@Suppress("MemberVisibilityCanBePrivate")
class PacketItemData internal constructor(val nbt: CompoundTag) {
    
    /**
     * The displayed name of this item.
     */
    var name: Array<BaseComponent>? = null
    
    /**
     * The displayed lore of this item.
     */
    var lore: MutableList<Array<BaseComponent>>? = null
    
    /**
     * The advanced tooltips lore of this item which is displayed when Nova's advanced tooltips are enabled.
     */
    var advancedTooltipsLore: MutableList<Array<BaseComponent>>? = null
    
    /**
     * The durability percentage of this item from 0 to 1.
     */
    var durabilityBar: Double = 1.0
    
    /**
     * The flags that should be hidden on this item.
     */
    var hiddenFlags: MutableList<HideableFlag> = mutableListOf(HideableFlag.MODIFIERS)
    
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
    
    /**
     * Adds multiple lore lines to the advanced tooltips lore.
     */
    fun addAdvancedTooltipsLore(lore: List<Array<BaseComponent>>) {
        if (this.advancedTooltipsLore == null)
            this.advancedTooltipsLore = ArrayList()
        
        this.advancedTooltipsLore!!.addAll(lore)
    }
    
    /**
     * Adds one lore line to the advanced tooltips lore.
     */
    fun addAdvancedTooltipsLore(lore: Array<BaseComponent>) {
        if (this.advancedTooltipsLore == null)
            this.advancedTooltipsLore = ArrayList()
        
        this.advancedTooltipsLore!!.add(lore)
    }
    
    /**
     * Hides something from the item.
     */
    fun hide(vararg hideableFlags: HideableFlag) {
        hiddenFlags.addAll(hideableFlags)
    }
    
}