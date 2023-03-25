package xyz.xenondevs.nova.player.attachment

import net.minecraft.resources.ResourceLocation
import org.bukkit.entity.Player
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId

class AttachmentType<T : Attachment> internal constructor(val id: ResourceLocation, val constructor: (Player) -> T)