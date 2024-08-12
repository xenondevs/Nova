package xyz.xenondevs.nova.patch.adapter

import xyz.xenondevs.bytebase.jvm.ClassWrapper

interface Adapter {
    
    fun adapt(clazz: ClassWrapper)
    
}