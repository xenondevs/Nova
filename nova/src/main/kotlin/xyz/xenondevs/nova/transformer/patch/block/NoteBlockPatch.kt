@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.transformer.patch.block

import net.minecraft.world.level.block.NoteBlock
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.transformer.ClassTransformer
import xyz.xenondevs.nova.world.block.behavior.impl.noteblock.AgentNoteBlockBehavior

internal object NoteBlockPatch : ClassTransformer(NoteBlock::class) {
    
    override fun shouldTransform(): Boolean =
        DEFAULT_CONFIG.getBoolean("resource_pack.generation.use_solid_blocks")
    
    override fun transform() {
        NoteBlock::neighborChanged.replaceWith(AgentNoteBlockBehavior::neighborChanged)
        NoteBlock::use.replaceWith(AgentNoteBlockBehavior::use)
        NoteBlock::attack.replaceWith(AgentNoteBlockBehavior::attack)

        val triggerEvent = VirtualClassPath[NoteBlock::triggerEvent]
        triggerEvent.localVariables.clear()
        triggerEvent.instructions = buildInsnList { ldc(1); ireturn() }
    }
    
}