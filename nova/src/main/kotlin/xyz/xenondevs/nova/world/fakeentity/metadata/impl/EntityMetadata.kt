package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Pose
import xyz.xenondevs.nova.world.fakeentity.metadata.Metadata
import xyz.xenondevs.nova.world.fakeentity.metadata.MetadataSerializers

open class EntityMetadata internal constructor() : Metadata() {
    
    private val sharedFlags = sharedFlags(0)
    
    var isOnFire: Boolean by sharedFlags[0]
    var isCrouching: Boolean by sharedFlags[1]
    var isSprinting: Boolean by sharedFlags[3]
    var isSwimming: Boolean by sharedFlags[4]
    var isInvisible: Boolean by sharedFlags[5]
    var isGlowing: Boolean by sharedFlags[6]
    var isFlyingElytra: Boolean by sharedFlags[7]
    var airTicks: Int by entry(1, MetadataSerializers.VAR_INT, 300)
    var customName: Component? by entry(2, MetadataSerializers.OPT_COMPONENT, null)
    var isCustomNameVisible: Boolean by entry(3, MetadataSerializers.BOOLEAN, false)
    var isSilent: Boolean by entry(4, MetadataSerializers.BOOLEAN, false)
    var hasNoGravity: Boolean by entry(5, MetadataSerializers.BOOLEAN, false)
    var pose: Pose by entry(6, MetadataSerializers.POSE, Pose.STANDING)
    var frozenTicks: Int by entry(7, MetadataSerializers.VAR_INT, 0)
    
}