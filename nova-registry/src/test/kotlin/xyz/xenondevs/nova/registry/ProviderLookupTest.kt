package xyz.xenondevs.nova.registry

import org.junit.jupiter.api.Test
import xyz.xenondevs.commons.provider.mutableProvider
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ProviderLookupTest {
    
    @Test
    fun `providers are cached and follow source updates`() {
        val source = mutableProvider(mapOf("present" to 1))
        val lookup = ProviderLookup(source) { 0 }
        
        val present = lookup["present"]
        assertSame(present, lookup["present"])
        assertEquals(1, present.get())
        
        val missing = lookup["missing"]
        assertEquals(0, missing.get())
        
        source.set(mapOf("present" to 2, "missing" to 3))
        assertEquals(2, present.get())
        assertEquals(3, missing.get())
    }
    
}
