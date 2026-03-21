package xyz.xenondevs.nova.world.player.attachment

import kotlinx.serialization.Serializable
import org.bukkit.entity.Player
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.serialization.kotlinx.AttachmentTypeSerializer

@Serializable(with = AttachmentTypeSerializer::class)
class AttachmentType<T : Attachment> internal constructor(
    override val entry: RegistryEntry.Nova<AttachmentType<T>>,
    val constructor: (Player) -> T
) : NovaRegistryElement<AttachmentType<T>> {
    override fun toString(): String = key.toString()
}