package xyz.xenondevs.nova.resources.builder.layout.entity

import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl

@RegistryElementBuilderDsl
abstract class EntityVariantLayoutBuilder<T : EntityVariantLayout> internal constructor() {
    
    internal abstract fun build(): T
    
}