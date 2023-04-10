package xyz.xenondevs.nova.transformer.adapter

import xyz.xenondevs.bytebase.jvm.ClassWrapper

interface Adapter {
    
    fun adapt(clazz: ClassWrapper)
    
}