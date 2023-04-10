package xyz.xenondevs.nova.transformer.patch.worldgen

import net.minecraft.util.ThreadingDetector
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry

/**
 * Patches the [ThreadingDetector] to allow for "concurrent access".
 * (They still wait for the lock, but no exception is thrown)
 */
internal object ThreadingDetectorPatch : MethodTransformer(ThreadingDetector::checkAndLock) {
    
    override fun transform() {
        methodNode.localVariables.clear()
        methodNode.tryCatchBlocks.clear()
        methodNode.instructions = buildInsnList { 
            aLoad(0)
            getField(ReflectionRegistry.THREADING_DETECTOR_LOCK_FIELD)
            invokeVirtual(ReflectionRegistry.SEMAPHORE_ACQUIRE_METHOD)
            _return()
        }
    }
    
}