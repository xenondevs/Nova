package xyz.xenondevs.nova.serialization.cbf.element.other

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.serialization.cbf.BackedElement
import xyz.xenondevs.nova.serialization.cbf.BinaryDeserializer
import xyz.xenondevs.nova.upgrade.UpgradeType
import xyz.xenondevs.nova.util.readString
import xyz.xenondevs.nova.util.writeString
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