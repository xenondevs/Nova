package xyz.xenondevs.nova.detekt

import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class NovaRuleSetProvider : RuleSetProvider {
    
    override val ruleSetId = RuleSetId("NovaRuleSet")
    
    override fun instance() = RuleSet(
        ruleSetId,
        mapOf(
            RuleName("RegistryEntryComparison") to ::RegistryEntryComparisonRule
        )
    )
    
}