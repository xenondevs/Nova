package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import xyz.xenondevs.nova.world.fakeentity.metadata.MetadataSerializers

class SlimeMetadata : MobMetadata() {
    
    var size by entry(16, MetadataSerializers.VAR_INT, 1)
    
}