package xyz.xenondevs.nova.network.event

interface PacketListener {
}

fun PacketListener.registerPacketListener() {
    PacketEventManager.registerListener(this)
}

fun PacketListener.unregisterPacketListener() {
    PacketEventManager.unregisterListener(this)
}