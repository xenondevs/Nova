package xyz.xenondevs.nova.data.resources.builder

import xyz.xenondevs.nova.util.data.WildcardUtils

abstract class ResourceFilter internal constructor() {
    abstract fun test(path: String): Boolean
    internal open fun performFilterEvaluations() = Unit
}

class SimpleResourceFilter(
    private val exclusions: List<Regex>
) : ResourceFilter() {
    
    override fun test(path: String): Boolean {
        return exclusions.none { it.matches(path) }
    }
    
}

class ConditionalResourceFilter(
    private val exclusions: Map<Regex, () -> Boolean>
) : ResourceFilter() {
    
    private var evaluatedExclusions: Set<Regex>? = null
    
    override fun performFilterEvaluations() {
        evaluatedExclusions = exclusions.mapNotNullTo(HashSet()) { (regex, condition) -> regex.takeUnless { condition.invoke() } }
    }
    
    override fun test(path: String): Boolean {
        val evaluatedFilters = evaluatedExclusions
        check(evaluatedFilters != null) { "Exclusions have not been evaluated yet" }
        return evaluatedFilters.none { it.matches(path) }
    }
    
}

fun resourceFilterOf(vararg exclusions: Pair<Regex, () -> Boolean>): ConditionalResourceFilter =
    ConditionalResourceFilter(exclusions.toMap())

@JvmName("resourceFilterOf1")
fun resourceFilterOf(vararg exclusions: Pair<String, () -> Boolean>): ConditionalResourceFilter =
    ConditionalResourceFilter(exclusions.toMap().mapKeys { WildcardUtils.toRegex(it.key) })

fun resourceFilterOf(vararg exclusions: Regex): SimpleResourceFilter =
    SimpleResourceFilter(exclusions.asList())

fun resourceFilterOf(vararg exclusions: String): SimpleResourceFilter =
    SimpleResourceFilter(exclusions.map(WildcardUtils::toRegex))