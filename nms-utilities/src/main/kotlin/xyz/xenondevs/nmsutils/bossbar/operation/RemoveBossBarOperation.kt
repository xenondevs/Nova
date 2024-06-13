package xyz.xenondevs.nmsutils.bossbar.operation

import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry

private val REMOVE_OPERATION = ReflectionRegistry.CLIENTBOUND_BOSS_EVENT_PACKET_REMOVE_OPERATION_FIELD.get(null)!!

object RemoveBossBarOperation : BossBarOperation(), BossBarOperation.Type<RemoveBossBarOperation> {
    override fun fromNMS(operation: Any) = this
    override fun toNMS(): Any = REMOVE_OPERATION
}