package xyz.xenondevs.nova.data.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector4d
import org.joml.Vector4dc
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.model.Model
import xyz.xenondevs.nova.data.resources.builder.model.Model.*
import xyz.xenondevs.nova.data.resources.builder.model.Model.Element.Face
import xyz.xenondevs.nova.data.resources.builder.model.Model.Element.Rotation

internal object ModelTypeAdapter : TypeAdapter<Model>() {
    
    //<editor-fold desc="write">
    override fun write(writer: JsonWriter, value: Model?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        
        writer.beginObject()
        
        val parentPath = value.parent
        if (parentPath != null) {
            writer.name("parent")
            writer.value(parentPath.toString())
        }
        
        val textures = value.textures
        if (textures.isNotEmpty()) {
            writer.name("textures")
            writeTextures(writer, value.textures)
        }
        
        val elements = value.elements
        if (elements != null) {
            writer.name("elements")
            writeElements(writer, elements)
        }
        
        // block-specific
        if (!value.ambientOcclusion) {
            writer.name("ambientocclusion")
            writer.value(false)
        }
        
        // item-specific
        val guiLight = value.guiLight
        if (guiLight != GuiLight.SIDE) {
            writer.name("gui_light")
            writer.value(guiLight.name.lowercase())
        }
        
        val display = value.display
        if (display.isNotEmpty()) {
            writer.name("display")
            writeDisplays(writer, value.display)
        }
        
        val overrides = value.overrides
        if (overrides.isNotEmpty()) {
            writer.name("overrides")
            writeOverrides(writer, overrides)
        }
        
        writer.endObject()
    }
    
    private fun writeTextures(writer: JsonWriter, textures: Map<String, String>) {
        writer.beginObject()
        for ((key, value) in textures) {
            writer.name(key)
            writer.value(value)
        }
        writer.endObject()
    }
    
    private fun writeElements(writer: JsonWriter, elements: List<Element>) {
        writer.beginArray()
        for (element in elements) {
            writeElement(writer, element)
        }
        writer.endArray()
    }
    
    private fun writeElement(writer: JsonWriter, element: Element) {
        writer.beginObject()
        
        writer.name("from")
        writeVector3d(writer, element.from)
        
        writer.name("to")
        writeVector3d(writer, element.to)
        
        val rotation = element.rotation
        if (rotation != null) {
            writer.name("rotation")
            writeRotation(writer, rotation)
        }
        
        writer.name("faces")
        writeFaces(writer, element.faces)
        
        if (!element.shade) {
            writer.name("shade")
            writer.value(false)
        }
        
        writer.endObject()
    }
    
    private fun writeRotation(writer: JsonWriter, rotation: Rotation) {
        writer.beginObject()
        
        writer.name("angle")
        writer.value(rotation.angle)
        
        writer.name("axis")
        writer.value(rotation.axis.name.lowercase())
        
        writer.name("origin")
        writeVector3d(writer, rotation.origin)
        
        if (rotation.rescale) {
            writer.name("rescale")
            writer.value(true)
        }
        
        writer.endObject()
    }
    
    private fun writeFaces(writer: JsonWriter, faces: Map<Direction, Face>) {
        writer.beginObject()
        for ((faceName, face) in faces) {
            writer.name(faceName.name.lowercase())
            writeFace(writer, face)
        }
        writer.endObject()
    }
    
    private fun writeFace(writer: JsonWriter, face: Face) {
        writer.beginObject()
        
        val uv = face.uv
        if (uv != null) {
            writer.name("uv")
            writeVector4d(writer, face.uv)
        }
        
        writer.name("texture")
        writer.value(face.texture)
        
        val cullface = face.cullface
        if (cullface != null) {
            writer.name("cullface")
            writer.value(cullface.name.lowercase())
        }
        
        val rotation = face.rotation
        if (rotation != 0) {
            writer.name("rotation")
            writer.value(rotation)
        }
        
        val tintIndex = face.tintIndex
        if (tintIndex != -1) {
            writer.name("tintindex")
            writer.value(tintIndex)
        }
        
        writer.endObject()
    }
    
    private fun writeDisplays(writer: JsonWriter, displays: Map<Display.Position, Display>) {
        writer.beginObject()
        for ((position, display) in displays) {
            writer.name(position.name.lowercase())
            writeDisplay(writer, display)
        }
        writer.endObject()
    }
    
    private fun writeDisplay(writer: JsonWriter, display: Display) {
        writer.beginObject()
        
        if (display.rotation != Vector3d(0.0, 0.0, 0.0)) {
            writer.name("rotation")
            writeVector3d(writer, display.rotation)
        }
        
        if (display.translation != Vector3d(0.0, 0.0, 0.0)) {
            writer.name("translation")
            writeVector3d(writer, display.translation)
        }
        
        if (display.scale != Vector3d(1.0, 1.0, 1.0)) {
            writer.name("scale")
            writeVector3d(writer, display.scale)
        }
        
        writer.endObject()
    }
    
