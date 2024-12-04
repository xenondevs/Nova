package xyz.xenondevs.nova.resources.builder.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.joml.Vector3d
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSerializationApi::class)
class ModelBuilderTest {
    
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    @Test
    fun testScaleCentered() {
        val model = deserializeModel("full_cube/model")
        
        val builder = ModelBuilder(model)
        builder.scale(0.5)
        val halfCentered = builder.buildScaled(null)
        
        assertEquals(deserializeModel("full_cube/scaled/half_centered"), halfCentered)
    }
    
    @Test
    fun testScaleUncentered() {
        val model = deserializeModel("full_cube/model")
        
        val builder = ModelBuilder(model)
        builder.scale(Vector3d(0.0, 0.0, 0.0), Vector3d(0.5, 0.5, 0.5))
        val halfUncentered = builder.buildScaled(null)
        
        assertEquals(deserializeModel("full_cube/scaled/half_uncentered"), halfUncentered)
    }
    
    @Test
    fun testScaleUV() {
        val rots = listOf(0, 90, 180, 270)
        for (rot in rots) {
            val model = deserializeModel("arrow_cube/$rot/model")
            
            val m1 = ModelBuilder(model)
                .scale(Vector3d(0.0, 0.0, 0.0), Vector3d(0.5, 1.0, 0.25), true)
                .buildScaled(null)
            assertEquals(deserializeModel("arrow_cube/$rot/scaled/pillar_0_0"), m1)

            val m2 = ModelBuilder(model)
                .scale(Vector3d(16.0, 0.0, 16.0), Vector3d(0.5, 1.0, 0.25), true)
                .buildScaled(null)
            assertEquals(deserializeModel("arrow_cube/$rot/scaled/pillar_16_16"), m2)

            val m3 = ModelBuilder(model)
                .scale(Vector3d(8.0, 8.0, 8.0), Vector3d(0.5, 0.5, 0.5), true)
                .buildScaled(null)
            assertEquals(deserializeModel("arrow_cube/$rot/scaled/cube_centered"), m3)
        }
    }
    
    @Test
    fun testTranslate() {
        val model = deserializeModel("full_cube/model")
        
        val builder = ModelBuilder(model)
        builder.translate(Vector3d(16.0, 16.0, 16.0))
        val translated = builder.buildScaled(null)
        
        assertEquals(deserializeModel("full_cube/translated/16_16_16"), translated)
    }
    
    @Test
    fun testRotateX() {
        assertRotatedEquals("half_cube", Model.Axis.X, 22.5)
        assertRotatedEquals("half_cube", Model.Axis.X, 45.0)
        assertRotatedEquals("half_cube", Model.Axis.X, 67.5)
        assertRotatedEquals("half_cube", Model.Axis.X, 90.0)
        assertRotatedEquals("half_cube", Model.Axis.X, 112.5)
        assertRotatedEquals("half_cube", Model.Axis.X, 135.0)
        assertRotatedEquals("half_cube", Model.Axis.X, 157.5)
        assertRotatedEquals("half_cube", Model.Axis.X, 180.0)
        assertRotatedEquals("half_cube", Model.Axis.X, 202.5)
        assertRotatedEquals("half_cube", Model.Axis.X, 225.0)
        assertRotatedEquals("half_cube", Model.Axis.X, 247.5)
        assertRotatedEquals("half_cube", Model.Axis.X, 270.0)
        assertRotatedEquals("half_cube", Model.Axis.X, 292.5)
        assertRotatedEquals("half_cube", Model.Axis.X, 315.0)
        assertRotatedEquals("half_cube", Model.Axis.X, 337.5)
    }
    
    @Test
    fun testRotateY() {
        assertRotatedEquals("half_cube", Model.Axis.Y, 22.5)
        assertRotatedEquals("half_cube", Model.Axis.Y, 45.0)
        assertRotatedEquals("half_cube", Model.Axis.Y, 67.5)
        assertRotatedEquals("half_cube", Model.Axis.Y, 90.0)
        assertRotatedEquals("half_cube", Model.Axis.Y, 112.5)
        assertRotatedEquals("half_cube", Model.Axis.Y, 135.0)
        assertRotatedEquals("half_cube", Model.Axis.Y, 157.5)
        assertRotatedEquals("half_cube", Model.Axis.Y, 180.0)
        assertRotatedEquals("half_cube", Model.Axis.Y, 202.5)
        assertRotatedEquals("half_cube", Model.Axis.Y, 225.0)
        assertRotatedEquals("half_cube", Model.Axis.Y, 247.5)
        assertRotatedEquals("half_cube", Model.Axis.Y, 270.0)
        assertRotatedEquals("half_cube", Model.Axis.Y, 292.5)
        assertRotatedEquals("half_cube", Model.Axis.Y, 315.0)
        assertRotatedEquals("half_cube", Model.Axis.Y, 337.5)
    }
    
