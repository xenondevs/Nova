package xyz.xenondevs.nova.item.behavior

import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
import net.minecraft.world.entity.ai.attributes.Attributes
import xyz.xenondevs.commons.provider.immutable.combinedProvider
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.nova.item.vanilla.AttributeModifier
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.options.ToolOptions
import java.util.*

private const val PLAYER_ATTACK_SPEED = 4.0
private const val PLAYER_ATTACK_DAMAGE = 1.0

class Tool(val options: ToolOptions) : ItemBehavior() {
    
    override val vanillaMaterialProperties = options.canBreakBlocksInCreativeProvider.map { canBreakBlocksInCreative ->
        buildList {
            this += VanillaMaterialProperty.DAMAGEABLE
            if (!canBreakBlocksInCreative)
                this += VanillaMaterialProperty.CREATIVE_NON_BLOCK_BREAKING
        }
    }
    
    override val attributeModifiers = combinedProvider(options.attackSpeedProvider, options.attackDamageProvider) { attackSpeed, attackDamage ->
        buildList {
            if (attackDamage != null) {
                this += AttributeModifier(
                    BASE_ATTACK_DAMAGE_UUID,
                    "Nova Attack Damage",
                    Attributes.ATTACK_DAMAGE,
                    Operation.ADDITION,
                    options.attackDamage!! - PLAYER_ATTACK_DAMAGE,
                    true,
                    EquipmentSlot.MAINHAND
                )
            }
            if (attackSpeed != null) {
                this += AttributeModifier(
                    BASE_ATTACK_SPEED_UUID,
                    "Nova Attack Speed",
                    Attributes.ATTACK_SPEED,
                    Operation.ADDITION,
                    options.attackSpeed!! - PLAYER_ATTACK_SPEED,
                    true,
                    EquipmentSlot.MAINHAND
                )
            }
        }
    }
    
    companion object : ItemBehaviorFactory<Tool>() {
        
        val BASE_ATTACK_DAMAGE_UUID: UUID = UUID.fromString("e63e979b-a109-4519-8b20-2ce3962e490c")
        val BASE_ATTACK_SPEED_UUID: UUID = UUID.fromString("54277290-d821-4616-936b-eee88e98bc0b")
        
        override fun create(material: ItemNovaMaterial) =
            Tool(ToolOptions.configurable(material))
    }
    
}