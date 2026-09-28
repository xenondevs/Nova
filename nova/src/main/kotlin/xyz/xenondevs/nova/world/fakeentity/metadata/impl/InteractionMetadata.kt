@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.syncher.EntityDataSerializers
import xyz.xenondevs.nova.world.fakeentity.FAKE_ENTITY_DEPRECATION

@Deprecated(FAKE_ENTITY_DEPRECATION)
class InteractionMetadata : EntityMetadata() {
    
    var width: Float by entry(8, EntityDataSerializers.FLOAT, 1f)
    var height: Float by entry(9, EntityDataSerializers.FLOAT, 1f)
    var response: Boolean by entry(10, EntityDataSerializers.BOOLEAN, false)
    
}