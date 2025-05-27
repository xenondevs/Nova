package xyz.xenondevs.nova.world.block.state.model

import io.papermc.paper.datacomponent.DataComponentTypes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Fluid
import org.bukkit.Material
import org.bukkit.entity.Display.Brightness
import org.bukkit.inventory.ItemStack
import org.joml.Matrix4f
import org.joml.Matrix4fc
import xyz.xenondevs.nova.serialization.kotlinx.BlockStateSerializer
import xyz.xenondevs.nova.serialization.kotlinx.KeySerializer
import xyz.xenondevs.nova.serialization.kotlinx.Matrix4fcAsArraySerializer
import xyz.xenondevs.nova.util.item.requiresLight
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.setBlockState
import xyz.xenondevs.nova.util.setBlockStateNoUpdate
import xyz.xenondevs.nova.util.setBlockStateSilently
import xyz.xenondevs.nova.util.withoutBlockMigration
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.behavior.Waterloggable
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties
import xyz.xenondevs.nova.world.fakeentity.FakeEntity
import xyz.xenondevs.nova.world.fakeentity.impl.FakeItemDisplay
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.ItemDisplayMetadata
import xyz.xenondevs.nova.world.item.DefaultBlockOverlays
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

@Serializable
internal data class DisplayEntityBlockModelData(
    val blockState: NovaBlockState,
    val models: List<Model>,
    @Serializable(with = BlockStateSerializer::class)
    val hitboxType: BlockState
) {
    
    @Serializable
    internal data class Model(
        @Serializable(with = KeySerializer::class)
        val model: Key,
        @Serializable(with = Matrix4fcAsArraySerializer::class)
        val transform: Matrix4fc
    ) {
        
        val itemStack: ItemStack
            get() = ItemStack(Material.PAPER).apply {
                @Suppress("UnstableApiUsage")
                setData(DataComponentTypes.ITEM_MODEL, model)
            }
        
    }
    
}

/**
 * A block model provider that uses display entities to display the block model.
 */
@Serializable
@SerialName("entity_backed")
internal class DisplayEntityBlockModelProvider(val info: DisplayEntityBlockModelData) : BlockModelProvider {
    
    companion object {
        val entities = ConcurrentHashMap<BlockPos, List<FakeItemDisplay>>()
    }
    
    override fun set(pos: BlockPos, method: BlockUpdateMethod) {
        placeHitbox(pos, method)
        load(pos)
    }
    
    private fun placeHitbox(pos: BlockPos, method: BlockUpdateMethod) {
        withoutBlockMigration(pos) {
            when (method) {
                BlockUpdateMethod.DEFAULT -> pos.setBlockState(info.hitboxType)
                BlockUpdateMethod.NO_UPDATE -> pos.setBlockStateNoUpdate(info.hitboxType)
                BlockUpdateMethod.SILENT -> pos.setBlockStateSilently(info.hitboxType)
            }
        }
    }
    
    override fun load(pos: BlockPos) {
        if (pos in entities.keys)
            throw IllegalStateException("ItemDisplay already exists at $pos")
        
        val models = info.models.mapTo(ArrayList()) { model ->
            FakeItemDisplay(pos.location.toCenterLocation()) { _, data -> setMetadata(data, model) }
        }
        if (hasWaterlogEntity()) {
            models += FakeItemDisplay(pos.location.toCenterLocation()) { _, data -> setWaterlogMetadata(data, pos) }
        }
        
        entities[pos] = models
    }
    
    override fun remove(pos: BlockPos, method: BlockUpdateMethod) {
        super.remove(pos, method)
        entities.remove(pos)?.forEach(FakeEntity<*>::remove)
    }
    
    override fun unload(pos: BlockPos) {
        entities.remove(pos)?.forEach(FakeEntity<*>::remove)
    }
    
    override fun replace(pos: BlockPos, method: BlockUpdateMethod) {
        placeHitbox(pos, method)
        
        // re-use as many existing entities as possible
        val prevEntities = entities[pos] ?: emptyList()
        val newEntities = ArrayList<FakeItemDisplay>()
     
        var i = 0
        for (model in info.models) {
            newEntities += prevEntities.getOrNull(i++)
                ?.also { prevEntity -> prevEntity.updateEntityData(true) { setMetadata(this, model) } }
                ?: FakeItemDisplay(pos.location.toCenterLocation()) { _, data -> setMetadata(data, model) }
        }
        if (hasWaterlogEntity()) {
            newEntities += prevEntities.getOrNull(i)
                ?.also { prevEntity -> prevEntity.updateEntityData(true) { setWaterlogMetadata(this, pos) } }
                ?: FakeItemDisplay(pos.location.toCenterLocation()) { _, data -> setWaterlogMetadata(data, pos) }
        }
        
        entities[pos] = newEntities
    }
    
    fun updateWaterlogEntity(pos: BlockPos) {
        entities[pos]?.lastOrNull()?.updateEntityData(true) { setWaterlogMetadata(this, pos) }
    }
    
    private fun hasWaterlogEntity(): Boolean =
        info.blockState.block.hasBehavior<Waterloggable>() && info.blockState.getOrThrow(DefaultBlockStateProperties.WATERLOGGED)
    
    private fun setWaterlogMetadata(data: ItemDisplayMetadata, pos: BlockPos) {
        data.brightness = null
        data.itemStack = DefaultBlockOverlays.WATERLOGGED.createClientsideItemBuilder()
            .setCustomModelData(0, pos.world.getFluidData(pos.x, pos.y + 1, pos.z).fluidType == Fluid.WATER)
            .setCustomModelData(0, Color(pos.world.serverLevel.getBiome(pos.nmsPos).value().waterColor))
            .build()
        data.transform = Matrix4f()
    }
    
    private fun setMetadata(data: ItemDisplayMetadata, model: DisplayEntityBlockModelData.Model) {
        // TODO: proper light level
        if (info.hitboxType.bukkitMaterial.requiresLight) {
            data.brightness = Brightness(15, 15)
        }
        
        data.itemStack = model.itemStack
        data.transform = model.transform
    }
    
}