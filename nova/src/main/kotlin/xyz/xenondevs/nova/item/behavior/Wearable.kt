package xyz.xenondevs.nova.item.behavior

import net.minecraft.nbt.CompoundTag
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.provider.combinedProvider
import xyz.xenondevs.nova.data.provider.lazyProviderWrapper
import xyz.xenondevs.nova.data.provider.map
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.item.PacketItemData
import xyz.xenondevs.nova.item.vanilla.AttributeModifier
import xyz.xenondevs.nova.item.vanilla.HideableFlag
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.options.WearableOptions
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.player.equipment.ArmorType
import xyz.xenondevs.nova.player.equipment.EquipMethod
import xyz.xenondevs.nova.util.data.getOrPut
import xyz.xenondevs.nova.util.isPlayerView
import xyz.xenondevs.nova.util.item.isActuallyInteractable
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.nmsEquipmentSlot

@Suppress("FunctionName")
fun Wearable(type: ArmorType): ItemBehaviorFactory<Wearable> =
    object : ItemBehaviorFactory<Wearable>() {
        override fun create(material: ItemNovaMaterial): Wearable =
            Wearable(WearableOptions.semiConfigurable(type, material))
    }

class Wearable(val options: WearableOptions) : ItemBehavior() {
    
    private val textureColor: Int? by lazy {
        Resources.getModelData(novaMaterial.id).armor
            ?.let { Resources.getArmorData(it) }?.color
    }
    
    override val vanillaMaterialProperties = lazyProviderWrapper {
        if (textureColor == null)
            return@lazyProviderWrapper emptyList()
        
        return@lazyProviderWrapper listOf(
            when (options.armorType) {
                ArmorType.HELMET -> VanillaMaterialProperty.HELMET
                ArmorType.CHESTPLATE -> VanillaMaterialProperty.CHESTPLATE
                ArmorType.LEGGINGS -> VanillaMaterialProperty.LEGGINGS
                ArmorType.BOOTS -> VanillaMaterialProperty.BOOTS
            }
        )
    }
    
    override val attributeModifiers = combinedProvider(
        options.armorTypeProvider, options.armorProvider, options.armorToughnessProvider, options.knockbackResistanceProvider
    ).map {
        val equipmentSlot = (it[0] as ArmorType).equipmentSlot.nmsEquipmentSlot
        
        return@map listOf(
            AttributeModifier(
                "nova armor ${this@Wearable.hashCode()}",
                Attribute.GENERIC_ARMOR,
                AttributeModifier.Operation.INCREMENT,
                it[1] as Double,
                equipmentSlot
            ),
            AttributeModifier(
                "nova armor toughness ${this@Wearable.hashCode()}",
                Attribute.GENERIC_ARMOR_TOUGHNESS,
                AttributeModifier.Operation.INCREMENT,
                it[2] as Double,
                equipmentSlot
            ),
            AttributeModifier(
                "nova knockback resistance ${this@Wearable.hashCode()}",
                Attribute.GENERIC_KNOCKBACK_RESISTANCE,
                AttributeModifier.Operation.INCREMENT,
                it[3] as Double,
                equipmentSlot
            )
        )
    }
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        if ((action == Action.RIGHT_CLICK_AIR || (action == Action.RIGHT_CLICK_BLOCK && !event.clickedBlock!!.type.isActuallyInteractable()))
            && player.inventory.getItem(options.armorType.equipmentSlot)?.takeUnlessEmpty() == null
            && !callArmorEquipEvent(player, EquipMethod.RIGHT_CLICK_EQUIP, null, itemStack)
        ) {
            event.isCancelled = true
            player.inventory.setItem(options.armorType.equipmentSlot, itemStack)
            player.inventory.setItem(event.hand!!, null)
        }
    }
    
    @Suppress("DEPRECATION")
    override fun handleInventoryClickOnCursor(player: Player, itemStack: ItemStack, event: InventoryClickEvent) {
        val slotType = event.slotType
        val currentItem = event.currentItem
        if (slotType == InventoryType.SlotType.ARMOR
            && event.rawSlot == options.armorType.rawSlot
            && (event.click == ClickType.LEFT || event.click == ClickType.RIGHT)
            && !callArmorEquipEvent(player, EquipMethod.SWAP, currentItem, itemStack)
        ) {
            event.isCancelled = true
            
            player.inventory.setItem(options.armorType.equipmentSlot, itemStack)
            event.cursor = currentItem
        }
    }
    
    override fun handleInventoryClick(player: Player, itemStack: ItemStack, event: InventoryClickEvent) {
        if ((event.click == ClickType.SHIFT_LEFT || event.click == ClickType.SHIFT_RIGHT)
            && event.view.isPlayerView()
            && event.clickedInventory != event.view.topInventory
            && player.inventory.getItem(options.armorType.equipmentSlot)?.takeUnlessEmpty() == null
            && !callArmorEquipEvent(player, EquipMethod.SHIFT_CLICK, null, itemStack)
        ) {
            event.isCancelled = true
            player.inventory.setItem(options.armorType.equipmentSlot, itemStack)
            event.view.setItem(event.rawSlot, null)
        }
    }
    
    override fun handleInventoryHotbarSwap(player: Player, itemStack: ItemStack, event: InventoryClickEvent) {
        val currentItem = event.currentItem
        if (event.slotType == InventoryType.SlotType.ARMOR
            && event.rawSlot == options.armorType.rawSlot
            && !callArmorEquipEvent(player, EquipMethod.HOTBAR_SWAP, currentItem, itemStack)
        ) {
            event.isCancelled = true
            player.inventory.setItem(options.armorType.equipmentSlot, itemStack)
            player.inventory.setItem(event.hotbarButton, currentItem)
        }
    }
    
    private fun callArmorEquipEvent(player: Player, method: EquipMethod, previous: ItemStack?, now: ItemStack?): Boolean {
        val event = ArmorEquipEvent(player, method, previous, now)
        Bukkit.getPluginManager().callEvent(event)
        return event.isCancelled
    }
    
    override fun updatePacketItemData(itemStack: ItemStack, itemData: PacketItemData) {
        val textureColor = textureColor
        if (textureColor != null) {
            itemData.nbt.getOrPut("display", ::CompoundTag).putInt("color", textureColor)
            itemData.hide(HideableFlag.DYE)
        }
        textureColor?.let { itemData.nbt.getOrPut("display", ::CompoundTag).putInt("color", it) }
    }
    
    companion object : ItemBehaviorFactory<Wearable>() {
        override fun create(material: ItemNovaMaterial): Wearable =
            Wearable(WearableOptions.configurable(material))
    }
    
}