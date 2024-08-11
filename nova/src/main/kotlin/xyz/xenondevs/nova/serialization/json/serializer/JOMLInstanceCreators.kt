package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.InstanceCreator
import org.joml.Matrix2d
import org.joml.Matrix2dc
import org.joml.Matrix2f
import org.joml.Matrix2fc
import org.joml.Matrix3d
import org.joml.Matrix3dc
import org.joml.Matrix3f
import org.joml.Matrix3fc
import org.joml.Matrix3x2d
import org.joml.Matrix3x2dc
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fc
import org.joml.Matrix4d
import org.joml.Matrix4dc
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Matrix4x3d
import org.joml.Matrix4x3dc
import org.joml.Matrix4x3f
import org.joml.Matrix4x3fc
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Quaternionf
import org.joml.Quaternionfc
import org.joml.Vector2d
import org.joml.Vector2dc
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector2i
import org.joml.Vector2ic
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.joml.Vector4d
import org.joml.Vector4dc
import org.joml.Vector4f
import org.joml.Vector4fc
import org.joml.Vector4i
import org.joml.Vector4ic
import java.lang.reflect.Type

internal object Matrix2dcInstanceCreator : InstanceCreator<Matrix2dc> {
    override fun createInstance(type: Type?) = Matrix2d()
}

internal object Matrix2fcInstanceCreator : InstanceCreator<Matrix2fc> {
    override fun createInstance(type: Type?) = Matrix2f()
}

internal object Matrix3dcInstanceCreator : InstanceCreator<Matrix3dc> {
    override fun createInstance(type: Type?) = Matrix3d()
}

internal object Matrix3fcInstanceCreator : InstanceCreator<Matrix3fc> {
    override fun createInstance(type: Type?) = Matrix3f()
}

internal object Matrix3x2dcInstanceCreator : InstanceCreator<Matrix3x2dc> {
    override fun createInstance(type: Type?) = Matrix3x2d()
}

internal object Matrix3x2fcInstanceCreator : InstanceCreator<Matrix3x2fc> {
    override fun createInstance(type: Type?) = Matrix3x2f()
}

internal object Matrix4dcInstanceCreator : InstanceCreator<Matrix4dc> {
    override fun createInstance(type: Type?) = Matrix4d()
}

internal object Matrix4fcInstanceCreator : InstanceCreator<Matrix4fc> {
    override fun createInstance(type: Type?) = Matrix4f()
}

internal object Matrix4x3dcInstanceCreator : InstanceCreator<Matrix4x3dc> {
    override fun createInstance(type: Type?) = Matrix4x3d()
}

internal object Matrix4x3fcInstanceCreator : InstanceCreator<Matrix4x3fc> {
    override fun createInstance(type: Type?) = Matrix4x3f()
}

internal object QuaterniondcInstanceCreator : InstanceCreator<Quaterniondc> {
    override fun createInstance(type: Type?) = Quaterniond()
}

internal object QuaternionfcInstanceCreator : InstanceCreator<Quaternionfc> {
    override fun createInstance(type: Type?) = Quaternionf()
}

internal object Vector2dcInstanceCreator : InstanceCreator<Vector2dc> {
    override fun createInstance(type: Type?) = Vector2d()
}

internal object Vector2fcInstanceCreator : InstanceCreator<Vector2fc> {
    override fun createInstance(type: Type?) = Vector2f()
}

internal object Vector2icInstanceCreator : InstanceCreator<Vector2ic> {
    override fun createInstance(type: Type?) = Vector2i()
}

internal object Vector3dcInstanceCreator : InstanceCreator<Vector3dc> {
    override fun createInstance(type: Type?) = Vector3d()
}

internal object Vector3fcInstanceCreator : InstanceCreator<Vector3fc> {
    override fun createInstance(type: Type?) = Vector3f()
}

internal object Vector3icInstanceCreator : InstanceCreator<Vector3ic> {
    override fun createInstance(type: Type?) = Vector3i()
}

internal object Vector4dcInstanceCreator : InstanceCreator<Vector4dc> {
    override fun createInstance(type: Type?) = Vector4d()
}

internal object Vector4fcInstanceCreator : InstanceCreator<Vector4fc> {
    override fun createInstance(type: Type?) = Vector4f()
}

internal object Vector4icInstanceCreator : InstanceCreator<Vector4ic> {
    override fun createInstance(type: Type?) = Vector4i()
}

