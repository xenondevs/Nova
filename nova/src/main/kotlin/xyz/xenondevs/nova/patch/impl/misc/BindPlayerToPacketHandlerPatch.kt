package xyz.xenondevs.nova.patch.impl.misc

import net.minecraft.network.Connection
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.players.PlayerList
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.network.PacketManager
import xyz.xenondevs.nova.patch.MultiTransformer

internal object BindPlayerToPacketHandlerPatch : MultiTransformer(PlayerList::class) {
    
    override fun transform() {
        VirtualClassPath[PlayerList::placeNewPlayer].instructions.insert(buildInsnList { 
            addLabel()
            aLoad(1)
            aLoad(2)
            invokeStatic(::placeNewPlayer)
        })
    }
    
    @JvmStatic
    fun placeNewPlayer(connection: Connection, player: ServerPlayer) {
        PacketManager.handlePlayerCreated(player, connection)
    }

}