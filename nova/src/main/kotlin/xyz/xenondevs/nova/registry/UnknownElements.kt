package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    runBefore = [RegistryLoader::class]
)
internal object UnknownElements {
    
    @InitFun
    private fun registerUnknownBuilderFactories() {
        RegistryLoader.registerVanillaUnknown(RegistryKey.ENCHANTMENT, ::EnchantmentBuilderImpl) {
            name(Component.translatable(
                "enchantment.nova.unknown",
                NamedTextColor.RED,
                Component.text(entry.key.asString())
            ))
        }
        
        RegistryLoader.registerVanillaUnknown(RegistryKey.CHICKEN_VARIANT, ::ChickenVariantBuilderImpl) {}
        RegistryLoader.registerVanillaUnknown(RegistryKey.COW_VARIANT, ::CowVariantBuilderImpl) {}
        RegistryLoader.registerVanillaUnknown(RegistryKey.FROG_VARIANT, ::FrogVariantBuilderImpl) {}
        RegistryLoader.registerVanillaUnknown(RegistryKey.PIG_VARIANT, ::PigVariantBuilderImpl) {}
        RegistryLoader.registerVanillaUnknown(RegistryKey.WOLF_VARIANT, ::WolfVariantBuilderImpl) {}
        RegistryLoader.registerVanillaUnknown(RegistryKey.WOLF_SOUND_VARIANT, ::WolfSoundVariantBuilderImpl) {}
    }
    
}