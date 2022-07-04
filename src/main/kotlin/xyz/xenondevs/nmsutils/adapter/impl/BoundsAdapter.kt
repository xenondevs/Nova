package xyz.xenondevs.nmsutils.adapter.impl

import net.minecraft.advancements.critereon.MinMaxBounds
import xyz.xenondevs.nmsutils.adapter.NonNullAdapter

object IntBoundsAdapter : NonNullAdapter<IntRange, MinMaxBounds.Ints>(MinMaxBounds.Ints.ANY) {
    
    override fun convert(value: IntRange): MinMaxBounds.Ints {
        return MinMaxBounds.Ints.between(value.first, value.last)
    }
    
}

object DoubleBoundsAdapter : NonNullAdapter<ClosedRange<Double>, MinMaxBounds.Doubles>(MinMaxBounds.Doubles.ANY) {
    
    override fun convert(value: ClosedRange<Double>): MinMaxBounds.Doubles {
        return MinMaxBounds.Doubles.between(value.start, value.endInclusive)
    }
    
}