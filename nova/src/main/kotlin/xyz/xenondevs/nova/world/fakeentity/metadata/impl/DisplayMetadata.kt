package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.syncher.EntityDataSerializers
import org.joml.Quaternionf
import org.joml.Vector3f

abstract class DisplayMetadata : EntityMetadata() {
    
    var interpolationDelay: Int by entry(8, EntityDataSerializers.INT, 0)
    var interpolationDuration: Int by entry(9, EntityDataSerializers.INT, 0)
    var translation: Vector3f by entry(10, EntityDataSerializers.VECTOR3, Vector3f())
    var scale: Vector3f by entry(11, EntityDataSerializers.VECTOR3, Vector3f(1f, 1f, 1f))
    var leftRotation: Quaternionf by entry(12, EntityDataSerializers.QUATERNION, Quaternionf())
    var rightRotation: Quaternionf by entry(13, EntityDataSerializers.QUATERNION, Quaternionf())
    var billboardConstraints: Byte by entry(14, EntityDataSerializers.BYTE, 0)
    var brightness: Int by entry(15, EntityDataSerializers.INT, -1)
    var viewRange: Float by entry(16, EntityDataSerializers.FLOAT, 1f)
    var shadowRadius: Float by entry(17, EntityDataSerializers.FLOAT, 0f)
    var shadowStrength: Float by entry(18, EntityDataSerializers.FLOAT, 1f)
    var width: Float by entry(19, EntityDataSerializers.FLOAT, 0f)
    var height: Float by entry(20, EntityDataSerializers.FLOAT, 0f)
    var glowColor: Int by entry(21, EntityDataSerializers.INT, -1)
    
    var blockLight: Int
        get() = (brightness shr 20) and 0xF
        set(value) {
            brightness = skyLight shl 20 or (value shl 4)
        }
    
    var skyLight: Int
        get() = (brightness shr 4) and 0xF
        set(value) {
            brightness = value shl 20 or (blockLight shl 4)
        }
    
}