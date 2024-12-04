package xyz.xenondevs.nova.world.player.attachment

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player

class AttachmentType<T : Attachment> internal constructor(val id: Key, val constructor: (Player) -> T)