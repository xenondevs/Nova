package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.kyori.adventure.text.Component
import net.minecraft.network.syncher.EntityDataSerializers
import org.bukkit.entity.Pose
import xyz.xenondevs.nova.util.component.adventure.toNMSComponent
import xyz.xenondevs.nova.util.nmsPose
import xyz.xenondevs.nova.world.fakeentity.metadata.Metadata

open class EntityMetadata internal constructor() : Metadata() {
    
    private val sharedFlags = sharedFlags(0)
    
    var isOnFire: Boolean by sharedFlags[0]
    var isCrouching: Boolean by sharedFlags[1]
    var isSprinting: Boolean by sharedFlags[3]
    var isSwimming: Boolean by sharedFlags[4]
    var isInvisible: Boolean by sharedFlags[5]
    var isGlowing: Boolean by sharedFlags[6]
    var isFlyingElytra: Boolean by sharedFlags[7]
    var airTicks: Int by entry(1, EntityDataSerializers.INT, 300)
    var isCustomNameVisible: Boolean by entry(2, EntityDataSerializers.BOOLEAN, false)
    var customName: Component? by optional(3, EntityDataSerializers.OPTIONAL_COMPONENT) { it.toNMSComponent() }
    var isSilent: Boolean by entry(4, EntityDataSerializers.BOOLEAN, false)
    var hasNoGravity: Boolean by entry(5, EntityDataSerializers.BOOLEAN, false)
    var pose: Pose by entry(6, EntityDataSerializers.POSE, Pose.STANDING) { it.nmsPose }
    var frozenTicks: Int by entry(7, EntityDataSerializers.INT, 0)
    
}