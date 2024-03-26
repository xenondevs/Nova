package xyz.xenondevs.nova.world.format

class NotImplementedIdResolver : IdResolver<String> {
    override val size = -1
    override fun fromId(id: Int): String = throw UnsupportedOperationException()
    override fun toId(value: String?): Int = throw UnsupportedOperationException()
}