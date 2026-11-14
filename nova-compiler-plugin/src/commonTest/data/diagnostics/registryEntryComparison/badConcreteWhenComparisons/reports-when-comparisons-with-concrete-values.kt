import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.registry.*

fun test(subject: SUBJECT, branch: BRANCH) {
    when (subject) {
        <!NOVA_REGISTRY_ENTRY_COMPARISON!>branch<!> -> Unit
        else -> Unit
    }
}
