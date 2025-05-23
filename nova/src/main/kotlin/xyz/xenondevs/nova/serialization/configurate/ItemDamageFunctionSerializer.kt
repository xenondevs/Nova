package xyz.xenondevs.nova.serialization.configurate

import io.papermc.paper.datacomponent.item.blocksattacks.ItemDamageFunction
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import xyz.xenondevs.nova.util.data.get
import xyz.xenondevs.nova.util.data.setTyped
import java.lang.reflect.Type

internal object ItemDamageFunctionSerializer : TypeSerializer<ItemDamageFunction> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): ItemDamageFunction? {
        if (node.raw() == null)
            return null
        
        val threshold = node.node("threshold").get<Float>()
        val base = node.node("base").get<Float>()
        val factor = node.node("factor").get<Float>()
        
        val builder = ItemDamageFunction.itemDamageFunction()
        if (threshold != null)
            builder.threshold(threshold)
        if (base != null)
            builder.base(base)
        if (factor != null)
            builder.factor(factor)
        
        return builder.build()
    }
    
    override fun serialize(type: Type, obj: ItemDamageFunction?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }
        
        node.node("threshold").setTyped(obj.threshold())
        node.node("base").setTyped(obj.base())
        node.node("factor").setTyped(obj.factor())
    }
    
}