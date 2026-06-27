package xyz.xenondevs.nova.packetentity

import xyz.xenondevs.commons.provider.dsl.DslProperty

sealed interface FlagsDslProperty {
    
    operator fun get(index: Int): DslProperty<Boolean>
    
}

sealed interface MutableFlags {
    
    operator fun get(i: Int): Boolean
    operator fun set(i: Int, value: Boolean)
    
}

internal class FlagsState {
    
    private val bits = Array(8) { DefaultEntityValue(false) }
    val packedValue = FlagsEntityValue(bits)
    
    fun get(i: Int) = bits[i]
    
}

internal class FlagsDslPropertyImpl(
    private val state: FlagsState
) : FlagsDslProperty {
    
    override fun get(index: Int): DslProperty<Boolean> =
        state.get(index)
    
}

internal class MutableFlagsImpl(
    private val state: FlagsState
) : MutableFlags {
    
    override fun get(i: Int): Boolean =
        state.get(i).get()
    
    override fun set(i: Int, value: Boolean) {
        state.get(i) by value
    }
    
}
