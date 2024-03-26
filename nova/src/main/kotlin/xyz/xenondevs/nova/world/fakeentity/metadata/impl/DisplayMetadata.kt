@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import com.mojang.math.MatrixUtil
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.util.Brightness
import net.minecraft.world.entity.Display.BillboardConstraints
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Quaternionf
import org.joml.Quaternionfc
import org.joml.Vector3f
import org.joml.Vector3fc
import xyz.xenondevs.nova.world.fakeentity.metadata.NonNullMetadataEntry

abstract class DisplayMetadata : EntityMetadata() {
    
    var posRotInterpolationDuration: Int by entry(8, EntityDataSerializers.INT, 0)
    var transformationInterpolationDelay: Int by entry(9, EntityDataSerializers.INT, 0)
    var transformationInterpolationDuration: Int by entry(10, EntityDataSerializers.INT, 0)
    var translation: Vector3fc by entry(11, EntityDataSerializers.VECTOR3, Vector3f()) as NonNullMetadataEntry<Vector3fc>
    var scale: Vector3fc by entry(12, EntityDataSerializers.VECTOR3, Vector3f(1f, 1f, 1f)) as NonNullMetadataEntry<Vector3fc>
    var rightRotation: Quaternionfc by entry(13, EntityDataSerializers.QUATERNION, Quaternionf()) as NonNullMetadataEntry<Quaternionfc>
    var leftRotation: Quaternionfc by entry(14, EntityDataSerializers.QUATERNION, Quaternionf()) as NonNullMetadataEntry<Quaternionfc>
    var transform: Matrix4fc
        get() = Matrix4f()
            .translate(translation)
            .rotate(rightRotation)
            .scale(scale)
            .rotate(leftRotation)
        set(value) {
            val f = 1f / value.m33()
            val triple = MatrixUtil.svdDecompose(Matrix3f(value).scale(f))
            
            translation = value.getTranslation(Vector3f()).mul(f)
            leftRotation = triple.left
            scale = triple.middle
            rightRotation = triple.right
        }
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