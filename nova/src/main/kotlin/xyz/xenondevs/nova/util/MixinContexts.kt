package xyz.xenondevs.nova.util

import net.minecraft.world.phys.Vec3

/**
 * Contains thread locals for passing contexts from mixins.
 */
internal object MixinContexts {
    
    @JvmField
    val INTERACT_LOCATION: ThreadLocal<Vec3> = ThreadLocal.withInitial { Vec3.ZERO }
    
}