package xyz.xenondevs.nova.world.item.legacy

import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.Test
import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.cbf.io.byteWriter
import xyz.xenondevs.nova.util.data.getByteArrayOrNull
import xyz.xenondevs.nova.util.data.getCompoundOrNull
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ItemStackLegacyConversionTest {
    
    @Test
    fun testNamespacedCompoundToPersistentDataContainer() {
        val customData = CompoundTag().apply {
            putByteArray("nova_cbf", legacyNamespacedCompound(
                "nova" to mapOf("int" to Cbf.write(1)),
                "my_addon" to mapOf("string" to Cbf.write("abc"))
            ))
            put("PublicBukkitValues", CompoundTag().apply {
                putByteArray("unrelated:key", Cbf.write(true))
                putByteArray("nova:int", Cbf.write(0))
            })
        }
        
        assertTrue(ItemStackNamespacedCompoundConverter.convert(customData))
        assertFalse(customData.contains("nova_cbf"))
        
        val persistentData = customData.getCompoundOrNull("PublicBukkitValues")!!
        assertContentEquals(Cbf.write(1), persistentData.getByteArrayOrNull("nova:int"))
        assertContentEquals(Cbf.write("abc"), persistentData.getByteArrayOrNull("my_addon:string"))
        assertContentEquals(Cbf.write(true), persistentData.getByteArrayOrNull("unrelated:key"))
    }
    
    @Test
    fun testNoLegacyData() {
        assertFalse(ItemStackNamespacedCompoundConverter.convert(CompoundTag()))
    }
    
    private fun legacyNamespacedCompound(vararg namespaces: Pair<String, Map<String, ByteArray>>): ByteArray =
        byteWriter {
            writeUnsignedByte(1.toUByte())
            writeVarInt(namespaces.size)
            for ([namespace, entries] in namespaces) {
                writeString(namespace)
                writeUnsignedByte(2.toUByte())
                writeVarInt(entries.size)
                for ([key, value] in entries) {
                    writeString(key)
                    writeVarInt(value.size)
                    writeBytes(value)
                }
            }
        }
    
}
