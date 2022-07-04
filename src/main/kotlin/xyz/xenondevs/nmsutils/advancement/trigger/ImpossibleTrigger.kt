package xyz.xenondevs.nmsutils.advancement.trigger

import xyz.xenondevs.nmsutils.adapter.Adapter
import net.minecraft.advancements.critereon.ImpossibleTrigger as MojangImpossibleTrigger

object ImpossibleTrigger : Trigger, Adapter<ImpossibleTrigger, MojangImpossibleTrigger.TriggerInstance> {
    
    override fun toNMS(value: ImpossibleTrigger) = MojangImpossibleTrigger.TriggerInstance()
    
}