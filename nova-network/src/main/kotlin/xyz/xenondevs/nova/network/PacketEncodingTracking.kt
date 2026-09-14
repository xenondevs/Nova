package xyz.xenondevs.nova.network

@JvmField
internal val PACKET_ENCODING: ScopedValue<Unit> = ScopedValue.newInstance()

/**
 * Whether the current thread is currently encoding packet data.
 */
val isInPacketEncoding: Boolean
    get() = PACKET_ENCODING.isBound
