package xyz.xenondevs.nova.util.bossbar.operation

import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import java.lang.invoke.MethodHandles

private val OPERATION = Class.forName("net.minecraft.network.protocol.game.ClientboundBossEventPacket\$Operation")
private val REMOVE_OPERATION = MethodHandles
    .privateLookupIn(ClientboundBossEventPacket::class.java, MethodHandles.lookup())
    .findStaticGetter(ClientboundBossEventPacket::class.java, "REMOVE_OPERATION", OPERATION)
    .invoke()

object RemoveBossBarOperation : BossBarOperation(), BossBarOperation.Type<RemoveBossBarOperation> {
    override fun fromNMS(operation: Any) = this
    override fun toNMS(): Any = REMOVE_OPERATION
}