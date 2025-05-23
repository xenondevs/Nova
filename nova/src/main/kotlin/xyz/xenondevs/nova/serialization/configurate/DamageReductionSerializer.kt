package xyz.xenondevs.nova.serialization.configurate

import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction
import io.papermc.paper.registry.set.RegistryKeySet
import org.bukkit.damage.DamageType
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import xyz.xenondevs.nova.util.data.get
import xyz.xenondevs.nova.util.data.setTyped
import java.lang.reflect.Type

internal object DamageReductionSerializer : TypeSerializer<DamageReduction> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): DamageReduction? {
        if (node.raw() == null)
            return null
        
        val base = node.node("base").get<Float>()
        val factor = node.node("factor").get<Float>()
        val horizontalBlockingAngle = node.node("horizontal_blocking_angle").get<Float>()
        val type = node.node("type").get<RegistryKeySet<DamageType>>()
        
        val builder = DamageReduction.damageReduction()
        if (base != null)
            builder.base(base)
        if (factor != null)
            builder.factor(factor)
        if (horizontalBlockingAngle != null)
            builder.horizontalBlockingAngle(horizontalBlockingAngle)
        if (type != null)
            builder.type(type)
        
        return builder.build()
    }
    
    override fun serialize(type: Type, obj: DamageReduction?, node: ConfigurationNode) {
        if (obj == null) {
            node.raw(null)
            return
        }
        
        node.node("base").setTyped(obj.base())
        node.node("factor").setTyped(obj.factor())
        node.node("horizontal_blocking_angle").setTyped(obj.horizontalBlockingAngle())
        node.node("type").setTyped(obj.type())
    }
    
}