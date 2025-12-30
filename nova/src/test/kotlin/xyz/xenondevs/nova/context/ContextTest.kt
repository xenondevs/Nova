package xyz.xenondevs.nova.context

import net.kyori.adventure.key.Key
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

private data object TestIntention : AbstractContextIntention<TestIntention>() {
    
    val STRING_PARENT = ContextParamType<String, TestIntention>(Key.key("nova", "string_parent"))
    val STRING = RequiredContextParamType<String, TestIntention>(Key.key("nova", "string"))
    val STRING_LENGTH = ContextParamType<Int, TestIntention>(Key.key("nova", "string_length"))
    val STRING_LENGTH_AS_STRING = ContextParamType<String, TestIntention>(Key.key("nova", "string_length_as_string"))
    val BOOLEAN = DefaultingContextParamType<Boolean, TestIntention>(Key.key("nova", "boolean"), false)
    
    init {
        require(STRING)
        addAutofiller(STRING, Autofiller.from(STRING_PARENT) { it })
        addAutofiller(STRING_LENGTH, Autofiller.from(STRING) { it.length })
        addAutofiller(STRING_LENGTH_AS_STRING, Autofiller.from(STRING_LENGTH) { it.toString() })
    }
    
}

private data object TestIntention2 : AbstractContextIntention<TestIntention2>() {
    
    val STRING_SOURCE = ContextParamType<String, TestIntention2>(Key.key("nova", "string_source"))
    val STRING = ContextParamType<String, TestIntention2>(Key.key("nova", "string"))
    val STRING_MIRROR = ContextParamType<String, TestIntention2>(Key.key("nova", "string_mirror"))
    
    init {
        addAutofiller(STRING, Autofiller.from(STRING_MIRROR) { it })
        addAutofiller(STRING_MIRROR, Autofiller.from(STRING) { it } )
        addAutofiller(STRING, Autofiller.from(STRING_SOURCE) { it } )
    }
    
}

class ContextTest {
    
    @Test
    fun testFailOnMissingRequiredParams() {
        assertThrows<IllegalStateException> {
            Context.intention(TestIntention)
                .param(TestIntention.BOOLEAN, true)
                .build()
        }
    }
    
    @Test
    fun testAutofill() {
        val context = Context.intention(TestIntention)
            .param(TestIntention.STRING, "Hello")
            .build()
        
        assertEquals("Hello", context[TestIntention.STRING])
        assertEquals(5, context[TestIntention.STRING_LENGTH])
    }
    
    @Test
    fun testAutofillChain() {
        val context = Context.intention(TestIntention)
            .param(TestIntention.STRING, "Hello")
            .build()
        
        assertEquals("Hello", context[TestIntention.STRING])
        assertEquals("5", context[TestIntention.STRING_LENGTH_AS_STRING])
    }
    
    @Test
    fun testDefaultValue() {
        val context = Context.intention(TestIntention)
            .param(TestIntention.STRING, "Hello")
            .build()
        
        assertEquals(false, context[TestIntention.BOOLEAN])
    }
    
    @Test
    fun testDefaultValueDoesNotOverrideExplicitValue() {
        val context = Context.intention(TestIntention)
            .param(TestIntention.STRING, "Hello")
            .param(TestIntention.BOOLEAN, true)
            .build()
        
        assertEquals(true, context[TestIntention.BOOLEAN])
    }
    
    @Test
    fun testDoesNotFailWhenRequiredParamsAreResolvable() {
        val context = Context.intention(TestIntention)
            .param(TestIntention.STRING_PARENT, "Hello")
            .build()
        
        assertEquals("Hello", context[TestIntention.STRING])
    }
    
    @Test
    fun `test that failing autofiller is not remembered after value becomes available`() {
        val ctx = Context.intention(TestIntention2)
            .param(TestIntention2.STRING_SOURCE, "ABC")
            .build()
        
        // When querying STRING, the autofiller will first try STRING_MIRROR, which is not set and cannot be 
        // filled by STRING as well, setting STRING_MIRROR to null.
        // Then, STRING queries STRING_SOURCE and copies its value
        // It is expected that STRING_MIRROR can now correctly retrieve the value from STRING
        // and does not remember the previous failure
        
        assertEquals(ctx[TestIntention2.STRING_SOURCE], "ABC")
        assertEquals(ctx[TestIntention2.STRING], "ABC")
        assertEquals(ctx[TestIntention2.STRING_MIRROR], "ABC")
    }
    
}