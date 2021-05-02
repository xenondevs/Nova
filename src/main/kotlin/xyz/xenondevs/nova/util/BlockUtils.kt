package xyz.xenondevs.nova.util

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.particle.ParticleBuilder
import xyz.xenondevs.particle.ParticleEffect
import xyz.xenondevs.particle.data.texture.BlockTexture
import kotlin.random.Random

fun Block.breakAndTakeDrops(): Collection<ItemStack> {
    val drops = drops
    
    val state = state
    if (state is Chest) {
        drops += state.blockInventory.contents.filterNotNull()
        state.blockInventory.clear()
    } else if (state is Container) {
        drops += state.inventory.contents.filterNotNull()
        state.inventory.clear()
    }
    
    type = Material.AIR
    
    return drops
}

fun Block.playBreakEffects() {
    // break particles
    ParticleBuilder(ParticleEffect.BLOCK_CRACK, location.add(0.5, 0.5, 0.5))
        .setParticleData(BlockTexture(type))
        .setOffsetX(0.2f)
        .setOffsetY(0.2f)
        .setOffsetZ(0.2f)
        .setAmount(50)
        .display()
    
    // break sound
    val breakSound = SoundUtils.getSoundEffects(this)[0]
    world.playSound(location, breakSound, 1f, Random.nextDouble(0.8, 0.95).toFloat())
}
