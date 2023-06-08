package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.DistancePredicate
import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.advancements.critereon.DistancePredicate as MojangDistancePredicate

class DistancePredicateBuilder : PredicateBuilder<DistancePredicate>() {
    
    private var x = MinMaxBounds.Doubles.ANY
    private var y = MinMaxBounds.Doubles.ANY
    private var z = MinMaxBounds.Doubles.ANY
    private var horizontal = MinMaxBounds.Doubles.ANY
    private var absolute = MinMaxBounds.Doubles.ANY
    
    fun x(x: MinMaxBounds.Doubles) {
        this.x = x
    }
    
    fun x(x: ClosedRange<Double>) {
        this.x = MinMaxBounds.Doubles.between(x.start, x.endInclusive)
    }
    
    fun x(x: Double) {
        this.x = MinMaxBounds.Doubles.exactly(x)
    }
    
    fun y(y: MinMaxBounds.Doubles) {
        this.y = y
    }
    
    fun y(y: ClosedRange<Double>) {
        this.y = MinMaxBounds.Doubles.between(y.start, y.endInclusive)
    }
    
    fun y(y: Double) {
        this.y = MinMaxBounds.Doubles.exactly(y)
    }
    
    fun z(z: MinMaxBounds.Doubles) {
        this.z = z
    }
    
    fun z(z: ClosedRange<Double>) {
        this.z = MinMaxBounds.Doubles.between(z.start, z.endInclusive)
    }
    
    fun z(z: Double) {
        this.z = MinMaxBounds.Doubles.exactly(z)
    }
    
    fun horizontal(horizontal: MinMaxBounds.Doubles) {
        this.horizontal = horizontal
    }
    
    fun horizontal(horizontal: ClosedRange<Double>) {
        this.horizontal = MinMaxBounds.Doubles.between(horizontal.start, horizontal.endInclusive)
    }
    
    fun horizontal(horizontal: Double) {
        this.horizontal = MinMaxBounds.Doubles.exactly(horizontal)
    }
    
    fun absolute(absolute: MinMaxBounds.Doubles) {
        this.absolute = absolute
    }
    
    fun absolute(absolute: ClosedRange<Double>) {
        this.absolute = MinMaxBounds.Doubles.between(absolute.start, absolute.endInclusive)
    }
    
    fun absolute(absolute: Double) {
        this.absolute = MinMaxBounds.Doubles.exactly(absolute)
    }
    
    override fun build(): MojangDistancePredicate {
        return MojangDistancePredicate(x, y, z, horizontal, absolute)
    }
    
}
