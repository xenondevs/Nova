import org.bukkit.Keyed
import xyz.xenondevs.nova.registry.*

fun <T : BOUND> test(left: LEFT, right: RIGHT) {
    <!NOVA_REGISTRY_ENTRY_COMPARISON!>left OPERATION right<!>
}
