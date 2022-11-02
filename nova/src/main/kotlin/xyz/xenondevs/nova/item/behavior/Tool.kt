package xyz.xenondevs.nova.item.behavior

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.MobType
import net.minecraft.world.item.enchantment.EnchantmentHelper
import org.bukkit.attribute.Attribute
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.provider.combinedProvider
import xyz.xenondevs.nova.data.provider.map
import xyz.xenondevs.nova.item.PacketItemData
import xyz.xenondevs.nova.item.vanilla.AttributeModifier
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.options.ToolOptions
import xyz.xenondevs.nova.util.data.appendLocalized
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.nmsCopy

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
    
    override val attributeModifiers = combinedProvider(options.attackSpeedProvider, options.attackDamageProvider).map {
        val attackSpeed = it[0]
        val attackDamage = it[1]
        
        buildList {
            if (attackSpeed != null) {
                this += AttributeModifier(
                    Attribute.GENERIC_ATTACK_SPEED,
                    AttributeModifier.Operation.INCREMENT,
                    options.attackSpeed!! - PLAYER_ATTACK_SPEED,
                    EquipmentSlot.MAINHAND
                )
            }
            if (attackDamage != null) {
                this += AttributeModifier(
                    Attribute.GENERIC_ATTACK_DAMAGE,
                    AttributeModifier.Operation.INCREMENT,
                    options.attackDamage!! - PLAYER_ATTACK_DAMAGE,
                    EquipmentSlot.MAINHAND
                )
            }
        }
    }
    
    override fun updatePacketItemData(itemStack: ItemStack, itemData: PacketItemData) {
        if (options.attackDamage != null && options.attackSpeed != null) {
            itemData.addLore(arrayOf(TextComponent(" ")))
            itemData.addLore(arrayOf(localized(ChatColor.GRAY, "item.modifiers.mainhand")))
            
            val mojangStack = itemStack.nmsCopy
            val attackDamage = options.attackDamage!! + EnchantmentHelper.getDamageBonus(mojangStack, MobType.UNDEFINED)
            itemData.addLore(ComponentBuilder(" ${attackDamage.toFormattedString()} ")
                .color(ChatColor.DARK_GREEN)
                .appendLocalized("attribute.name.generic.attack_damage")
                .create())
            itemData.addLore(ComponentBuilder(" ${options.attackSpeed!!.toFormattedString()} ")
                .color(ChatColor.DARK_GREEN)
                .appendLocalized("attribute.name.generic.attack_speed")
                .create())
        }
    }
    
    private fun Double.toFormattedString(): String = toString().removeSuffix(".0")
    
    companion object : ItemBehaviorFactory<Tool>() {
        override fun create(material: ItemNovaMaterial) =
            Tool(ToolOptions.configurable(material))
    }
    
}