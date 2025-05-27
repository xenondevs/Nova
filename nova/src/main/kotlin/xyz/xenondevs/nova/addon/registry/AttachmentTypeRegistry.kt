package xyz.xenondevs.nova.addon.registry

import org.bukkit.entity.Player
import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.world.player.attachment.Attachment
import xyz.xenondevs.nova.world.player.attachment.AttachmentType

@Deprecated(REGISTRIES_DEPRECATION)
interface AttachmentTypeRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun <T : Attachment> registerAttachmentType(name: String, constructor: (Player) -> T): AttachmentType<T> =
        addon.registerAttachmentType(name, constructor)
    
}