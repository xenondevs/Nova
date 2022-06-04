package xyz.xenondevs.nova.player.attachment

import org.bukkit.entity.Player
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId

object AttachmentTypeRegistry {
    
    private val _types = HashMap<NamespacedId, AttachmentType<*>>()
    val types: List<AttachmentType<*>>
        get() = _types.values.toList()
    
    fun <T : Attachment> register(addon: Addon, name: String, constructor: (Player) -> T): AttachmentType<T> {
        val id = NamespacedId.of(name, addon.description.id)
        val attachment = AttachmentType(id, constructor)
        _types[id] = attachment
        return attachment
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : AttachmentType<*>> of(id: NamespacedId): T? {
        return _types[id] as? T
    }
    
}

class AttachmentType<T : Attachment> internal constructor(val id: NamespacedId, val constructor: (Player) -> T)