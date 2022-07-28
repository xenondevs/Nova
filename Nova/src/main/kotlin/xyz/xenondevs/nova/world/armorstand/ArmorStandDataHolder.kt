package xyz.xenondevs.nova.world.armorstand

import io.netty.buffer.Unpooled
import net.minecraft.core.Rotations
import net.minecraft.network.FriendlyByteBuf
import java.util.*

private fun FriendlyByteBuf.writeMetadataSharedFlags(index: Int, byte: Int) {
    writeByte(index)
    writeVarInt(0)
    writeByte(byte)
}

private fun FriendlyByteBuf.writeMetadataRotation(index: Int, rotations: Rotations?) {
    if (rotations == null) return
    
    writeByte(index)
    writeVarInt(8)
    writeFloat(rotations.x)
    writeFloat(rotations.y)
    writeFloat(rotations.z)
}

private fun BitSet.toInt(): Int =
    toLongArray().firstOrNull()?.toInt() ?: 0

class ArmorStandDataHolder internal constructor(private val entityId: Int) {
    
    private val changedDataTypes = HashSet<ChangedDataType>()
    
    //<editor-fold desc="entity shared flags">
    var invisible = false
        set(value) {
            field = value
            changedDataTypes += ChangedDataType.ENTITY_SHARED_FLAGS
        }
    var glowing = false
        set(value) {
            field = value
            changedDataTypes += ChangedDataType.ENTITY_SHARED_FLAGS
        }
    var onFire = false
        set(value) {
            field = value
            changedDataTypes += ChangedDataType.ENTITY_SHARED_FLAGS
        }
    
    //</editor-fold>
    
    //<editor-fold desc="armor stand shared flags">
    var small = false
        set(value) {
            field = value
            changedDataTypes += ChangedDataType.ARMOR_STAND_SHARED_FLAGS
        }
    var hasArms = false
        set(value) {
            field = value
            changedDataTypes += ChangedDataType.ARMOR_STAND_SHARED_FLAGS
        }
    var hasBasePlate = false
        set(value) {
            field = value
            changedDataTypes += ChangedDataType.ARMOR_STAND_SHARED_FLAGS
        }
    var marker = false
        set(value) {
            field = value
            changedDataTypes += ChangedDataType.ARMOR_STAND_SHARED_FLAGS
        }
    //</editor-fold>
    
    //<editor-fold desc="rotation">
    var headRotation: Rotations? = null
        set(value) {
            field = value
            changedDataTypes += ChangedDataType.HEAD_ROT
        }
    var bodyRotation: Rotations? = null
        set(value) {
            field = value
            changedDataTypes += ChangedDataType.BODY_ROT
        }
    var leftArmRotation: Rotations? = null
        set(value) {
            field = value
            changedDataTypes += ChangedDataType.LEFT_ARM_ROT
        }
    var rightArmRotation: Rotations? = null
        set(value) {
            field = value
            changedDataTypes += ChangedDataType.RIGHT_ARM_ROT
        }
    var leftLegRotation: Rotations? = null
        set(value) {
            field = value
            changedDataTypes += ChangedDataType.LEFT_LEG_ROT
        }
    var rightLegRotation: Rotations? = null
        set(value) {
            field = value
            changedDataTypes += ChangedDataType.RIGHT_LEG_ROT
        }
    //</editor-fold>
    
    fun createPartialDataBuf(): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(0x50)
        buf.writeVarInt(entityId)
        
        changedDataTypes.forEach { it.writeFun(this, buf) }
        changedDataTypes.clear()
        buf.writeByte(0xFF)
        return buf
    }
    
    fun createCompleteDataBuf(): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(0x50)
        buf.writeVarInt(entityId)
        
        ChangedDataType.values().forEach { it.writeFun(this, buf) }
        buf.writeByte(0xFF)
        return buf
    }
    
    private enum class ChangedDataType(val writeFun: (ArmorStandDataHolder, FriendlyByteBuf) -> Unit) {
        ENTITY_SHARED_FLAGS({ holder, buf ->
            buf.writeMetadataSharedFlags(0,
                BitSet(8).apply {
                    set(0, holder.onFire)
                    set(5, holder.invisible)
                    set(6, holder.glowing)
                }.toInt()
            )
        }),
        ARMOR_STAND_SHARED_FLAGS({ holder, buf ->
            buf.writeMetadataSharedFlags(15,
                BitSet(8).apply {
                    set(0, holder.small)
                    set(2, holder.hasArms)
                    set(3, holder.hasBasePlate)
                    set(4, holder.marker)
                }.toInt()
            )
        }),
        HEAD_ROT({ holder, buf -> buf.writeMetadataRotation(16, holder.headRotation) }),
        BODY_ROT({ holder, buf -> buf.writeMetadataRotation(17, holder.bodyRotation) }),
        LEFT_ARM_ROT({ holder, buf -> buf.writeMetadataRotation(18, holder.leftArmRotation) }),
        RIGHT_ARM_ROT({ holder, buf -> buf.writeMetadataRotation(19, holder.rightArmRotation) }),
        LEFT_LEG_ROT({ holder, buf -> buf.writeMetadataRotation(20, holder.leftLegRotation) }),
        RIGHT_LEG_ROT({ holder, buf -> buf.writeMetadataRotation(21, holder.rightLegRotation) })
    }
    
}
