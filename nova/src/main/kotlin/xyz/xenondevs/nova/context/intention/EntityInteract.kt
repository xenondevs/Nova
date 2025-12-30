package xyz.xenondevs.nova.context.intention

import org.joml.Vector3d
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.AbstractContextIntention
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.ContextParamType
import xyz.xenondevs.nova.context.intention.EntityInteract.INTERACT_LOCATION
import xyz.xenondevs.nova.context.intention.EntityInteract.SOURCE_EYE_LOCATION
import xyz.xenondevs.nova.context.intention.EntityInteract.TARGET_ENTITY
import xyz.xenondevs.nova.util.EntityUtils
import xyz.xenondevs.nova.util.Key

/**
 * A [ContextIntention] for interacting with an entity.
 *
 * ## Autofillers
 *
 * Inherits autofillers from [HasRequiredTargetEntity], [HasHeldItem], and [HasOptionalSource].
 *
 * | Target | # | Source(s) | Notes |
 * |--------|---|-----------|-------|
 * | [INTERACT_LOCATION] | 1. | [SOURCE_EYE_LOCATION], [TARGET_ENTITY] | |
 */
object EntityInteract :
    AbstractContextIntention<EntityInteract>(),
    HasRequiredTargetEntity<EntityInteract>,
    HasHeldItem<EntityInteract>,
    HasOptionalSource<EntityInteract> {
    
    /**
     * The location that was interacted with, relative to the position of the target entity.
     */
    val INTERACT_LOCATION = ContextParamType<Vector3d, EntityInteract>(
        Key(Nova, "interact_location")
    )
    
    init {
        HasRequiredTargetEntity.applyDefaults(this)
        HasHeldItem.applyDefaults(this)
        HasOptionalSource.applyDefaults(this)
        
        addAutofiller(INTERACT_LOCATION, Autofiller.from(SOURCE_EYE_LOCATION, TARGET_ENTITY, EntityUtils::computeInteractionLocation))
    }
    
}