package xyz.xenondevs.nova.transformer.patch.noteblock

import net.minecraft.world.level.block.NoteBlock
import org.objectweb.asm.Type
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.transformer.ClassTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionUtils

internal object NoteBlockPatch : ClassTransformer(NoteBlock::class, computeFrames = true) {
    
    override fun transform() {
        val patchesClass = NoteBlockMethods::class.java
        
        val neighborChangedMethod = ReflectionUtils.getMethodByName(patchesClass, true, "neighborChanged")
        classWrapper.getMethod("SRM(net.minecraft.world.level.block.NoteBlock neighborChanged)", Type.getType(neighborChangedMethod).descriptor)!!
            .instructions = VirtualClassPath.getInstructions(neighborChangedMethod)
        
        val useMethod = ReflectionUtils.getMethodByName(patchesClass, true, "use")
        classWrapper.getMethod("SRM(net.minecraft.world.level.block.NoteBlock use)", Type.getType(useMethod).descriptor)!!
            .instructions = VirtualClassPath.getInstructions(useMethod)
        
        val attackMethod = ReflectionUtils.getMethodByName(patchesClass, true, "attack")
        classWrapper.getMethod("SRM(net.minecraft.world.level.block.NoteBlock attack)", Type.getType(attackMethod).descriptor)!!
            .instructions = VirtualClassPath.getInstructions(attackMethod)
        
        val triggerEventMethod = ReflectionUtils.getMethodByName(patchesClass, true, "triggerEvent")
        classWrapper.getMethod("SRM(net.minecraft.world.level.block.NoteBlock triggerEvent)", Type.getType(triggerEventMethod).descriptor)!!
            .instructions = VirtualClassPath.getInstructions(triggerEventMethod)
    }
    
}