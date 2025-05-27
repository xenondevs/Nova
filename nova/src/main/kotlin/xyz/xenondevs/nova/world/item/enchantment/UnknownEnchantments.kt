package xyz.xenondevs.nova.world.item.enchantment

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.resources.ResourceGeneration

private const val STORAGE_ID = "custom_enchantment_ids"

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    dependsOn = [ResourceGeneration.PreWorld::class]
)
internal object UnknownEnchantments {
    
    private val customEnchantmentIds: HashSet<Key> = PermanentStorage.retrieve(STORAGE_ID) ?: HashSet()
    private val registeredIds = HashSet<Key>()
    
    @InitFun
    private fun addUnknownEnchantments() {
        PermanentStorage.store(STORAGE_ID, customEnchantmentIds)
        
        val unregisteredIds = customEnchantmentIds - registeredIds
        for (id in unregisteredIds) {
            val builder = EnchantmentBuilder(id)
            with(builder) {
                name(Component.translatable(
                    "enchantment.nova.unknown",
                    NamedTextColor.RED,
                    Component.text(id.toString())
                ))
            }
            builder.register()
        }
    }
    
    fun rememberEnchantmentId(id: Key) {
        customEnchantmentIds += id
        registeredIds += id
    }
    
}