    private fun writeOverrides(writer: JsonWriter, overrides: List<Override>) {
        writer.beginArray()
        for (override in overrides) {
            writeOverride(writer, override)
        }
        writer.endArray()
    }
    
    private fun writeOverride(writer: JsonWriter, override: Override) {
        writer.beginObject()
        
        writer.name("predicate")
        writePredicate(writer, override.predicate)
        
        writer.name("model")
        writer.value(override.model.toString())
        
        writer.endObject()
    }
    
    private fun writePredicate(writer: JsonWriter, predicate: Map<String, Number>) {
        writer.beginObject()
        for ((key, value) in predicate) {
            writer.name(key)
            writer.value(value)
        }
        writer.endObject()
    }
    
    private fun writeVector3d(writer: JsonWriter, vector: Vector3dc) {
        writer.beginArray()
        writer.value(vector.x())
        writer.value(vector.y())
        writer.value(vector.z())
        writer.endArray()
    }
    
    private fun writeVector4d(writer: JsonWriter, vector: Vector4dc) {
        writer.beginArray()
        writer.value(vector.x())
        writer.value(vector.y())
        writer.value(vector.z())
        writer.value(vector.w())
        writer.endArray()
    }
    //</editor-fold>
    
    //<editor-fold desc="read">
    override fun read(reader: JsonReader): Model {
        reader.beginObject()
        
        var parentName: String? = null
        var textures: Map<String, String>? = null
        var elements: List<Element>? = null
        var ambientOcclusion = true
        var guiLight = GuiLight.SIDE
        var display: Map<Display.Position, Display>? = null
        var overrides: List<Override>? = null
        
        while (reader.peek() == JsonToken.NAME) {
            when (reader.nextName()) {
                "parent" -> parentName = reader.nextString()
                "textures" -> textures = readTextures(reader)
                "elements" -> elements = readElements(reader)
                "display" -> display = readDisplays(reader)
                
                // block-specific
                "ambientocclusion" -> ambientOcclusion = reader.nextBoolean()
                
                // item-specific
                "gui_light" -> guiLight = readGuiLight(reader)
                "overrides" -> overrides = readOverrides(reader)
                
                else -> reader.skipValue()
            }
        }
        
        reader.endObject()
        return Model(
            parentName?.let(ResourcePath::of),
            textures ?: emptyMap(),
            elements,
            ambientOcclusion,
            guiLight,
            display ?: emptyMap(),
            overrides ?: emptyList()
        )
    }
    
    private fun readTextures(reader: JsonReader): Map<String, String> {
        val textures = HashMap<String, String>()
        
        reader.beginObject()
        while (reader.peek() == JsonToken.NAME) {
            textures[reader.nextName()] = reader.nextString()
        }
        reader.endObject()
        
        return textures
    }
    
    private fun readElements(reader: JsonReader): List<Element> {
        val elements = ArrayList<Element>()
        
        reader.beginArray()
        while (reader.peek() == JsonToken.BEGIN_OBJECT) {
            elements += readElement(reader)
        }
        reader.endArray()
        
        return elements
    }
    
    private fun readElement(reader: JsonReader): Element {
        reader.beginObject()
        
        var from: Vector3d? = null
        var to: Vector3d? = null
        var rotation: Rotation? = null
        var faces: Map<Direction, Face>? = null
        var shade = true
        
        while (reader.peek() == JsonToken.NAME) {
            when (reader.nextName()) {
                "from" -> from = readVector3d(reader)
                "to" -> to = readVector3d(reader)
                "rotation" -> rotation = readRotation(reader)
                "faces" -> faces = readFaces(reader)
                "shade" -> shade = reader.nextBoolean()
                else -> reader.skipValue()
            }
        }
        
        reader.endObject()
        
        return Element(
            from ?: throw IllegalArgumentException("Missing property 'from'"),
            to ?: throw IllegalArgumentException("Missing property 'to'"),
            rotation,
            faces ?: throw IllegalArgumentException("Missing property 'faces'"),
            shade
        )
    }
    
    private fun readRotation(reader: JsonReader): Rotation {
        reader.beginObject()
        
        var angle: Double? = null
        var axis: Axis? = null
        var origin: Vector3d? = null
        var rescale = false
        
        while (reader.peek() == JsonToken.NAME) {
            when (reader.nextName()) {
                "angle" -> angle = reader.nextDouble()
                "axis" -> axis = Axis.valueOf(reader.nextString().uppercase())
                "origin" -> origin = readVector3d(reader)
                "rescale" -> rescale = reader.nextBoolean()
                else -> reader.skipValue()
            }
        }
        
        reader.endObject()
        
        return Rotation(
            angle ?: throw IllegalArgumentException("Missing property 'angle'"),
            axis ?: throw IllegalArgumentException("Missing property 'axis'"),
            origin ?: throw IllegalArgumentException("Missing property 'origin'"), 
            rescale
        )
    }
    
