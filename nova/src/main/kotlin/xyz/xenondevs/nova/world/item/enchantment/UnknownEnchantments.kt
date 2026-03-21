package xyz.xenondevs.nova.world.item.enchantment

import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.EnchantmentBuilderImpl
import xyz.xenondevs.nova.registry.RegistryLoader

private const val STORAGE_ID = "custom_enchantment_ids"

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    runBefore = [RegistryLoader::class]
)
internal object UnknownEnchantments {
    
    private val customEnchantmentIds: HashSet<Key> = PermanentStorage.retrieve(STORAGE_ID) ?: HashSet()
    private val registeredIds = HashSet<Key>()
    
    @InitFun // fixme: incorrect init order, same with all other "unknown" registry entries
    private fun addUnknownEnchantments() {
        PermanentStorage.store(STORAGE_ID, customEnchantmentIds)
        
        val unregisteredIds = customEnchantmentIds - registeredIds
        for (id in unregisteredIds) {
            RegistryLoader.enqueueVanilla(RegistryKey.ENCHANTMENT, id, ::EnchantmentBuilderImpl) {
                name(Component.translatable(
                    "enchantment.nova.unknown",
                    NamedTextColor.RED,
                    Component.text(id.toString())
                ))
            }
        }
    }
    
    fun rememberEnchantmentId(id: Key) {
        customEnchantmentIds += id
        registeredIds += id
    }
    
}