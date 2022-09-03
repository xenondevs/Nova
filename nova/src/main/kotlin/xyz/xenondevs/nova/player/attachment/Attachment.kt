package xyz.xenondevs.nova.player.attachment

import org.bukkit.entity.Player

/**
 * Superclass for all attachments.
 * 
 * @see ItemAttachment
 * @see HideDownItemAttachment
 */
interface Attachment {
    
    /**
     * The player that this [Attachment] is placed on.
     */
    val player: Player
    
    /**
     * The id of the entity that rides the [player].
     */
    val passengerId: Int
    
    /**
     * Called every tick by the [AttachmentManager].
     */
    fun handleTick()
    
    /**
     * Despawns all entities of this [Attachment].
     */
    fun despawn()
    
}