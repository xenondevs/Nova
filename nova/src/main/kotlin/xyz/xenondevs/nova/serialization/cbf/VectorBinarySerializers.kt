package xyz.xenondevs.nova.serialization.cbf

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
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer

internal object Vector2iBinarySerializer : UnversionedBinarySerializer<Vector2i>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector2i(reader.readInt(), reader.readInt())
    
    override fun writeUnversioned(obj: Vector2i, writer: ByteWriter) {
        writer.writeInt(obj.x)
        writer.writeInt(obj.y)
    }
    
    override fun copyNonNull(obj: Vector2i) = Vector2i(obj)
    
}

internal object Vector2icBinarySerializer : UnversionedBinarySerializer<Vector2ic>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector2i(reader.readInt(), reader.readInt())
    
    override fun writeUnversioned(obj: Vector2ic, writer: ByteWriter) {
        writer.writeInt(obj.x())
        writer.writeInt(obj.y())
    }
    
    override fun copyNonNull(obj: Vector2ic) = Vector2i(obj)
    
}

internal object Vector2dBinarySerializer : UnversionedBinarySerializer<Vector2d>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector2d(reader.readDouble(), reader.readDouble())
    
    override fun writeUnversioned(obj: Vector2d, writer: ByteWriter) {
        writer.writeDouble(obj.x)
        writer.writeDouble(obj.y)
    }
    
    override fun copyNonNull(obj: Vector2d) = Vector2d(obj)
    
}

internal object Vector2dcBinarySerializer : UnversionedBinarySerializer<Vector2dc>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector2d(reader.readDouble(), reader.readDouble())
    
    override fun writeUnversioned(obj: Vector2dc, writer: ByteWriter) {
        writer.writeDouble(obj.x())
        writer.writeDouble(obj.y())
    }
    
    override fun copyNonNull(obj: Vector2dc) = Vector2d(obj)
    
}

internal object Vector2fBinarySerializer : UnversionedBinarySerializer<Vector2f>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector2f(reader.readFloat(), reader.readFloat())
    
    override fun writeUnversioned(obj: Vector2f, writer: ByteWriter) {
        writer.writeFloat(obj.x)
        writer.writeFloat(obj.y)
    }
    
    override fun copyNonNull(obj: Vector2f) = Vector2f(obj)
    
}

internal object Vector2fcBinarySerializer : UnversionedBinarySerializer<Vector2fc>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector2f(reader.readFloat(), reader.readFloat())
    
    override fun writeUnversioned(obj: Vector2fc, writer: ByteWriter) {
        writer.writeFloat(obj.x())
        writer.writeFloat(obj.y())
    }
    
    override fun copyNonNull(obj: Vector2fc) = Vector2f(obj)
    
}

internal object Vector3iBinarySerializer : UnversionedBinarySerializer<Vector3i>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector3i(reader.readInt(), reader.readInt(), reader.readInt())
    
    override fun writeUnversioned(obj: Vector3i, writer: ByteWriter) {
        writer.writeInt(obj.x)
        writer.writeInt(obj.y)
        writer.writeInt(obj.z)
    }
    
    override fun copyNonNull(obj: Vector3i) = Vector3i(obj)
    
}

internal object Vector3icBinarySerializer : UnversionedBinarySerializer<Vector3ic>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector3i(reader.readInt(), reader.readInt(), reader.readInt())
    
    override fun writeUnversioned(obj: Vector3ic, writer: ByteWriter) {
        writer.writeInt(obj.x())
        writer.writeInt(obj.y())
        writer.writeInt(obj.z())
    }
    
    override fun copyNonNull(obj: Vector3ic) = Vector3i(obj)
    
}

internal object Vector3dBinarySerializer : UnversionedBinarySerializer<Vector3d>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector3d(reader.readDouble(), reader.readDouble(), reader.readDouble())
    
    override fun writeUnversioned(obj: Vector3d, writer: ByteWriter) {
        writer.writeDouble(obj.x)
        writer.writeDouble(obj.y)
        writer.writeDouble(obj.z)
    }
    
    override fun copyNonNull(obj: Vector3d) = Vector3d(obj)
    
}

internal object Vector3dcBinarySerializer : UnversionedBinarySerializer<Vector3dc>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector3d(reader.readDouble(), reader.readDouble(), reader.readDouble())
    
    override fun writeUnversioned(obj: Vector3dc, writer: ByteWriter) {
        writer.writeDouble(obj.x())
        writer.writeDouble(obj.y())
        writer.writeDouble(obj.z())
    }
    
    override fun copyNonNull(obj: Vector3dc) = Vector3d(obj)
    
}

