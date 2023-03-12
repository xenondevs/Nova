package xyz.xenondevs.nova.world.fakeentity.metadata

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.core.Rotations
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.EntityDataSerializers
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
    
    val BYTE: MetadataSerializer<Int> = metadataSerializer(EntityDataSerializers.BYTE, FriendlyByteBuf::writeByte)
    val VAR_INT: MetadataSerializer<Int> = metadataSerializer(EntityDataSerializers.INT, FriendlyByteBuf::writeVarInt)
    val FLOAT: MetadataSerializer<Float> = metadataSerializer(EntityDataSerializers.FLOAT, FriendlyByteBuf::writeFloat)
    val STRING: MetadataSerializer<String> = metadataSerializer(EntityDataSerializers.STRING, FriendlyByteBuf::writeUtf)
    val COMPONENT: MetadataSerializer<Component> = metadataSerializer(EntityDataSerializers.COMPONENT, FriendlyByteBuf::writeComponent)
    val OPT_COMPONENT: MetadataSerializer<Component?> = optionalMetadataSerializer(EntityDataSerializers.OPTIONAL_COMPONENT, FriendlyByteBuf::writeComponent)
    val ITEM_STACK: MetadataSerializer<ItemStack> = metadataSerializer(EntityDataSerializers.ITEM_STACK) { value, buf ->
        buf.writeItem(PacketItems.getClientSideStack(null, value))
    }
    val BOOLEAN: MetadataSerializer<Boolean> = metadataSerializer(EntityDataSerializers.BOOLEAN, FriendlyByteBuf::writeBoolean)
    val ROTATIONS: MetadataSerializer<Rotations> = metadataSerializer(EntityDataSerializers.ROTATIONS) { value, buf ->
        buf.writeFloat(value.x)
        buf.writeFloat(value.y)
        buf.writeFloat(value.z)
    }
    val BLOCK_POS: MetadataSerializer<BlockPos> = metadataSerializer(EntityDataSerializers.BLOCK_POS, FriendlyByteBuf::writeBlockPos)
    val OPT_BLOCK_POS: MetadataSerializer<BlockPos?> = optionalMetadataSerializer(EntityDataSerializers.OPTIONAL_BLOCK_POS, FriendlyByteBuf::writeBlockPos)
    val DIRECTION: MetadataSerializer<Direction> = metadataSerializer(EntityDataSerializers.DIRECTION, FriendlyByteBuf::writeEnum)
    val OPT_UUID: MetadataSerializer<UUID?> = optionalMetadataSerializer(EntityDataSerializers.OPTIONAL_UUID, FriendlyByteBuf::writeUUID)
    val OPT_BLOCK_STATE: MetadataSerializer<BlockState?> = optionalMetadataSerializer(EntityDataSerializers.BLOCK_STATE) { value, buf -> buf.writeVarInt(Block.getId(value)) }
    val COMPOUND_TAG: MetadataSerializer<CompoundTag> = metadataSerializer(EntityDataSerializers.COMPOUND_TAG, FriendlyByteBuf::writeNbt)
    val PARTICLE_OPTIONS: MetadataSerializer<ParticleOptions> = metadataSerializer(EntityDataSerializers.PARTICLE) { value, buf ->
        buf.writeId(BuiltInRegistries.PARTICLE_TYPE, value.type)
        value.writeToNetwork(buf)
    }
    val VILLAGER_DATA: MetadataSerializer<VillagerData> = metadataSerializer(EntityDataSerializers.VILLAGER_DATA) { value, buf ->
        buf.writeId(BuiltInRegistries.VILLAGER_TYPE, value.type)
        buf.writeId(BuiltInRegistries.VILLAGER_PROFESSION, value.profession)
        buf.writeVarInt(value.level)
    }
    val OPT_UNSIGNED_INT: MetadataSerializer<Int?> = metadataSerializer(EntityDataSerializers.OPTIONAL_UNSIGNED_INT) { value, buf -> buf.writeVarInt((value ?: -1) + 1) }
    val POSE: MetadataSerializer<Pose> = metadataSerializer(EntityDataSerializers.POSE, FriendlyByteBuf::writeEnum)
    val CAT_VARIANT: MetadataSerializer<CatVariant> = metadataSerializer(EntityDataSerializers.CAT_VARIANT) { value, buf -> buf.writeId(BuiltInRegistries.CAT_VARIANT, value) }
    val FROG_VARIANT: MetadataSerializer<FrogVariant> = metadataSerializer(EntityDataSerializers.FROG_VARIANT) { value, buf -> buf.writeId(BuiltInRegistries.FROG_VARIANT, value) }
    val OPT_GLOBAL_POS: MetadataSerializer<GlobalPos?> = optionalMetadataSerializer(EntityDataSerializers.OPTIONAL_GLOBAL_POS, FriendlyByteBuf::writeGlobalPos)
    val PAINTING_VARIANT: MetadataSerializer<PaintingVariant> = optionalMetadataSerializer(EntityDataSerializers.PAINTING_VARIANT) { value, buf -> buf.writeId(BuiltInRegistries.PAINTING_VARIANT, value) }
    
    @JvmName("optionalMetadataSerializer1")
    private inline fun <T : Any?> optionalMetadataSerializer(type: EntityDataSerializer<*>, crossinline write: FriendlyByteBuf.(T & Any) -> Unit): MetadataSerializer<T> =
        optionalMetadataSerializer(type) { value, buf -> write.invoke(buf, value) }
    
    private inline fun <T : Any?> optionalMetadataSerializer(type: EntityDataSerializer<*>, crossinline write: (T & Any, FriendlyByteBuf) -> Unit): MetadataSerializer<T> {
        val typeId = EntityDataSerializers.getSerializedId(type)
        return object : MetadataSerializer<T> {
            override fun write(value: T, buf: FriendlyByteBuf) {
                buf.writeVarInt(typeId)
                if (value != null) {
                    buf.writeBoolean(true)
                    write(value, buf)
                } else buf.writeBoolean(false)
            }
        }
    }
    
    @JvmName("metadataSerializer1")
    private inline fun <T> metadataSerializer(type: EntityDataSerializer<*>, crossinline write: FriendlyByteBuf.(T) -> Unit): MetadataSerializer<T> =
        metadataSerializer(type) { value, buf -> write.invoke(buf, value) }
    
    private inline fun <T> metadataSerializer(type: EntityDataSerializer<*>, crossinline write: (T, FriendlyByteBuf) -> Unit): MetadataSerializer<T> {
        val typeId = EntityDataSerializers.getSerializedId(type)
        return object : MetadataSerializer<T> {
            override fun write(value: T, buf: FriendlyByteBuf) {
                buf.writeVarInt(typeId)
                write(value, buf)
            }
        }
    }
    
}