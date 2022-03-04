package xyz.xenondevs.nova.player.ability

import org.bukkit.entity.Player

private val ID_PATTERN = Regex("""^[a-z][a-z0-9_]*:[a-z][a-z0-9_]*$""")

class AbilityType<T : Ability>(val id: String, val createAbility: (Player) -> T) {
    
    init {
        require(id.matches(ID_PATTERN)) { "Ability id $id does not match pattern $ID_PATTERN" }
    }
    
}