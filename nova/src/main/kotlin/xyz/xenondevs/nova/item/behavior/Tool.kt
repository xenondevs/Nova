package xyz.xenondevs.nova.item.behavior

import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
import net.minecraft.world.entity.ai.attributes.Attributes
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.options.ToolOptions
import xyz.xenondevs.nova.item.vanilla.AttributeModifier
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import java.util.*

private const val PLAYER_ATTACK_SPEED = 4.0
private const val PLAYER_ATTACK_DAMAGE = 1.0

class Tool(val options: ToolOptions) : ItemBehavior() {
    
    override fun getVanillaMaterialProperties(): List<VanillaMaterialProperty> {
        val properties = ArrayList<VanillaMaterialProperty>()
        properties += VanillaMaterialProperty.DAMAGEABLE
        if (!options.canBreakBlocksInCreative)
            properties += VanillaMaterialProperty.CREATIVE_NON_BLOCK_BREAKING
        return properties
    }
    
    override fun getAttributeModifiers(): List<AttributeModifier> {
        val modifiers = ArrayList<AttributeModifier>()
        
        val attackDamage = options.attackDamage
        if (attackDamage != null) {
            modifiers += AttributeModifier(
                BASE_ATTACK_DAMAGE_UUID,
                "Nova Attack Damage",
                Attributes.ATTACK_DAMAGE,
                Operation.ADDITION,
                options.attackDamage!! - PLAYER_ATTACK_DAMAGE,
                true,
                EquipmentSlot.MAINHAND
            )
        }
        
        val attackSpeed = options.attackSpeed
        if (attackSpeed != null) {
            modifiers += AttributeModifier(
                BASE_ATTACK_SPEED_UUID,
                "Nova Attack Speed",
                Attributes.ATTACK_SPEED,
                Operation.ADDITION,
                options.attackSpeed!! - PLAYER_ATTACK_SPEED,
                true,
                EquipmentSlot.MAINHAND
            )
        }
        
        return modifiers
    }
    
    companion object : ItemBehaviorFactory<Tool>() {
        
        val BASE_ATTACK_DAMAGE_UUID: UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF")
        val BASE_ATTACK_SPEED_UUID: UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3")
        
        override fun create(item: NovaItem) =
            Tool(ToolOptions.configurable(item))
    }
    
}