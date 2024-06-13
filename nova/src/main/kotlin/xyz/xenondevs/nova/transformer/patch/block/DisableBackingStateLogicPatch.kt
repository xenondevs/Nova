package xyz.xenondevs.nova.transformer.patch.block

import net.minecraft.world.InteractionResult
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HugeMushroomBlock
import net.minecraft.world.level.block.NoteBlock
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.transformer.MultiTransformer

internal object DisableBackingStateLogicPatch : MultiTransformer(NoteBlock::class, HugeMushroomBlock::class) {
    
    override fun transform() {
        val returnDefaultBlockState = buildInsnList { 
            addLabel()
            aLoad(0)
            invokeVirtual(Block::defaultBlockState)
            areturn()
        }
        
        val returnFirst = buildInsnList { 
            addLabel()
            aLoad(1)
            areturn()
        }
        
        val emptyInsn = buildInsnList { 
            addLabel()
            _return()
        }
        
        val passInteraction = buildInsnList { 
            addLabel()
            getStatic(InteractionResult.PASS)
            areturn()
        }
        
        val returnFalse = buildInsnList { 
            addLabel()
            ldc(0)
            ireturn()
        }
        
        VirtualClassPath[NoteBlock::getStateForPlacement].replaceInstructions(returnDefaultBlockState)
        VirtualClassPath[NoteBlock::updateShape].replaceInstructions(returnFirst)
        VirtualClassPath[NoteBlock::neighborChanged].replaceInstructions(emptyInsn)
        VirtualClassPath[NoteBlock::use].replaceInstructions(passInteraction)
        VirtualClassPath[NoteBlock::attack].replaceInstructions(emptyInsn)
        VirtualClassPath[NoteBlock::triggerEvent].replaceInstructions(returnFalse)
        
        VirtualClassPath[HugeMushroomBlock::getStateForPlacement].replaceInstructions(returnDefaultBlockState)
        VirtualClassPath[HugeMushroomBlock::updateShape].replaceInstructions(returnFirst)
        VirtualClassPath[HugeMushroomBlock::rotate].replaceInstructions(returnDefaultBlockState)
        VirtualClassPath[HugeMushroomBlock::mirror].replaceInstructions(returnDefaultBlockState)
    }

}