package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.LootTableTrigger
import net.minecraft.resources.ResourceLocation

class PlayerGeneratesContainerLootTriggerBuilder : TriggerBuilder<LootTableTrigger.TriggerInstance>() {
    
    private var lootTable: ResourceLocation? = null
    
    fun lootTable(id: ResourceLocation) {
        lootTable = id
    }
    
    fun lootTable(lootTable: String) {
        this.lootTable = ResourceLocation(lootTable)
    }
    
    override fun build(): LootTableTrigger.TriggerInstance {
        checkNotNull(lootTable) { "Loottable is not set" }
        return LootTableTrigger.TriggerInstance(player, lootTable!!)
    }
    
}