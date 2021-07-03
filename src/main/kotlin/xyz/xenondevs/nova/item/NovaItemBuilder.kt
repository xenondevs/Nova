package xyz.xenondevs.nova.item

import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.resourcepack.Icon
import net.md_5.bungee.api.chat.TranslatableComponent
import net.md_5.bungee.chat.ComponentSerializer
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.NBTUtils
import xyz.xenondevs.nova.util.ReflectionRegistry.CB_CRAFT_META_ITEM_APPLY_TO_ITEM_METHOD
import xyz.xenondevs.nova.util.ReflectionRegistry.CB_CRAFT_META_ITEM_UNHANDLED_TAGS_FIELD
import xyz.xenondevs.nova.util.clean

private val ICON_BUILDERS = Icon.values().associateWith {
    NovaItemBuilder(Material.POPPY)
        .setCustomModelData(it.itemBuilder.customModelData)
        .setDisplayName("§0")
        as NovaItemBuilder
}

val Icon.novaItemBuilder: NovaItemBuilder
    get() = ICON_BUILDERS[this]!!.clone()

class NovaItemBuilder : ItemBuilder {
    
    var localizedName: TranslatableComponent? = null
    var localizedLore = ArrayList<TranslatableComponent>()
    
    constructor(material: Material) : super(material)
    constructor(material: Material, amount: Int) : super(material, amount)
    constructor(base: ItemStack) : super(base)
    constructor(headTexture: HeadTexture) : super(headTexture)
    
    @Suppress("UNCHECKED_CAST")
    override fun build(): ItemStack {
        val itemStack = super.build()
        if (localizedName != null || localizedLore.isNotEmpty()) {
            val itemMeta = itemStack.itemMeta!!
            
            val mainTag = CompoundTag()
            CB_CRAFT_META_ITEM_APPLY_TO_ITEM_METHOD.invoke(itemMeta, mainTag)
            val displayTag = mainTag.getCompound("display")
            
            if (localizedName != null) displayTag.putString("Name", ComponentSerializer.toString(localizedName))
            if (localizedLore.isNotEmpty()) {
                val serializedLore = localizedLore.map { ComponentSerializer.toString(it) }
                displayTag.put("Lore", NBTUtils.createStringList(serializedLore))
            }
            
            val unhandledTags = CB_CRAFT_META_ITEM_UNHANDLED_TAGS_FIELD.get(itemMeta) as MutableMap<String, Tag>
            unhandledTags["display"] = displayTag
            
            itemStack.itemMeta = itemMeta
        }
        
        return itemStack
    }
    
    fun setLocalizedName(localizedName: String): NovaItemBuilder {
        if (localizedName.isEmpty() && displayName != null) {
            this.localizedName = null
        } else {
            this.displayName = null
            this.localizedName = TranslatableComponent(localizedName).clean()
        }
        return this
    }
    
    override fun setDisplayName(displayName: String?): ItemBuilder {
        this.localizedName = null
        return super.setDisplayName(displayName)
    }
    
    fun setLocalizedName(component: TranslatableComponent): NovaItemBuilder {
        this.localizedName = component.duplicate().clean()
        return this
    }
    
    fun addLocalizedLoreLines(vararg localizedNames: String): NovaItemBuilder {
        for (localizedName in localizedNames) {
            val component = TranslatableComponent(localizedName).clean()
            localizedLore.add(component)
        }
        
        return this
    }
    
    fun addLocalizedLoreLines(vararg components: TranslatableComponent): NovaItemBuilder {
        localizedLore.addAll(components.map { it.duplicate().clean() })
        return this
    }
    
    fun clearLocalizedLore(): NovaItemBuilder {
        localizedLore.clear()
        return this
    }
    
    override fun clone(): NovaItemBuilder {
        val clone = super.clone() as NovaItemBuilder
        clone.localizedName = localizedName?.duplicate()
        clone.localizedLore = localizedLore.mapTo(ArrayList()) { it.duplicate() }
        
        return clone
    }
    
}