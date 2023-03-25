package xyz.xenondevs.nova.addon.registry

import net.minecraft.resources.ResourceLocation
import org.bukkit.entity.Player
import xyz.xenondevs.nova.player.attachment.Attachment
import xyz.xenondevs.nova.player.attachment.AttachmentType
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.set

interface AttachmentTypeRegistry : AddonGetter {
    
    fun <T : Attachment> attachmentType(name: String, constructor: (Player) -> T): AttachmentType<T> {
        val id = ResourceLocation(addon.description.id, name)
        val attachmentType = AttachmentType(id, constructor)
        
        NovaRegistries.ATTACHMENT_TYPE[id] = attachmentType
        return attachmentType
    }
    
}