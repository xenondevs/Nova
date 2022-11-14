@file:Suppress("MemberVisibilityCanBePrivate", "FunctionName")

package xyz.xenondevs.nova.item.behavior

import net.minecraft.nbt.CompoundTag
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.provider.provider
import xyz.xenondevs.nova.data.resources.builder.content.armor.info.ArmorTexture
import xyz.xenondevs.nova.item.PacketItemData
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.options.WearableOptions
import xyz.xenondevs.nova.player.equipment.ArmorType
import xyz.xenondevs.nova.util.data.getOrPut

fun TexturedWearable(armorTexture: ArmorTexture): ItemBehaviorFactory<TexturedWearable> =
    object : ItemBehaviorFactory<TexturedWearable>() {
        override fun create(material: ItemNovaMaterial): TexturedWearable =
            TexturedWearable(armorTexture, WearableOptions.configurable(material))
    }

fun TexturedWearable(armorTexture: ArmorTexture, armorType: ArmorType): ItemBehaviorFactory<TexturedWearable> =
    object : ItemBehaviorFactory<TexturedWearable>() {
        override fun create(material: ItemNovaMaterial): TexturedWearable =
            TexturedWearable(armorTexture, WearableOptions.semiConfigurable(armorType, material))
    }

class TexturedWearable(val armorTexture: ArmorTexture, options: WearableOptions) : Wearable(options) {
    
    override val vanillaMaterialProperties = provider(listOf(
        when (options.armorType) {
            ArmorType.HELMET -> VanillaMaterialProperty.HELMET
            ArmorType.CHESTPLATE -> VanillaMaterialProperty.CHESTPLATE
            ArmorType.LEGGINGS -> VanillaMaterialProperty.LEGGINGS
            ArmorType.BOOTS -> VanillaMaterialProperty.BOOTS
        }
    ))
    
    override fun updatePacketItemData(itemStack: ItemStack, itemData: PacketItemData) {
        itemData.nbt.getOrPut("display", ::CompoundTag).putInt("color", armorTexture.color)
    }
    
}