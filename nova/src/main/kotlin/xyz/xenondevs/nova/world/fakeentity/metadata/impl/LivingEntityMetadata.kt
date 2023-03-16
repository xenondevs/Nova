package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.core.BlockPos
import net.minecraft.network.syncher.EntityDataSerializers

open class LivingEntityMetadata internal constructor(): EntityMetadata() {
    
    private val sharedFlags = sharedFlags(8)
    
    var isHandActive: Boolean by sharedFlags[0]
    var activeHand: Boolean by sharedFlags[1]
    var isInRiptideSpinAttack: Boolean by sharedFlags[2]
    var health: Float by entry(9, EntityDataSerializers.FLOAT, 1f)
    var potionEffectColor: Int by entry(10, EntityDataSerializers.INT, 0)
    var isPotionEffectAmbient: Boolean by entry(11, EntityDataSerializers.BOOLEAN, false)
    var arrowAmount: Int by entry(12, EntityDataSerializers.INT, 0)
    var beeStingers: Int by entry(13, EntityDataSerializers.INT, 0)
    var currentBedPos: BlockPos? by optional(14, EntityDataSerializers.OPTIONAL_BLOCK_POS, null)
    
}