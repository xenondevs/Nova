package xyz.xenondevs.nova.world.block.behavior

enum class InteractionResult(val swingsHand: Boolean, val consumesAction: Boolean) {
    
    SUCCESS(true, true),
    CONSUME(true, true),
    PASS(false, false),
    FAIL(false, false)
    
}