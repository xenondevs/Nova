package xyz.xenondevs.nova.detekt

import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import org.junit.jupiter.api.Test

class KeyToStringRuleTest {

    private val env = createEnvironment()
    private val typeStubs = arrayOf(
        """
        package net.kyori.adventure.key

        interface Key {
            fun asString(): String

            companion object {
                fun key(value: String): Key = TODO()
            }
        }

        class CustomKey : Key {
            override fun asString(): String = "custom:key"
        }
        """
    )

    @Test
    fun `reports toString calls on keys`() {
        val code = """
            import net.kyori.adventure.key.CustomKey
            import net.kyori.adventure.key.Key

            fun direct(key: Key) = key.toString()
            fun nullable(key: Key?) = key?.toString()
            fun inferred() = Key.key("example:key").toString()
            fun subtype(key: CustomKey) = key.toString()

            class AttachmentType(val key: Key) {
                override fun toString(): String = this@AttachmentType.key.toString()
            }
        """

        val findings = KeyToStringRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assert(findings.size == 5) { findings }
    }

    @Test
    fun `allows asString and unrelated toString calls`() {
        val code = """
            import net.kyori.adventure.key.Key

            fun key(key: Key) = key.asString()
            fun other(value: String) = value.toString()
            fun template(key: Key) = "${'$'}key"
        """

        val findings = KeyToStringRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assert(findings.isEmpty()) { findings }
    }
}
