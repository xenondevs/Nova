package xyz.xenondevs.nmsutils.advancement.predicate

import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.adapter.impl.DoubleBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import net.minecraft.advancements.critereon.DistancePredicate as MojangDistancePredicate

class DistancePredicate(
    val x: ClosedRange<Double>?,
    val y: ClosedRange<Double>?,
    val z: ClosedRange<Double>?,
    val horizontal: ClosedRange<Double>?,
    val absolute: ClosedRange<Double>?
) : Predicate {
    
    companion object : NonNullAdapter<DistancePredicate, MojangDistancePredicate>(MojangDistancePredicate.ANY) {
        
        override fun convert(value: DistancePredicate): MojangDistancePredicate {
            return MojangDistancePredicate(
                DoubleBoundsAdapter.toNMS(value.x),
                DoubleBoundsAdapter.toNMS(value.y),
                DoubleBoundsAdapter.toNMS(value.z),
                DoubleBoundsAdapter.toNMS(value.horizontal),
                DoubleBoundsAdapter.toNMS(value.absolute),
            )
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private var x: ClosedRange<Double>? = null
        private var y: ClosedRange<Double>? = null
        private var z: ClosedRange<Double>? = null
        private var horizontal: ClosedRange<Double>? = null
        private var absolute: ClosedRange<Double>? = null
        
        fun x(x: ClosedRange<Double>) {
            this.x = x
        }
        
        fun y(y: ClosedRange<Double>) {
            this.y = y
        }
        
        fun z(z: ClosedRange<Double>) {
            this.z = z
        }
        
        fun horizontal(horizontal: ClosedRange<Double>) {
            this.horizontal = horizontal
        }
        
        fun absolute(absolute: ClosedRange<Double>) {
            this.absolute = absolute
        }
        
        internal fun build(): DistancePredicate {
            return DistancePredicate(x, y, z, horizontal, absolute)
        }
        
    }
    
}
