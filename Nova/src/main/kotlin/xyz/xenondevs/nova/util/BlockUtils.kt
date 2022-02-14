package xyz.xenondevs.nova.util

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.ShulkerBox
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.Levelled
import org.bukkit.block.data.type.Bed
import org.bukkit.block.data.type.PistonHead
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.particle.ParticleEffect
import kotlin.random.Random

val Block.below: Block
    get() = location.subtract(0.0, 1.0, 0.0).block

fun Block.hasSameTypeBelow(): Boolean {
    return type == below.type
}

fun Block.remove(playEffects: Boolean = true) {
    val customRemoved = CustomItemServiceManager.removeBlock(this, playEffects)
    if (customRemoved) return
    
    val tileEntity = TileEntityManager.getTileEntityAt(location)
    if (tileEntity != null) {
        TileEntityManager.removeTileEntity(tileEntity)
    }
    
    if (playEffects) playBreakEffects()
    getMainHalf().type = Material.AIR
}

fun Block.getAllDrops(tool: ItemStack? = null): List<ItemStack> {
    CustomItemServiceManager.getDrops(this, tool)?.let { return it }
    
    val tileEntity = TileEntityManager.getTileEntityAt(location)
    if (tileEntity != null) {
        return tileEntity.getDrops(true)
    }
    
    val drops = ArrayList<ItemStack>()
    val state = state
    if (state is Chest) {
        drops += state.blockInventory.contents.filterNotNull()
        state.blockInventory.clear()
    } else if (state is Container && state !is ShulkerBox) {
        drops += state.inventory.contents.filterNotNull()
        state.inventory.clear()
    }
    
    val block = getMainHalf()
    drops += block.getDrops(tool)
    
    return drops.filterNot { it.type.isAir }
}

private fun Block.getMainHalf(): Block {
    val data = blockData
    if (data is Bisected) {
        if (data.half == Bisected.Half.TOP) {
            return location.subtract(0.0, 1.0, 0.0).block
        }
    } else if (data is Bed) {
        if (data.part == Bed.Part.FOOT) {
            return location.advance(data.facing).block
        }
    } else if (data is PistonHead) {
        return location.advance(data.facing.oppositeFace).block
    }
    
    return this
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

fun Block.place(player: ServerPlayer, itemStack: ItemStack): Boolean {
    val location = location
    val nmsStack = itemStack.nmsStack
    val blockItem = nmsStack.item as BlockItem
    val result = blockItem.place(BlockPlaceContext(UseOnContext(
        world.serverLevel,
        player,
        InteractionHand.MAIN_HAND,
        nmsStack,
        BlockHitResult(
            Vec3(location.x, location.y, location.z),
            Direction.UP,
            BlockPos(location.blockX, location.blockY, location.blockZ),
            false
        )
    )))
    
    if (result.consumesAction()) {
        setBlockEntityDataFromItemStack(itemStack)
        return true
    }
    return false
}

fun Block.setBlockEntityDataFromItemStack(itemStack: ItemStack) {
    val itemTag = CompoundTag()
    ReflectionRegistry.CB_CRAFT_META_APPLY_TO_METHOD.invoke(itemStack.itemMeta, itemTag)
    
    val tileEntityTag = itemTag.getCompound("BlockEntityTag")?.let { if (it.isEmpty) itemTag else it }
    if (tileEntityTag != null) {
        val world = this.world.serverLevel
        world.getBlockEntity(BlockPos(x, y, z), true)?.load(tileEntityTag)
    }
}

fun Location.getBlockName(): String {
    val tileEntity = TileEntityManager.getTileEntityAt(this)
    return if (tileEntity != null) "nova:" + tileEntity.material.id.lowercase()
    else "minecraft:" + block.type.name.lowercase()
}

fun Block.isSourceFluid(): Boolean {
    return blockData is Levelled && (blockData as Levelled).level == 0
}

val Block.sourceFluidType: FluidType?
    get() {
        val blockData = blockData
        if (blockData is Levelled && blockData.level == 0) {
            return when (type) {
                Material.WATER, Material.BUBBLE_COLUMN -> FluidType.WATER
                Material.LAVA -> FluidType.LAVA
                else -> null
            }
        }
        
        return null
    }
