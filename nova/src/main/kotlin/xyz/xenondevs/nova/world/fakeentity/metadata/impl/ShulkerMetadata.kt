package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.syncher.EntityDataSerializers
import org.bukkit.DyeColor
import org.bukkit.block.BlockFace

class ShulkerMetadata : MobMetadata() {
    
    var attachedFace: BlockFace by entry(16, EntityDataSerializers.BYTE, BlockFace.NORTH) { it.ordinal.toByte() }
    var peek: Byte by entry(17, EntityDataSerializers.BYTE, 0)
    var color: DyeColor by entry(18, EntityDataSerializers.BYTE, DyeColor.PURPLE) { it.ordinal.toByte() }
    
}