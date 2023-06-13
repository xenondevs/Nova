package xyz.xenondevs.nova.item.enchantment

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.enchantment.Enchantments
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.set
import net.minecraft.world.item.enchantment.Enchantment as MojangEnchantment

@InternalInit(stage = InternalInitStage.PRE_WORLD)
object VanillaEnchantments {
    
    val PROTECTION = register("protection", Enchantments.ALL_DAMAGE_PROTECTION)
    val FIRE_PROTECTION = register("fire_protection", Enchantments.FIRE_PROTECTION)
    val FEATHER_FALLING = register("feather_falling", Enchantments.FALL_PROTECTION)
    val BLAST_PROTECTION = register("blast_protection", Enchantments.BLAST_PROTECTION)
    val PROJECTILE_PROTECTION = register("projectile_protection", Enchantments.PROJECTILE_PROTECTION)
    val RESPIRATION = register("respiration", Enchantments.RESPIRATION)
    val AQUA_AFFINITY = register("aqua_affinity", Enchantments.AQUA_AFFINITY)
    val THORNS = register("thorns", Enchantments.THORNS)
    val DEPTH_STRIDER = register("depth_strider", Enchantments.DEPTH_STRIDER)
    val FROST_WALKER = register("frost_walker", Enchantments.FROST_WALKER)
    val BINDING_CURSE = register("binding_curse", Enchantments.BINDING_CURSE)
    val SOUL_SPEED = register("soul_speed", Enchantments.SOUL_SPEED)
    val SWIFT_SNEAK = register("swift_sneak", Enchantments.SWEEPING_EDGE)
    val SHARPNESS = register("sharpness", Enchantments.SHARPNESS)
    val SMITE = register("smite", Enchantments.SMITE)
    val BANE_OF_ARTHROPODS = register("bane_of_arthropods", Enchantments.BANE_OF_ARTHROPODS)
    val KNOCKBACK = register("knockback", Enchantments.KNOCKBACK)
    val FIRE_ASPECT = register("fire_aspect", Enchantments.FIRE_ASPECT)
    val LOOTING = register("looting", Enchantments.MOB_LOOTING)
    val SWEEPING = register("sweeping", Enchantments.SWEEPING_EDGE)
    val EFFICIENCY = register("efficiency", Enchantments.BLOCK_EFFICIENCY)
    val SILK_TOUCH = register("silk_touch", Enchantments.SILK_TOUCH)
    val UNBREAKING = register("unbreaking", Enchantments.UNBREAKING)
    val FORTUNE = register("fortune", Enchantments.BLOCK_FORTUNE)
    val POWER = register("power", Enchantments.POWER_ARROWS)
    val PUNCH = register("punch", Enchantments.PUNCH_ARROWS)
    val FLAME = register("flame", Enchantments.FLAMING_ARROWS)
    val INFINITY = register("infinity", Enchantments.INFINITY_ARROWS)
    val LUCK_OF_THE_SEA = register("luck_of_the_sea", Enchantments.FISHING_LUCK)
    val LURE = register("lure", Enchantments.FISHING_SPEED)
    val LOYALTY = register("loyalty", Enchantments.LOYALTY)
    val IMPALING = register("impaling", Enchantments.IMPALING)
    val RIPTIDE = register("riptide", Enchantments.RIPTIDE)
    val CHANNELING = register("channeling", Enchantments.CHANNELING)
    val MULTISHOT = register("multishot", Enchantments.MULTISHOT)
    val QUICK_CHARGE = register("quick_charge", Enchantments.QUICK_CHARGE)
    val PIERCING = register("piercing", Enchantments.PIERCING)
    val MENDING = register("mending", Enchantments.MENDING)
    val VANISHING_CURSE = register("vanishing_curse", Enchantments.VANISHING_CURSE)
    
    private fun register(name: String, vanillaEnchantment: MojangEnchantment): Enchantment {
        val id = ResourceLocation("minecraft", name)
        val category = VanillaEnchantment(id, vanillaEnchantment)
        NovaRegistries.ENCHANTMENT[id] = category
        return category
    }
    
}