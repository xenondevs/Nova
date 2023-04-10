package xyz.xenondevs.nova.world.block.model

import net.minecraft.util.Brightness
import net.minecraft.world.item.ItemDisplayContext
import org.bukkit.Material
import org.joml.Quaternionf
import xyz.xenondevs.nova.data.resources.model.data.DisplayEntityBlockModelData
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.tileentity.requiresLight
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.world.fakeentity.impl.FakeItemDisplay
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.ItemDisplayMetadata

private val ROTATIONS: Array<Quaternionf?> = arrayOf(
    // NORTH
    null,
    // EAST
    Quaternionf().setAngleAxis((Math.PI * 1.5).toFloat(), 0f, 1f, 0f),
    // SOUTH
    Quaternionf().setAngleAxis(Math.PI.toFloat(), 0f, 1f, 0f),
    // WEST
    Quaternionf().setAngleAxis((Math.PI / 2).toFloat(), 0f, 1f, 0f),
    // UP
    Quaternionf().setAngleAxis((Math.PI / 2).toFloat(), 1f, 0f, 0f),
    // DOWN
    Quaternionf().setAngleAxis((Math.PI * 1.5).toFloat(), 1f, 0f, 0f)
)

class DisplayEntityModelProvider(blockState: NovaBlockState) : BlockModelProvider {
    
    private val pos = blockState.pos
    private val material = blockState.block
    private val modelData = material.model as DisplayEntityBlockModelData
    
    private val entities = ArrayList<FakeItemDisplay>()
    private val multiBlockPositions = material.multiBlockLoader?.invoke(pos)
    
    override var currentSubId = 0
        private set
    
    init {
        val directional = blockState.getProperty(Directional::class)
        val location = pos.location.add(.5, .5, .5)
        
        entities += FakeItemDisplay(location, false) { _, data -> setDataValues(data, directional) }
        
        multiBlockPositions?.forEachIndexed { i, otherPos ->
            entities += FakeItemDisplay(
                otherPos.location.apply { 
                    add(.5, .5, .5)
                    yaw = location.yaw
                },
                false
            ) { _, data -> setDataValues(data, directional, i + 1) }
        }
    }
    
    private fun setDataValues(data: ItemDisplayMetadata, directional: Directional?, subId: Int = 0) {
        // TODO: proper light level
        if (modelData.hitboxType.requiresLight)
            data.brightness = Brightness(15, 15)
        
        data.itemDisplay = ItemDisplayContext.HEAD
        data.itemStack = modelData[subId].get().nmsCopy
        
        val rotation = directional?.facing?.let { ROTATIONS.getOrNull(it.ordinal) }
        if (rotation != null) {
            data.leftRotation = rotation
        }
    }
    
    override fun load(placed: Boolean) {
        if (placed) {
            // cannot be moved out of this if as it would break blocks that change their hitbox type such as cables
            pos.block.type = modelData.hitboxType
            multiBlockPositions?.forEach { it.block.type = modelData.hitboxType }
        }
        entities.forEach(FakeItemDisplay::register)
    }
    
    override fun remove(broken: Boolean) {
        entities.forEach(FakeItemDisplay::remove)
        if (broken) {
            pos.block.type = Material.AIR
            multiBlockPositions?.forEach { it.block.type = Material.AIR }
        }
    }
    
    override fun update(subId: Int) {
        currentSubId = subId
        entities[0].updateEntityData(true) {
            itemStack = modelData[subId].get().nmsCopy
        }
    }
    
    companion object : BlockModelProviderType<DisplayEntityModelProvider> {
        override fun create(blockState: NovaBlockState) = DisplayEntityModelProvider(blockState)
    }
    
}