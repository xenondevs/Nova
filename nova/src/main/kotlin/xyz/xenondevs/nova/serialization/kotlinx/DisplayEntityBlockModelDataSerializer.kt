package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.util.bukkitBlockData
import xyz.xenondevs.nova.util.nmsBlockState
import xyz.xenondevs.nova.world.block.ColliderCube
import xyz.xenondevs.nova.world.block.HitboxCuboid
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelData

internal object DisplayEntityBlockModelDataSerializer : KSerializer<DisplayEntityBlockModelData> {
    
    private val modelsSerializer = ListSerializer(DisplayEntityBlockModelData.Model.serializer())
    private val collidersSerializer = ListSerializer(ColliderCube.serializer())
    private val hitboxesSerializer = ListSerializer(HitboxCuboid.serializer())
    
    override val descriptor = buildClassSerialDescriptor("xyz.xenondevs.nova.DisplayEntityBlockModelData") {
        element<Boolean>("waterlogged")
        element<List<DisplayEntityBlockModelData.Model>>("models")
        element("collider", BlockStateSerializer.descriptor)
        element<List<ColliderCube>>("extra_colliders", isOptional = true)
        element<List<HitboxCuboid>>("extra_hitboxes", isOptional = true)
    }
    
    override fun serialize(encoder: Encoder, value: DisplayEntityBlockModelData) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, Boolean.serializer(), value.waterlogged)
            encodeSerializableElement(descriptor, 1, modelsSerializer, value.models)
            encodeSerializableElement(descriptor, 2, BlockStateSerializer, value.collider.nmsBlockState)
            encodeSerializableElement(descriptor, 3, collidersSerializer, value.extraColliders)
            encodeSerializableElement(descriptor, 4, hitboxesSerializer, value.extraHitboxes)
        }
    }
    
    override fun deserialize(decoder: Decoder): DisplayEntityBlockModelData {
        return decoder.decodeStructure(descriptor) {
            var waterlogged: Boolean? = null
            var models: List<DisplayEntityBlockModelData.Model>? = null
            var collider: BlockState? = null
            var extraColliders = emptyList<ColliderCube>()
            var extraHitboxes: List<HitboxCuboid>? = null
            
            while (true) {
                when (decodeElementIndex(descriptor)) {
                    0 -> waterlogged = decodeSerializableElement(descriptor, 0, Boolean.serializer())
                    1 -> models = decodeSerializableElement(descriptor, 1, modelsSerializer)
                    2 -> collider = decodeSerializableElement(descriptor, 2, BlockStateSerializer)
                    3 -> extraColliders = decodeSerializableElement(descriptor, 3, collidersSerializer)
                    4 -> extraHitboxes = decodeSerializableElement(descriptor, 4, hitboxesSerializer)
                    else -> break
                }
            }
            
            DisplayEntityBlockModelData(
                waterlogged ?: throw SerializationException("Missing 'waterlogged' field"),
                models ?: throw SerializationException("Missing 'models' field"),
                collider?.let { provider(it.bukkitBlockData) } ?: throw SerializationException("Missing 'collider' field"),
                extraColliders,
                extraHitboxes ?: extraColliders.map(HitboxCuboid::fromCollider)
            )
        }
    }
    
}