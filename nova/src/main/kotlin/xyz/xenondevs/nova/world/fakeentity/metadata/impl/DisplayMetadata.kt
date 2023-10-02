package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.util.Brightness
import net.minecraft.world.entity.Display.BillboardConstraints
import org.joml.Quaternionf
import org.joml.Vector3f

abstract class DisplayMetadata : EntityMetadata() {
    
    var posRotInterpolationDuration: Int by entry(8, EntityDataSerializers.INT, 0)
    var transformationInterpolationDelay: Int by entry(9, EntityDataSerializers.INT, 0)
    var transformationInterpolationDuration: Int by entry(10, EntityDataSerializers.INT, 0)
    var translation: Vector3f by entry(11, EntityDataSerializers.VECTOR3, Vector3f())
    var scale: Vector3f by entry(12, EntityDataSerializers.VECTOR3, Vector3f(1f, 1f, 1f))
    var rightRotation: Quaternionf by entry(13, EntityDataSerializers.QUATERNION, Quaternionf())
    var leftRotation: Quaternionf by entry(14, EntityDataSerializers.QUATERNION, Quaternionf())
    var billboardConstraints: BillboardConstraints by entry(15, EntityDataSerializers.BYTE, BillboardConstraints.FIXED) { it.ordinal.toByte() }
    private val brightnessEntry = entry<Brightness?, Int>(16, EntityDataSerializers.INT, null, { it?.pack() ?: -1 }, { Brightness.unpack(it) })
    var rawBrightness: Int by brightnessEntry.rawDelegate
    var brightness: Brightness? by brightnessEntry.mappedDelegate
    var viewRange: Float by entry(17, EntityDataSerializers.FLOAT, 1f)
    var shadowRadius: Float by entry(18, EntityDataSerializers.FLOAT, 0f)
    var shadowStrength: Float by entry(19, EntityDataSerializers.FLOAT, 1f)
    var width: Float by entry(20, EntityDataSerializers.FLOAT, 0f)
    var height: Float by entry(21, EntityDataSerializers.FLOAT, 0f)
    var glowColor: Int by entry(22, EntityDataSerializers.INT, -1)
    
}