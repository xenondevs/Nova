package xyz.xenondevs.nova.util

/**
 * Contains scoped values for passing context from mixins.
 */
internal object MixinContext {
    
    @JvmField
    val IS_USING_SECONDARY_ACTION: ScopedValue<Boolean> = ScopedValue.newInstance()
    
}