package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.advancements.critereon.PlayerPredicate
import net.minecraft.resources.ResourceLocation
import net.minecraft.stats.Stat
import net.minecraft.world.level.GameType
import org.bukkit.GameMode

class PlayerPredicateBuilder : PredicateBuilder<PlayerPredicate>() {
    
    private val builder = PlayerPredicate.Builder()
    
    fun level(level: MinMaxBounds.Ints) {
        builder.setLevel(level)
    }
    
    fun level(level: IntRange) {
        builder.setLevel(MinMaxBounds.Ints.between(level.first, level.last))
    }
    
    fun level(level: Int) {
        builder.setLevel(MinMaxBounds.Ints.exactly(level))
    }
    
    fun stat(stat: Stat<*>, value: MinMaxBounds.Ints) {
        builder.addStat(stat, value)
    }
    
    fun stat(stat: Stat<*>, value: IntRange) {
        builder.addStat(stat, MinMaxBounds.Ints.between(value.first, value.last))
    }
    
    fun stat(stat: Stat<*>, value: Int) {
        builder.addStat(stat, MinMaxBounds.Ints.exactly(value))
    }
    
    fun recipe(recipe: ResourceLocation, unlocked: Boolean = true) {
        builder.addRecipe(recipe, unlocked)
    }
    
    fun advancement(advancement: ResourceLocation, done: Boolean = true) {
        builder.checkAdvancementDone(advancement, done)
    }
    
    fun advancementCriteria(advancement: ResourceLocation, criteria: Map<String, Boolean>) {
        builder.checkAdvancementCriterions(advancement, criteria)
    }
    
    fun gameMode(gameMode: GameType) {
        builder.setGameType(gameMode)
    }
    
    fun gameMode(gameMode: GameMode) {
        builder.setGameType(GameType.byName(gameMode.name.lowercase()))
    }
    
    fun lookingAt(init: EntityPredicateBuilder.() -> Unit) {
        builder.setLookingAt(EntityPredicateBuilder().apply(init).build())
    }
    
    override fun build(): PlayerPredicate = builder.build()
    
}