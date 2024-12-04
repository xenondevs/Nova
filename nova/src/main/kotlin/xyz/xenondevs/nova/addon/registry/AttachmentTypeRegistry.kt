package xyz.xenondevs.nova.addon.registry

import org.bukkit.entity.Player
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.player.attachment.Attachment
import xyz.xenondevs.nova.world.player.attachment.AttachmentType

interface AttachmentTypeRegistry : AddonHolder {
    
    fun <T : Attachment> registerAttachmentType(name: String, constructor: (Player) -> T): AttachmentType<T> {
        val id = Key(addon, name)
        val attachmentType = AttachmentType(id, constructor)
        
        NovaRegistries.ATTACHMENT_TYPE[id] = attachmentType
        return attachmentType
    }
    
}