package xyz.xenondevs.nova.world.player.attachment

import net.minecraft.resources.ResourceLocation
import org.bukkit.entity.Player

class AttachmentType<T : Attachment> internal constructor(val id: ResourceLocation, val constructor: (Player) -> T)