package xyz.xenondevs.nova.context

import net.kyori.adventure.key.Key
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

data object TestIntention : AbstractContextIntention<TestIntention>() {
    
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
    
}