package xyz.xenondevs.nova.data.serialization.cbf.element.other

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.util.data.readString
import xyz.xenondevs.nova.util.data.writeString
import java.util.*

class UpgradesElement(override val value: EnumMap<UpgradeType, Int>) : BackedElement<EnumMap<UpgradeType, Int>> {
    override fun getTypeId() = 25
    
    override fun write(buf: ByteBuf) {
        buf.writeShort(value.size)
        if (value.isNotEmpty()) {
            value.forEach { (key, value) ->
                buf.writeString(key.name)
                buf.writeByte(value)
            }
        }
    }
}

object UpgradesDeserializer : BinaryDeserializer<UpgradesElement> {
    override fun read(buf: ByteBuf): UpgradesElement {
        val size = buf.readUnsignedShort()
        val map = EnumMap<UpgradeType, Int>(UpgradeType::class.java)
        repeat(size) {
            map[enumValueOf(buf.readString())] = buf.readUnsignedByte().toInt()
        }
        return UpgradesElement(map)
    }
}