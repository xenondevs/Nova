package xyz.xenondevs.nova.transformer.patch.block

import net.minecraft.world.level.Level
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.transformer.MultiTransformer

internal object BlockMigrationPatch : MultiTransformer(Level::class) {
    
    override fun transform() {
        // I don't know why this the newBlock == actualBlock check exists, but this
        // patch is necessary to prevent desync caused by block migration. Let's hope this doesn't blow up.
        VirtualClassPath[Level::notifyAndUpdatePhysics].instructions.insert(buildInsnList {
            // newBlock = actualBlock;
            addLabel()
            aLoad(5)
            aStore(4)
        })
    }
    
}