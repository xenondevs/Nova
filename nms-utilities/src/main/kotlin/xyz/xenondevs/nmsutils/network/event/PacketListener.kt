package xyz.xenondevs.nmsutils.network.event

interface PacketListener {
}

fun PacketListener.registerPacketListener() {
    PacketEventManager.registerListener(this)
}

fun PacketListener.unregisterPacketListener() {
    PacketEventManager.unregisterListener(this)
}