    @Test
    fun testRotateZ() {
        assertRotatedEquals("half_cube", Model.Axis.Z, 22.5)
        assertRotatedEquals("half_cube", Model.Axis.Z, 45.0)
        assertRotatedEquals("half_cube", Model.Axis.Z, 67.5)
        assertRotatedEquals("half_cube", Model.Axis.Z, 90.0)
        assertRotatedEquals("half_cube", Model.Axis.Z, 112.5)
        assertRotatedEquals("half_cube", Model.Axis.Z, 135.0)
        assertRotatedEquals("half_cube", Model.Axis.Z, 157.5)
        assertRotatedEquals("half_cube", Model.Axis.Z, 180.0)
        assertRotatedEquals("half_cube", Model.Axis.Z, 202.5)
        assertRotatedEquals("half_cube", Model.Axis.Z, 225.0)
        assertRotatedEquals("half_cube", Model.Axis.Z, 247.5)
        assertRotatedEquals("half_cube", Model.Axis.Z, 270.0)
        assertRotatedEquals("half_cube", Model.Axis.Z, 292.5)
        assertRotatedEquals("half_cube", Model.Axis.Z, 315.0)
        assertRotatedEquals("half_cube", Model.Axis.Z, 337.5)
    }
    
    @Test
    fun testRotateIdentityX() {
        val base = deserializeModel("half_cube/model")
        val builder = ModelBuilder(base)
        builder.rotateX(22.5)
        builder.rotateX(-382.5)
        assertEquals(base, builder.buildScaled(null))
    }
    
    @Test
    fun testRotateIdentityY() {
        val base = deserializeModel("half_cube/model")
        val builder = ModelBuilder(base)
        builder.rotateY(22.5)
        builder.rotateY(-382.5)
        assertEquals(base, builder.buildScaled(null))
    }
    
    @Test
    fun testRotateIdentityZ() {
        val base = deserializeModel("half_cube/model")
        val builder = ModelBuilder(base)
        builder.rotateZ(22.5)
        builder.rotateZ(-382.5)
        assertEquals(base, builder.buildScaled(null))
    }
    
    @Test
    fun testRotateMulti() {
        val base = deserializeModel("half_cube/model")
        val builder = ModelBuilder(base)
        builder.rotateY(22.5)
        builder.rotateX(180.0)
        builder.rotateY(67.5)
        builder.rotateZ(67.5)
        assertEquals(deserializeModel("half_cube/rotated/y22.5_x180.0_y67.5_z67.5"), builder.buildScaled(null))
    }
    
    @Test
    fun testRescaleOversizedPositive() {
        val base = deserializeModel("oversized/positive_y")
        val actual = ModelBuilder(base).buildScaled(null)
        
        val expected = deserializeModel("oversized/positive_y_resized")
        assertEquals(ScaledModel(expected, 0.6), actual)
    }
    
    @Test
    fun testRescaleOversizedNegative() {
        val base = deserializeModel("oversized/negative_y")
        val actual = ModelBuilder(base).buildScaled(null)
        
        val expected = deserializeModel("oversized/negative_y_resized")
        assertEquals(ScaledModel(expected, 0.6), actual)
    }
    
    private fun assertRotatedEquals(model: String, axis: Model.Axis, rotation: Double) {
        val builder = ModelBuilder(deserializeModel("$model/model"))
        builder.rotate(axis, rotation)
        assertEquals(deserializeModel("$model/rotated/${axis.name.lowercase()}/$rotation"), builder.buildScaled(null))
    }
    
    private fun deserializeModel(name: String): Model =
        javaClass.getResourceAsStream("/models/$name.json")?.use { json.decodeFromStream(it)!! }
            ?: throw IllegalArgumentException("Model $name not found")
    
    private fun serializeModel(model: Model): String =
        json.encodeToString(model)
    
    private fun assertEquals(expected: Model, actual: ScaledModel) {
        if (expected != actual.model) {
            throw AssertionError("Models are not equal\nExpected: ${serializeModel(expected)}\nActual: ${serializeModel(actual.model)}")
        } else if (actual.scale != 1.0) {
            throw AssertionError("Model builder produced non-identity scale: ${actual.scale}")
        }
    }
    
    private fun assertEquals(expected: ScaledModel, actual: ScaledModel) {
        if (expected.model != actual.model) {
            throw AssertionError("Models are not equal\nExpected: ${serializeModel(expected.model)}\nActual: ${serializeModel(actual.model)}")
        }
    }
    
}