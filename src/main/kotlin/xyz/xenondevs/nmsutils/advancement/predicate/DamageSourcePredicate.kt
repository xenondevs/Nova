package xyz.xenondevs.nmsutils.advancement.predicate

import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import net.minecraft.advancements.critereon.DamageSourcePredicate as MojangDamageSourcePredicate

class DamageSourcePredicate(
    val projectile: Boolean?,
    val explosion: Boolean?,
    val fire: Boolean?,
    val magic: Boolean?,
    val lightning: Boolean?,
    val bypassesArmor: Boolean?,
    val bypassesInvulnerability: Boolean?,
    val bypassesMagic: Boolean?,
    val directEntity: EntityPredicate?,
    val sourceEntity: EntityPredicate?
) : Predicate {
    
    companion object : NonNullAdapter<DamageSourcePredicate, MojangDamageSourcePredicate>(MojangDamageSourcePredicate.ANY) {
        
        override fun convert(value: DamageSourcePredicate): MojangDamageSourcePredicate {
            return MojangDamageSourcePredicate(
                value.projectile,
                value.explosion,
                value.bypassesArmor,
                value.bypassesInvulnerability,
                value.bypassesMagic,
                value.fire,
                value.magic,
                value.lightning,
                EntityPredicate.toNMS(value.directEntity),
                EntityPredicate.toNMS(value.sourceEntity)
            )
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private var projectile: Boolean? = null
        private var explosion: Boolean? = null
        private var fire: Boolean? = null
        private var magic: Boolean? = null
        private var lightning: Boolean? = null
        private var bypassesArmor: Boolean? = null
        private var bypassesInvulnerability: Boolean? = null
        private var bypassesMagic: Boolean? = null
        private var directEntity: EntityPredicate? = null
        private var sourceEntity: EntityPredicate? = null
        
        fun projectile(projectile: Boolean) {
            this.projectile = projectile
        }
        
        fun explosion(explosion: Boolean) {
            this.explosion = explosion
        }
        
        fun fire(fire: Boolean) {
            this.fire = fire
        }
        
        fun magic(magic: Boolean) {
            this.magic = magic
        }
        
        fun lightning(lightning: Boolean) {
            this.lightning = lightning
        }
        
        fun bypassesArmor(bypassesArmor: Boolean) {
            this.bypassesArmor = bypassesArmor
        }
        
        fun bypassesInvulnerability(bypassesInvulnerability: Boolean) {
            this.bypassesInvulnerability = bypassesInvulnerability
        }
        
        fun bypassesMagic(bypassesMagic: Boolean) {
            this.bypassesMagic = bypassesMagic
        }
        
        fun directEntity(init: EntityPredicate.Builder.() -> Unit) {
            directEntity = EntityPredicate.Builder().apply(init).build()
        }
        
        fun sourceEntity(init: EntityPredicate.Builder.() -> Unit) {
            sourceEntity = EntityPredicate.Builder().apply(init).build()
        }
        
        internal fun build(): DamageSourcePredicate {
            return DamageSourcePredicate(
                projectile,
                explosion,
                fire,
                magic,
                lightning,
                bypassesArmor,
                bypassesInvulnerability,
                bypassesMagic,
                directEntity,
                sourceEntity
            )
        }
        
    }
    
}