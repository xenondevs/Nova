package xyz.xenondevs.nova.context

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

private data object TestIntention : AbstractContextIntention<TestIntention>() {
    
    val STRING_PARENT = addOptionalParamType<String>()
    val STRING = addRequiredParamType<String>()
    val STRING_LENGTH = addOptionalParamType<Int>()
    val STRING_LENGTH_AS_STRING = addOptionalParamType<String>()
    val BOOLEAN = addDefaultingParamType(default = false)
    
    init {
        addAutofiller(STRING, Autofiller.from(STRING_PARENT) { it })
        addAutofiller(STRING, Autofiller.from(STRING_PARENT) { "overruled: $it" })
        
        addAutofiller(STRING_LENGTH, Autofiller.from(STRING) { it.length })
        
        addAutofiller(STRING_LENGTH_AS_STRING, Autofiller.dynamic { resolve(STRING_LENGTH)?.toString() ?: "missing" })
    }
    
}

private data object TestIntention2 : AbstractContextIntention<TestIntention2>() {
    
    val STRING_SOURCE = addOptionalParamType<String>()
    val STRING = addOptionalParamType<String>()
    val STRING_MIRROR = addOptionalParamType<String>()
    
    init {
        addAutofiller(STRING, Autofiller.from(STRING_MIRROR) { it })
        addAutofiller(STRING_MIRROR, Autofiller.from(STRING) { it })
        addAutofiller(STRING, Autofiller.from(STRING_SOURCE) { it })
    }
    
}

private data object DefaultingIntention : AbstractContextIntention<DefaultingIntention>() {
    
    val DERIVED_FROM_TYPE = addOptionalParamType<String>()
    val TYPE = addDefaultingParamType(default = "air")
    val STACK = addDefaultingParamType(default = "empty")
    val SOURCE = addOptionalParamType<String>()
    
    init {
        addAutofiller(STACK, Autofiller.from(TYPE) { "stack:$it" })
        addAutofiller(STACK, Autofiller.from(SOURCE) { "stack:$it" })
        
        addAutofiller(TYPE, Autofiller.from(STACK) { it.removePrefix("stack:") })
        
        addAutofiller(DERIVED_FROM_TYPE, Autofiller.from(TYPE) { "derived:$it" })
    }
    
}

private data object ValidationIntention : AbstractContextIntention<ValidationIntention>() {
    
    val SOURCE = addOptionalParamType<Int>()
    val POSITIVE = addOptionalParamType<Int>(validate = { it > 0 })
    
    init {
        addAutofiller(POSITIVE, Autofiller.from(SOURCE) { -it })
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
    fun testDynamicAutofillChain() {
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
    fun testRequiredParamAutofillUsesFirstSuccessfulAutofiller() {
        val context = Context.intention(TestIntention)
            .param(TestIntention.STRING_PARENT, "Hello")
            .build()
        
        assertEquals("Hello", context[TestIntention.STRING])
    }
    
    @Test
    fun testAutofillCycleUsesAvailableFallback() {
        val ctx = Context.intention(TestIntention2)
            .param(TestIntention2.STRING_SOURCE, "ABC")
            .build()
        
        assertEquals("ABC", ctx[TestIntention2.STRING_SOURCE])
        assertEquals("ABC", ctx[TestIntention2.STRING])
        assertEquals("ABC", ctx[TestIntention2.STRING_MIRROR])
    }
    
    @Test
    fun testAutofillersUseDerivedValuesBeforeDefaults() {
        val context = Context.intention(DefaultingIntention)
            .param(DefaultingIntention.SOURCE, "diamond")
            .build()
        
        assertEquals("stack:diamond", context[DefaultingIntention.STACK])
        assertEquals("diamond", context[DefaultingIntention.TYPE])
        assertEquals("derived:diamond", context[DefaultingIntention.DERIVED_FROM_TYPE])
    }
    
    @Test
    fun testDefaultsAreNotAutofillerInputs() {
        val context = Context.intention(DefaultingIntention).build()
        
        assertEquals("empty", context[DefaultingIntention.STACK])
        assertEquals("air", context[DefaultingIntention.TYPE])
        assertNull(context[DefaultingIntention.DERIVED_FROM_TYPE])
    }
    
    @Test
    fun testInvalidExplicitValueThrows() {
        assertThrows<IllegalArgumentException> {
            Context.intention(ValidationIntention).param(ValidationIntention.POSITIVE, -1)
        }
    }
    
    @Test
    fun testInvalidAutofilledValueThrows() {
        assertThrows<IllegalStateException> {
            Context.intention(ValidationIntention)
                .param(ValidationIntention.SOURCE, 1)
                .build()
        }
    }
    
}