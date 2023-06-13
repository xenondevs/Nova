package xyz.xenondevs.nova.transformer.adapter

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunkSection
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.util.MethodNode
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.nova.util.ServerUtils.SERVER_SOFTWARE

object LcsWrapperAdapter : Adapter {
    
    override fun adapt(clazz: ClassWrapper) {
        if (SERVER_SOFTWARE.isPaper()) {
            val blockStateName = "L" + BlockState::class.internalName + ";"
            clazz.methods.add(MethodNode(ACC_PUBLIC, "updateKnownBlockInfo", "(I$blockStateName$blockStateName)V") {
                addLabel()
                aLoad(0)
                getField(clazz.name, "delegate", "L" + LevelChunkSection::class.internalName + ";")
                iLoad(1)
                aLoad(2)
                aLoad(3)
                invokeVirtual(LevelChunkSection::class.internalName, "updateKnownBlockInfo", "(I$blockStateName$blockStateName)V")
                aLoad(0)
                invokeVirtual(clazz.name, "copyBlockCounts", "()V")
                
                addLabel()
                _return()
            })
            
            val friendlyByteBufName = "L" + FriendlyByteBuf::class.internalName + ";"
            val chunkPacketInfoName = "Lcom/destroystokyo/paper/antixray/ChunkPacketInfo;"
            val writeDesc = "($friendlyByteBufName${chunkPacketInfoName}I)V"
            clazz.methods.add(MethodNode(ACC_PUBLIC, "write", writeDesc) {
                addLabel()
                aLoad(0)
                getField(clazz.name, "delegate", "L" + LevelChunkSection::class.internalName + ";")
                
                aLoad(1)
                aLoad(2)
                iLoad(3)
                
                invokeVirtual(LevelChunkSection::class.internalName, "write", writeDesc)
                
                addLabel()
                _return()
            })
        }
    }
    
}