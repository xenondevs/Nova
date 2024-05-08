package xyz.xenondevs.nova.data.context

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import xyz.xenondevs.nova.data.context.intention.ContextIntention
import xyz.xenondevs.nova.data.context.param.ContextParamType
import xyz.xenondevs.nova.data.context.param.DefaultingContextParamType
import kotlin.test.assertEquals

object TestIntentions {
    
    data object TestIntention : ContextIntention()
    
}

object TestParamTypes {
    
    val STRING_PARENT: ContextParamType<String> =
        ContextParamType.builder<String>("string_parent")
            .optionalIn(TestIntentions.TestIntention)
            .build()
    
    val STRING: ContextParamType<String> =
        ContextParamType.builder<String>("string")
            .requiredIn(TestIntentions.TestIntention)
            .autofilledBy(::STRING_PARENT) { it }
            .build()
    
    val STRING_LENGTH: ContextParamType<Int> =
        ContextParamType.builder<Int>("string_length")
            .optionalIn(TestIntentions.TestIntention)
            .autofilledBy(::STRING) { it.length }
            .build()
    
    val STRING_LENGTH_AS_STRING: ContextParamType<String> =
        ContextParamType.builder<String>("string_length_as_string")
            .optionalIn(TestIntentions.TestIntention)
            .autofilledBy(::STRING_LENGTH) { it.toString() }
            .build()
    
    val BOOLEAN: DefaultingContextParamType<Boolean> =
        ContextParamType.builder<Boolean>("boolean")
            .optionalIn(TestIntentions.TestIntention)
            .build(false)
    
}

class ContextTest {
    
    @Test
    fun testFailOnMissingRequiredParams() {
        assertThrows<IllegalStateException> {
            Context.intention(TestIntentions.TestIntention)
                .param(TestParamTypes.BOOLEAN, true)
                .build()
        }
    }
    
    @Test
    fun testAutofill() {
        val context = Context.intention(TestIntentions.TestIntention)
            .param(TestParamTypes.STRING, "Hello")
            .build()
        
        assertEquals("Hello", context[TestParamTypes.STRING])
        assertEquals(5, context[TestParamTypes.STRING_LENGTH])
    }
    
    @Test
    fun testAutofillChain() {
        val context = Context.intention(TestIntentions.TestIntention)
            .param(TestParamTypes.STRING, "Hello")
            .build()
        
        assertEquals("Hello", context[TestParamTypes.STRING])
        assertEquals("5", context[TestParamTypes.STRING_LENGTH_AS_STRING])
    }
    
    @Test
    fun testDefaultValue() {
        val context = Context.intention(TestIntentions.TestIntention)
            .param(TestParamTypes.STRING, "Hello")
            .build()
        
        assertEquals(false, context[TestParamTypes.BOOLEAN])
    }
    
    @Test
    fun testDefaultValueDoesNotOverrideExplicitValue() {
        val context = Context.intention(TestIntentions.TestIntention)
            .param(TestParamTypes.STRING, "Hello")
            .param(TestParamTypes.BOOLEAN, true)
            .build()
        
        assertEquals(true, context[TestParamTypes.BOOLEAN])
    }
    
    @Test
    fun testDoesNotFailWhenRequiredParamsAreResolvable() {
        val context = Context.intention(TestIntentions.TestIntention)
            .param(TestParamTypes.STRING_PARENT, "Hello")
            .build()
        
        assertEquals("Hello", context[TestParamTypes.STRING])
    }
    
}