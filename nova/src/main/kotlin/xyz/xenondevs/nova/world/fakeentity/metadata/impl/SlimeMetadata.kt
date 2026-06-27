@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.syncher.EntityDataSerializers
import xyz.xenondevs.nova.world.fakeentity.FAKE_ENTITY_DEPRECATION

@Deprecated(FAKE_ENTITY_DEPRECATION)
class SlimeMetadata : MobMetadata() {
    
    var size: Int by entry(16, EntityDataSerializers.INT, 1)
    
}