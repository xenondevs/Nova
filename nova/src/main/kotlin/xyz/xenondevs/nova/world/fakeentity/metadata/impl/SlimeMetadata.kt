package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.syncher.EntityDataSerializers

class SlimeMetadata : MobMetadata() {
    
    var size by entry(16, EntityDataSerializers.INT, 1)
    
}