package xyz.xenondevs.nova.tileentity.vanilla

import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.util.Instrument

internal class VanillaNoteBlockTileEntity constructor(blockState: VanillaTileEntityState) : VanillaTileEntity(blockState) {
    
    override val type = Type.NOTE_BLOCK
    
    var instrument: Instrument by storedValue("instrument") { Instrument.byBlockAbove(pos.add(0, 1, 0)) ?: Instrument.byBlockBelow(pos.add(0, -1, 0))}
    var note: Int by storedValue("note") { 0 }
    var powered: Boolean by storedValue("powered") { false }
    
    fun cycleNote() {
        note = (note + 1) % 25
    }
    
    override fun handleInitialized() = Unit
    override fun handleRemoved(unload: Boolean) = Unit
    
}