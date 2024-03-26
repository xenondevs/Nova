package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.core.Rotations
import net.minecraft.network.syncher.EntityDataSerializers
import org.joml.Vector3f
import org.joml.Vector3fc

private fun Vector3fc.toRotations(): Rotations = Rotations(x(), y(), z())

class ArmorStandMetadata : LivingEntityMetadata() {
    
    private val sharedFlags = sharedFlags(15)
    
    var isSmall: Boolean by sharedFlags[0]
    var hasArms: Boolean by sharedFlags[2]
    var hasNoBasePlate: Boolean by sharedFlags[3]
    var isMarker: Boolean by sharedFlags[4]
    var headRotation: Vector3fc by entry(16, EntityDataSerializers.ROTATIONS, Vector3f(), Vector3fc::toRotations)
    var bodyRotation: Vector3fc by entry(17, EntityDataSerializers.ROTATIONS, Vector3f(), Vector3fc::toRotations)
    var leftArmRotation: Vector3fc by entry(18, EntityDataSerializers.ROTATIONS, Vector3f(), Vector3fc::toRotations)
    var rightArmRotation: Vector3fc by entry(19, EntityDataSerializers.ROTATIONS, Vector3f(), Vector3fc::toRotations)
    var leftLegRotation: Vector3fc by entry(20, EntityDataSerializers.ROTATIONS, Vector3f(), Vector3fc::toRotations)
    var rightLegRotation: Vector3fc by entry(21, EntityDataSerializers.ROTATIONS, Vector3f(), Vector3fc::toRotations)
    
}