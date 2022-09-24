package xyz.xenondevs.nova.material.options

import xyz.xenondevs.nova.data.config.ConfigAccess
import xyz.xenondevs.nova.material.ItemNovaMaterial

@HardcodedMaterialOptions
fun DamageableOptions(
    maxDurability: Int,
    itemDamageOnAttackEntity: Int,
    itemDamageOnBreakBlock: Int
): DamageableOptions = HardcodedDamageableOptions(maxDurability, itemDamageOnAttackEntity, itemDamageOnBreakBlock)

sealed interface DamageableOptions {
    
    val maxDurability: Int
    val itemDamageOnAttackEntity: Int
    val itemDamageOnBreakBlock: Int
    
    companion object : MaterialOptionsType<DamageableOptions> {
        
        override fun configurable(material: ItemNovaMaterial): DamageableOptions =
            ConfigurableDamageableOptions(material)
        
        override fun configurable(path: String): DamageableOptions =
            ConfigurableDamageableOptions(path)
        
    }
    
}

private class HardcodedDamageableOptions(
    override val maxDurability: Int,
    override val itemDamageOnAttackEntity: Int,
    override val itemDamageOnBreakBlock: Int
) : DamageableOptions

private class ConfigurableDamageableOptions : ConfigAccess, DamageableOptions {
    
    override val maxDurability by getEntry<Int>("max_durability")
    override val itemDamageOnAttackEntity by getEntry<Int>("item_damage_on_attack_entity")
    override val itemDamageOnBreakBlock by getEntry<Int>("item_damage_on_break_block")
    
    constructor(path: String) : super(path)
    constructor(material: ItemNovaMaterial) : super(material)
    
}