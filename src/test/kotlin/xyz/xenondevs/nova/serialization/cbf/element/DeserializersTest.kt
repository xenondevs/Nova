package xyz.xenondevs.nova.serialization.cbf.element

import org.junit.jupiter.api.Test
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.element.primitive.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class DeserializersTest {
    
    @Test
    fun testEmptyDeserializer() {
        assertEquals(EmptyElement, EmptyDeserializer.read(byteArrayOf()))
    }
    
    @Test
    fun testBooleanDeserializer() {
        assertTrue(BooleanDeserializer.read(byteArrayOf(1)).value)
        assertFalse(BooleanDeserializer.read(byteArrayOf(0)).value)
    }
    
    @Test
    fun testByteDeserializer() {
        assertValueEquals(123, ByteDeserializer.read(byteArrayOf(123)))
        assertValueEquals(5, ByteDeserializer.read(byteArrayOf(5)))
        assertValueEquals(16, ByteDeserializer.read(byteArrayOf(16)))
    }
    
    @Test
    fun testIntDeserializer() {
        assertValueEquals(311035591, IntDeserializer.read(byteArrayOf(18, -118, 6, -57)))
        assertValueEquals(-492796405, IntDeserializer.read(byteArrayOf(-30, -96, -122, 11)))
        assertValueEquals(1876954595, IntDeserializer.read(byteArrayOf(111, -32, 13, -29)))
    }
    
    @Test
    fun testCharDeserializer() {
        assertValueEquals('a', CharDeserializer.read(byteArrayOf(0, 97)))
        assertValueEquals('♆', CharDeserializer.read(byteArrayOf(38, 70)))
        assertValueEquals('星', CharDeserializer.read(byteArrayOf(102, 31)))
    }
    
    @Test
    fun testFloatDeserializer() {
        assertValueEquals(0.8401922f, FloatDeserializer.read(byteArrayOf(63, 87, 22, -42)))
        assertValueEquals(0.6463885f, FloatDeserializer.read(byteArrayOf(63, 37, 121, -73)))
        assertValueEquals(1337.424242f, FloatDeserializer.read(byteArrayOf(68, -89, 45, -109)))
    }
    
    @Test
    fun testLongDeserializer() {
        assertValueEquals(-585275695679690238, LongDeserializer.read(byteArrayOf(-9, -32, -82, -37, 15, 87, -86, 2)))
        assertValueEquals(857773058568126104, LongDeserializer.read(byteArrayOf(11, -25, 108, 13, 85, -38, 126, -104)))
        assertValueEquals(8716459852843525593, LongDeserializer.read(byteArrayOf(120, -9, 22, 13, -21, -79, 37, -39)))
    }
    
    @Test
    fun testStringDeserializer() {
        assertValueEquals("test", StringDeserializer.read(byteArrayOf(0, 4, 116, 101, 115, 116)))
        assertValueEquals("123", StringDeserializer.read(byteArrayOf(0, 3, 49, 50, 51)))
        assertValueEquals("超新星", StringDeserializer.read(byteArrayOf(0, 9, -24, -74, -123, -26, -106, -80, -26, -104, -97)))
    }
    
    private inline fun <reified T> assertValueEquals(expected: T, actualElement: BackedElement<T>) =
        assertEquals(expected, actualElement.value)
    
}