    private fun readFaces(reader: JsonReader): Map<Direction, Face> {
        val faces = enumMap<Direction, Face>()
        
        reader.beginObject()
        while (reader.peek() == JsonToken.NAME) {
            val blockFace = Direction.valueOf(reader.nextName().uppercase())
            faces[blockFace] = readFace(reader)
        }
        reader.endObject()
        
        return faces
    }
    
    private fun readFace(reader: JsonReader): Face {
        reader.beginObject()
        
        var uv: Vector4d? = null
        var texture: String? = null
        var cullface: Direction? = null
        var rotation = 0
        var tintIndex = -1
        
        while (reader.peek() == JsonToken.NAME) {
            when (reader.nextName()) {
                "uv" -> uv = readVector4d(reader)
                "texture" -> texture = reader.nextString()
                "cullface" -> cullface = Direction.valueOf(reader.nextString().uppercase())
                "rotation" -> rotation = reader.nextInt()
                "tintindex" -> tintIndex = reader.nextInt()
                else -> reader.skipValue()
            }
        }
        
        reader.endObject()
        
        return Face(
            uv,
            texture ?: throw IllegalStateException("Missing property 'textures'"),
            cullface,
            rotation,
            tintIndex
        )
    }
    
    private fun readDisplays(reader: JsonReader): Map<Display.Position, Display> {
        val displays = enumMap<Display.Position, Display>()
        
        reader.beginObject()
        while (reader.peek() == JsonToken.NAME) {
            val position = Display.Position.valueOf(reader.nextName().uppercase())
            displays[position] = readDisplay(reader)
        }
        reader.endObject()
        
        return displays
    }
    
    private fun readDisplay(reader: JsonReader): Display {
        reader.beginObject()
        
        var rotation: Vector3d? = null
        var translation: Vector3d? = null
        var scale: Vector3d? = null
        
        while (reader.peek() == JsonToken.NAME) {
            when (reader.nextName()) {
                "rotation" -> rotation = readVector3d(reader)
                "translation" -> translation = readVector3d(reader)
                "scale" -> scale = readVector3d(reader)
                else -> reader.skipValue()
            }
        }
        
        reader.endObject()
        
        return Display(
            rotation ?: Vector3d(0.0, 0.0, 0.0), 
            translation ?: Vector3d(0.0, 0.0, 0.0),
            scale ?: Vector3d(1.0, 1.0, 1.0)
        )
    }
    
    private fun readGuiLight(reader: JsonReader): GuiLight {
        return GuiLight.valueOf(reader.nextString().uppercase())
    }
    
    private fun readOverrides(reader: JsonReader): List<Override> {
        val overrides = ArrayList<Override>()
        
        reader.beginArray()
        while (reader.peek() == JsonToken.BEGIN_OBJECT) {
            overrides += readOverride(reader)
        }
        reader.endArray()
        
        return overrides
    }
    
    private fun readOverride(reader: JsonReader): Override {
        reader.beginObject()
        
        var predicate: Map<String, Number>? = null
        var model: ResourcePath? = null
        
        while (reader.peek() == JsonToken.NAME) {
            when (reader.nextName()) {
                "predicate" -> predicate = readPredicate(reader)
                "model" -> model = ResourcePath.of(reader.nextString())
                else -> reader.skipValue()
            }
        }
        
        reader.endObject()
        
        return Override(
            predicate ?: throw IllegalArgumentException("Missing property 'predicate'"),
            model ?: throw IllegalArgumentException("Missing property 'model'")
        )
    }
    
    private fun readPredicate(reader: JsonReader): Map<String, Number> {
        val predicate = HashMap<String, Number>()
        
        reader.beginObject()
        while (reader.peek() == JsonToken.NAME) {
            predicate[reader.nextName()] = reader.nextDouble()
        }
        reader.endObject()
        
        return predicate
    }
    
    private fun readVector3d(reader: JsonReader): Vector3d {
        reader.beginArray()
        val vector = Vector3d(reader.nextDouble(), reader.nextDouble(), reader.nextDouble())
        reader.endArray()
        return vector
    }
    
    private fun readVector4d(reader: JsonReader): Vector4d {
        reader.beginArray()
        val vector = Vector4d(reader.nextDouble(), reader.nextDouble(), reader.nextDouble(), reader.nextDouble())
        reader.endArray()
        return vector
    }
    //</editor-fold>
    
}
