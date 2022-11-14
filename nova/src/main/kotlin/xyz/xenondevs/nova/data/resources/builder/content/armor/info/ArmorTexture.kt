package xyz.xenondevs.nova.data.resources.builder.content.armor.info

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.Resources

data class ArmorTexture(val color: Int) {
    
    companion object {
        
        fun of(addon: Addon, name: String): ArmorTexture =
            Resources.getArmorData(NamespacedId(addon, name))
        
    }
    
}