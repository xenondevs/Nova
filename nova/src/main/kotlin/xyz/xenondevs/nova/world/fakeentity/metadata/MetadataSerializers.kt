package xyz.xenondevs.nova.world.fakeentity.metadata

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.core.Registry
import net.minecraft.core.Rotations
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.animal.CatVariant
import net.minecraft.world.entity.animal.FrogVariant
import net.minecraft.world.entity.decoration.PaintingVariant
import net.minecraft.world.entity.npc.VillagerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.material.PacketItems
import java.util.*

internal interface MetadataSerializer<T> {
    
    fun write(value: T, buf: FriendlyByteBuf)
    
}

internal object MetadataSerializers {
    
    val BYTE: MetadataSerializer<Int> = metadataSerializer(0, FriendlyByteBuf::writeByte)
    val VAR_INT: MetadataSerializer<Int> = metadataSerializer(1, FriendlyByteBuf::writeVarInt)
    val FLOAT: MetadataSerializer<Float> = metadataSerializer(2, FriendlyByteBuf::writeFloat)
    val STRING: MetadataSerializer<String> = metadataSerializer(3, FriendlyByteBuf::writeUtf)
    val COMPONENT: MetadataSerializer<Component> = metadataSerializer(4, FriendlyByteBuf::writeComponent)
    val OPT_COMPONENT: MetadataSerializer<Component?> = optionalMetadataSerializer(5, FriendlyByteBuf::writeComponent)
    val ITEM_STACK: MetadataSerializer<ItemStack> = metadataSerializer(6) { value, buf ->
        buf.writeItem(if (PacketItems.isNovaItem(value)) PacketItems.getFakeItem(null, value) else value)
    }
    val BOOLEAN: MetadataSerializer<Boolean> = metadataSerializer(7, FriendlyByteBuf::writeBoolean)
    val ROTATIONS: MetadataSerializer<Rotations> = metadataSerializer(8) { value, buf ->
        buf.writeFloat(value.x)
        buf.writeFloat(value.y)
        buf.writeFloat(value.z)
    }
    val BLOCK_POS: MetadataSerializer<BlockPos> = metadataSerializer(9, FriendlyByteBuf::writeBlockPos)
    val OPT_BLOCK_POS: MetadataSerializer<BlockPos?> = optionalMetadataSerializer(10, FriendlyByteBuf::writeBlockPos)
    val DIRECTION: MetadataSerializer<Direction> = metadataSerializer(11, FriendlyByteBuf::writeEnum)
    val OPT_UUID: MetadataSerializer<UUID?> = optionalMetadataSerializer(12, FriendlyByteBuf::writeUUID)
    val OPT_BLOCK_STATE: MetadataSerializer<BlockState?> = optionalMetadataSerializer(13) { value, buf -> buf.writeVarInt(Block.getId(value)) }
    val COMPOUND_TAG: MetadataSerializer<CompoundTag> = metadataSerializer(14, FriendlyByteBuf::writeNbt)
    val PARTICLE_OPTIONS: MetadataSerializer<ParticleOptions> = metadataSerializer(15) { value, buf ->
        buf.writeId(Registry.PARTICLE_TYPE, value.type)
        value.writeToNetwork(buf)
    }
    val VILLAGER_DATA: MetadataSerializer<VillagerData> = metadataSerializer(16) { value, buf ->
        buf.writeId(Registry.VILLAGER_TYPE, value.type)
        buf.writeId(Registry.VILLAGER_PROFESSION, value.profession)
        buf.writeVarInt(value.level)
    }
    val OPT_UNSIGNED_INT: MetadataSerializer<Int?> = metadataSerializer(17) { value, buf -> buf.writeVarInt((value ?: -1) + 1) }
    val POSE: MetadataSerializer<Pose> = metadataSerializer(18, FriendlyByteBuf::writeEnum)
    val CAT_VARIANT: MetadataSerializer<CatVariant> = metadataSerializer(19) { value, buf -> buf.writeId(Registry.CAT_VARIANT, value) }
    val FROG_VARIANT: MetadataSerializer<FrogVariant> = metadataSerializer(20) { value, buf -> buf.writeId(Registry.FROG_VARIANT, value) }
    val OPT_GLOBAL_POS: MetadataSerializer<GlobalPos?> = optionalMetadataSerializer(21, FriendlyByteBuf::writeGlobalPos)
    val PAINTING_VARIANT: MetadataSerializer<PaintingVariant> = optionalMetadataSerializer(22) { value, buf -> buf.writeId(Registry.PAINTING_VARIANT, value) }
    
    @JvmName("optionalMetadataSerializer1")
    private inline fun <T : Any?> optionalMetadataSerializer(typeId: Int, crossinline write: FriendlyByteBuf.(T & Any) -> Unit): MetadataSerializer<T> =
        optionalMetadataSerializer(typeId) { value, buf -> write.invoke(buf, value) }
    
    private inline fun <T : Any?> optionalMetadataSerializer(typeId: Int, crossinline write: (T & Any, FriendlyByteBuf) -> Unit): MetadataSerializer<T> =
        object : MetadataSerializer<T> {
            override fun write(value: T, buf: FriendlyByteBuf) {
                buf.writeVarInt(typeId)
                if (value != null) {
                    buf.writeBoolean(true)
                    write(value, buf)
                } else buf.writeBoolean(false)
            }
        }
    
    @JvmName("metadataSerializer1")
    private inline fun <T> metadataSerializer(typeId: Int, crossinline write: FriendlyByteBuf.(T) -> Unit): MetadataSerializer<T> =
        metadataSerializer(typeId) { value, buf -> write.invoke(buf, value) }
    
    private inline fun <T> metadataSerializer(typeId: Int, crossinline write: (T, FriendlyByteBuf) -> Unit): MetadataSerializer<T> =
        object : MetadataSerializer<T> {
            override fun write(value: T, buf: FriendlyByteBuf) {
                buf.writeVarInt(typeId)
                write(value, buf)
            }
        }
    
}