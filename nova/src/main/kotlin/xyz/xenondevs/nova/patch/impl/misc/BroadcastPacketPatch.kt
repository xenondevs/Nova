package xyz.xenondevs.nova.patch.impl.misc

import net.minecraft.network.protocol.Packet
import net.minecraft.resources.ResourceKey
import net.minecraft.server.players.PlayerList
import net.minecraft.world.entity.player.Player
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.nova.patch.MethodTransformer

internal object BroadcastPacketPatch : MethodTransformer(PlayerList::broadcast) {
    
    var dropAll = false
    var ignoreExcludedPlayer = false
    var exclude: Player? = null
    
    override fun transform() {
        methodNode.instructions = buildInsnList {
            aLoad(0) // this
            aLoad(1) // player
            dLoad(2) // x
            dLoad(4) // y
            dLoad(6) // z
            dLoad(8) // max distance
            aLoad(10) // dimension
            aLoad(11) // packet
            invokeStatic(::broadcast)
            _return()
        }
    }
    
    @JvmStatic
    fun broadcast(playerList: PlayerList, exclude: Player?, x: Double, y: Double, z: Double, maxDistance: Double, dimension: ResourceKey<*>, packet: Packet<*>) {
        if (dropAll)
            return
        
        val excludedPlayer = this.exclude ?: exclude
        
        for (player in playerList.players) {
            if (
                (ignoreExcludedPlayer || excludedPlayer !== player)
                && player.level().dimension() == dimension
                && (excludedPlayer == null || player.bukkitEntity.canSee(excludedPlayer.bukkitEntity))
            ) {
                val dx = x - player.x
                val dy = y - player.y
                val dz = z - player.z
                
                if (dx * dx + dy * dy + dz * dz < maxDistance * maxDistance) {
                    player.connection.send(packet)
                }
            }
        }
    }
    
}