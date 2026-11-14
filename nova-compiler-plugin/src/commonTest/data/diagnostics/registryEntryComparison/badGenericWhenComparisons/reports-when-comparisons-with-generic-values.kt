import org.bukkit.Keyed
import xyz.xenondevs.nova.registry.*

fun <T : BOUND> test(subject: SUBJECT, branch: BRANCH) {
    when (subject) {
        <!NOVA_REGISTRY_ENTRY_COMPARISON!>branch<!> -> Unit
        else -> Unit
    }
}
