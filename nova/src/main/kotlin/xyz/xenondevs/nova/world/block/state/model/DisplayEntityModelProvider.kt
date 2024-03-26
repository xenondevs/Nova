package xyz.xenondevs.nova.world.block.state.model

import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Display.Brightness
import org.joml.Matrix4fc
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nova.util.item.requiresLight
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.fakeentity.FakeEntity
import xyz.xenondevs.nova.world.fakeentity.impl.FakeItemDisplay
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.ItemDisplayMetadata
import java.util.concurrent.ConcurrentHashMap

internal data class DisplayEntityBlockModelData(
    val models: List<Model>,
    val hitboxType: BlockData,
) {
    
    internal data class Model(val material: Material, val customModelData: Int, val transform: Matrix4fc) {
        
        @Transient
        val itemStack = ItemBuilder(material).setCustomModelData(customModelData).get()
        
    }
    
}

/**
 * A block model provider that uses display entities to display the block model.
 */
internal data object DisplayEntityBlockModelProvider : BlockModelProvider<DisplayEntityBlockModelData> {
    
    val entities = ConcurrentHashMap<BlockPos, List<FakeItemDisplay>>()
    
    override fun set(pos: BlockPos, info: DisplayEntityBlockModelData) {
        pos.block.blockData = info.hitboxType
        load(pos, info)
    }
    
    override fun load(pos: BlockPos, info: DisplayEntityBlockModelData) {
        createItemDisplays(pos, info)
    }
    
    override fun remove(pos: BlockPos) {
        entities.remove(pos)?.forEach(FakeEntity<*>::remove)
        pos.block.type = Material.AIR
    }
    
    override fun unload(pos: BlockPos) {
        entities.remove(pos)?.forEach(FakeEntity<*>::remove)
    }
    
    override fun replace(pos: BlockPos, prevInfo: DisplayEntityBlockModelData, newInfo: DisplayEntityBlockModelData) {
        if (prevInfo.hitboxType != newInfo.hitboxType)
            pos.block.blockData = newInfo.hitboxType
        
        // re-use as many existing entities as possible
        val prevEntities = entities[pos]!!
        entities[pos] = newInfo.models.mapIndexed { i, model ->
            prevEntities.getOrNull(i)
                ?.also { prevEntity -> prevEntity.updateEntityData(true) { setMetadata(this, newInfo, model) } }
                ?: FakeItemDisplay(pos.location.add(0.5, 0.5, 0.5)) { _, data -> setMetadata(data, newInfo, model) }
        }
    }
    
    private fun createItemDisplays(pos: BlockPos, info: DisplayEntityBlockModelData) {
        if (pos in entities.keys)
            throw IllegalStateException("ItemDisplay already exists at $pos")
        
        entities[pos] = info.models.map { model ->
            FakeItemDisplay(pos.location.add(0.5, 0.5, 0.5)) { _, data -> setMetadata(data, info, model) }
        }
    }
    
    private fun setMetadata(data: ItemDisplayMetadata, info: DisplayEntityBlockModelData, model: DisplayEntityBlockModelData.Model) {
        // TODO: proper light level
        if (info.hitboxType.material.requiresLight) {
            data.brightness = Brightness(15, 15)
        }
        
        data.itemStack = model.itemStack
        data.transform = model.transform
    }
    
}