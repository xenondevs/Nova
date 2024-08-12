package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import org.joml.Matrix4f
import org.joml.Matrix4fc

internal object Matrix4fcTypeAdapter : TypeAdapter<Matrix4fc?>() {
    
    override fun write(writer: JsonWriter, value: Matrix4fc?) {
        if (value != null) {
            writer.beginArray()
            writer.value(value.m00())
            writer.value(value.m01())
            writer.value(value.m02())
            writer.value(value.m03())
            writer.value(value.m10())
            writer.value(value.m11())
            writer.value(value.m12())
            writer.value(value.m13())
            writer.value(value.m20())
            writer.value(value.m21())
            writer.value(value.m22())
            writer.value(value.m23())
            writer.value(value.m30())
            writer.value(value.m31())
            writer.value(value.m32())
            writer.value(value.m33())
            writer.endArray()
        } else 
            writer.nullValue()
    }
    
    @Suppress("DuplicatedCode")
    override fun read(reader: JsonReader): Matrix4fc? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        
        reader.beginArray()
        val m00 = reader.nextDouble().toFloat()
        val m01 = reader.nextDouble().toFloat()
        val m02 = reader.nextDouble().toFloat()
        val m03 = reader.nextDouble().toFloat()
        val m10 = reader.nextDouble().toFloat()
        val m11 = reader.nextDouble().toFloat()
        val m12 = reader.nextDouble().toFloat()
        val m13 = reader.nextDouble().toFloat()
        val m20 = reader.nextDouble().toFloat()
        val m21 = reader.nextDouble().toFloat()
        val m22 = reader.nextDouble().toFloat()
        val m23 = reader.nextDouble().toFloat()
        val m30 = reader.nextDouble().toFloat()
        val m31 = reader.nextDouble().toFloat()
        val m32 = reader.nextDouble().toFloat()
        val m33 = reader.nextDouble().toFloat()
        reader.endArray()
        
        return Matrix4f(
            m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33
        )
    }
    
}