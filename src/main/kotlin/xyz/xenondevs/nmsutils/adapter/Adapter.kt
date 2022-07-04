package xyz.xenondevs.nmsutils.adapter

interface Adapter<V, R> {
    
    fun toNMS(value: V): R
    
}