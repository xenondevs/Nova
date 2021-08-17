package xyz.xenondevs.nova.util

import dev.lone.itemsadder.api.CustomBlock
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.integration.other.ItemsAdder
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.particle.ParticleEffect
import kotlin.random.Random

val Block.below: Block
    get() = location.subtract(0.0, 1.0, 0.0).block

fun Block.hasSameTypeBelow(): Boolean {
    return type == below.type
}

fun Block.breakAndTakeDrops(tool: ItemStack? = null, playEffects: Boolean = true): List<ItemStack> {
    if (playEffects) playBreakEffects()
    
    val tileEntity = TileEntityManager.getTileEntityAt(location)
    if (tileEntity != null) {
        return TileEntityManager.destroyTileEntity(tileEntity, true)
    }
    
    if (ItemsAdder.isInstalled()) {
        val customBlock = CustomBlock.byAlreadyPlaced(this)
        if (customBlock != null)
            return ItemsAdder.breakCustomBlock(customBlock)
    }
    
    val drops = this.getDrops(tool).toMutableList()
    val state = state
    if (state is Chest) {
        drops += state.blockInventory.contents.filterNotNull()
        state.blockInventory.clear()
    } else if (state is Container && state !is ShulkerBox) {
        drops += state.inventory.contents.filterNotNull()
        state.inventory.clear()
    }
    
    type = Material.AIR
    
    return drops
}

fun Block.playBreakEffects() {
    showBreakParticles()
    playBreakSound()
}

fun Block.showBreakParticles() {
    particleBuilder(ParticleEffect.BLOCK_CRACK, location.add(0.5, 0.5, 0.5)) {
        texture(type)
        offset(0.2, 0.2, 0.2)
        amount(50)
    }.display()
}

fun Block.playBreakSound() {
    val breakSound = blockData.soundGroup.breakSound
    world.playSound(location, breakSound, 1f, Random.nextDouble(0.8, 0.95).toFloat())
}

fun Block.setBreakState(entityId: Int, state: Int) {
    val packet = ClientboundBlockDestructionPacket(entityId, location.blockPos, state)
    
    chunk.getSurroundingChunks(1, true)
        .flatMap { it.entities.toList() }
        .filterIsInstance<Player>()
        .forEach { it.send(packet) }
}

fun Block.place(itemStack: ItemStack) {
    type = itemStack.type
    
    if (state is TileState || type == Material.PLAYER_HEAD || type == Material.PLAYER_WALL_HEAD)
        setBlockEntityDataFromItemStack(itemStack)
}

fun Block.setBlockEntityDataFromItemStack(itemStack: ItemStack) {
    val itemTag = CompoundTag()
    ReflectionRegistry.CB_CRAFT_META_APPLY_TO_METHOD.invoke(itemStack.itemMeta, itemTag)
    
    val tileEntityTag = itemTag.getCompound("BlockEntityTag")?.let { if (it.isEmpty) itemTag else it }
    if (tileEntityTag != null) {
        val world = this.world.serverLevel
        world.getTileEntity(BlockPos(x, y, z), true)?.load(tileEntityTag)
    }
}

fun Location.getBlockName(): String {
    val tileEntity = TileEntityManager.getTileEntityAt(this)
    return if (tileEntity != null) "nova:" + tileEntity.material.typeName.lowercase()
    else "minecraft:" + block.type.name.lowercase()
}
