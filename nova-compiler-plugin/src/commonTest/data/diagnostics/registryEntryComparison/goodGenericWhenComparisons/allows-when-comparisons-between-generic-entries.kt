import org.bukkit.Keyed
import xyz.xenondevs.nova.registry.*

fun <T : BOUND> test(subject: SUBJECT, branch: BRANCH) {
    when (subject) {
        branch -> Unit
        else -> Unit
    }
}
