package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.BlockPredicate
import net.minecraft.advancements.critereon.FluidPredicate
import net.minecraft.advancements.critereon.LightPredicate
import net.minecraft.advancements.critereon.LocationPredicate
import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.structure.Structure
import xyz.xenondevs.nmsutils.LightPredicate

class LocationPredicateBuilder : PredicateBuilder<LocationPredicate>() {
    
    private var x = MinMaxBounds.Doubles.ANY
    private var y = MinMaxBounds.Doubles.ANY
    private var z = MinMaxBounds.Doubles.ANY
    private var biome: ResourceKey<Biome>? = null
    private var structure: ResourceKey<Structure>? = null
    private var dimension: ResourceKey<Level>? = null
    private var smokey: Boolean? = null
    private var light = LightPredicate.ANY
    private var block = BlockPredicate.ANY
    private var fluid = FluidPredicate.ANY
    
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
    
    fun biome(biome: ResourceKey<Biome>) {
        this.biome = biome
    }
    
    fun structure(structure: ResourceKey<Structure>) {
        this.structure = structure
    }
    
    fun dimension(dimension: ResourceKey<Level>) {
        this.dimension = dimension
    }
    
    fun smokey(smokey: Boolean) {
        this.smokey = smokey
    }
    
    fun light(light: MinMaxBounds.Ints) {
        this.light = LightPredicate(light)
    }
    
    fun light(light: IntRange) {
        this.light = LightPredicate(MinMaxBounds.Ints.between(light.first, light.last))
    }
    
    fun light(light: Int) {
        this.light = LightPredicate(MinMaxBounds.Ints.exactly(light))
    }
    
    fun block(init: BlockPredicateBuilder.() -> Unit) {
        this.block = BlockPredicateBuilder().apply(init).build()
    }
    
    fun fluid(init: FluidPredicateBuilder.() -> Unit) {
        this.fluid = FluidPredicateBuilder().apply(init).build()
    }
    
    override fun build() = LocationPredicate(x, y, z, biome, structure, dimension, smokey, light, block, fluid)
    
}