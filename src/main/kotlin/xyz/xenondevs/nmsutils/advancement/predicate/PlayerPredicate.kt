package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.advancements.critereon.EntitySubPredicate
import net.minecraft.world.level.GameType
import org.bukkit.GameMode
import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.adapter.impl.IntBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import net.minecraft.advancements.critereon.PlayerPredicate as MojangPlayerPredicate

// TODO: stats, recipes, advancements
class PlayerPredicate(
    val level: IntRange?,
    val gameMode: GameMode?,
    val lookingAt: EntityPredicate?
) : Predicate {
    
    companion object : NonNullAdapter<PlayerPredicate, EntitySubPredicate>(MojangPlayerPredicate.ANY) {
        
        @Suppress("DEPRECATION")
        override fun convert(value: PlayerPredicate): MojangPlayerPredicate {
            val builder = MojangPlayerPredicate.Builder()
            
            if (value.level != null)
                builder.setLevel(IntBoundsAdapter.toNMS(value.level))
            if (value.gameMode != null)
                builder.setGameType(GameType.byId(value.gameMode.value))
            if (value.lookingAt != null)
                builder.setLookingAt(EntityPredicate.toNMS(value.lookingAt))
            
            return builder.build()
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private var level: IntRange? = null
        private var gameMode: GameMode? = null
        private var lookingAt: EntityPredicate? = null
        
        fun level(level: IntRange) {
            this.level = level
        }
        
        fun gameMode(gameMode: GameMode) {
            this.gameMode = gameMode
        }
        
        fun lookingAt(init: EntityPredicate.Builder.() -> Unit) {
            lookingAt = EntityPredicate.Builder().apply(init).build()
        }
        
        internal fun build(): PlayerPredicate {
            return PlayerPredicate(level, gameMode, lookingAt)
        }
        
    }
    
}