internal object Vector3fBinarySerializer : UnversionedBinarySerializer<Vector3f>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector3f(reader.readFloat(), reader.readFloat(), reader.readFloat())
    
    override fun writeUnversioned(obj: Vector3f, writer: ByteWriter) {
        writer.writeFloat(obj.x)
        writer.writeFloat(obj.y)
        writer.writeFloat(obj.z)
    }
    
    override fun copyNonNull(obj: Vector3f) = Vector3f(obj)
    
}

internal object Vector3fcBinarySerializer : UnversionedBinarySerializer<Vector3fc>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector3f(reader.readFloat(), reader.readFloat(), reader.readFloat())
    
    override fun writeUnversioned(obj: Vector3fc, writer: ByteWriter) {
        writer.writeFloat(obj.x())
        writer.writeFloat(obj.y())
        writer.writeFloat(obj.z())
    }
    
    override fun copyNonNull(obj: Vector3fc) = Vector3f(obj)
    
}

internal object Vector4iBinarySerializer : UnversionedBinarySerializer<Vector4i>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector4i(reader.readInt(), reader.readInt(), reader.readInt(), reader.readInt())
    
    override fun writeUnversioned(obj: Vector4i, writer: ByteWriter) {
        writer.writeInt(obj.x)
        writer.writeInt(obj.y)
        writer.writeInt(obj.z)
        writer.writeInt(obj.w)
    }
    
    override fun copyNonNull(obj: Vector4i) = Vector4i(obj)
    
}

internal object Vector4icBinarySerializer : UnversionedBinarySerializer<Vector4ic>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector4i(reader.readInt(), reader.readInt(), reader.readInt(), reader.readInt())
    
    override fun writeUnversioned(obj: Vector4ic, writer: ByteWriter) {
        writer.writeInt(obj.x())
        writer.writeInt(obj.y())
        writer.writeInt(obj.z())
        writer.writeInt(obj.w())
    }
    
    override fun copyNonNull(obj: Vector4ic) = Vector4i(obj)
    
}

internal object Vector4dBinarySerializer : UnversionedBinarySerializer<Vector4d>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector4d(reader.readDouble(), reader.readDouble(), reader.readDouble(), reader.readDouble())
    
    override fun writeUnversioned(obj: Vector4d, writer: ByteWriter) {
        writer.writeDouble(obj.x)
        writer.writeDouble(obj.y)
        writer.writeDouble(obj.z)
        writer.writeDouble(obj.w)
    }
    
    override fun copyNonNull(obj: Vector4d) = Vector4d(obj)
    
}

internal object Vector4dcBinarySerializer : UnversionedBinarySerializer<Vector4dc>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector4d(reader.readDouble(), reader.readDouble(), reader.readDouble(), reader.readDouble())
    
    override fun writeUnversioned(obj: Vector4dc, writer: ByteWriter) {
        writer.writeDouble(obj.x())
        writer.writeDouble(obj.y())
        writer.writeDouble(obj.z())
        writer.writeDouble(obj.w())
    }
    
    override fun copyNonNull(obj: Vector4dc) = Vector4d(obj)
    
}

internal object Vector4fBinarySerializer : UnversionedBinarySerializer<Vector4f>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector4f(reader.readFloat(), reader.readFloat(), reader.readFloat(), reader.readFloat())
    
    override fun writeUnversioned(obj: Vector4f, writer: ByteWriter) {
        writer.writeFloat(obj.x)
        writer.writeFloat(obj.y)
        writer.writeFloat(obj.z)
        writer.writeFloat(obj.w)
    }
    
    override fun copyNonNull(obj: Vector4f) = Vector4f(obj)
    
}

internal object Vector4fcBinarySerializer : UnversionedBinarySerializer<Vector4fc>() {
    
    override fun readUnversioned(reader: ByteReader) = Vector4f(reader.readFloat(), reader.readFloat(), reader.readFloat(), reader.readFloat())
    
    override fun writeUnversioned(obj: Vector4fc, writer: ByteWriter) {
        writer.writeFloat(obj.x())
        writer.writeFloat(obj.y())
        writer.writeFloat(obj.z())
        writer.writeFloat(obj.w())
    }
    
    override fun copyNonNull(obj: Vector4fc) = Vector4f(obj)
    
}