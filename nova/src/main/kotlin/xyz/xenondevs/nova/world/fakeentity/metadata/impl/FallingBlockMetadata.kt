package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.core.BlockPos
import net.minecraft.network.syncher.EntityDataSerializers
import org.joml.Vector3i
import org.joml.Vector3ic

class FallingBlockMetadata : EntityMetadata() {
    
    var spawnPosition: Vector3ic by entry(8, EntityDataSerializers.BLOCK_POS, Vector3i()) { BlockPos(it.x(), it.y(), it.z()) }
    
}