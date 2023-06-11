package xyz.xenondevs.nova.util.data.font

internal data class SizeOverride(val from: Int, val to: Int, val left: Int, val right: Int) {
    
    constructor(from: String, to: String, left: Int, right: Int) : this(
        from.codePoints().findFirst().orElseThrow(), to.codePoints().findFirst().orElseThrow(), left, right
    )
    
}