package xyz.xenondevs.nmsutils.advancement

@AdvancementDsl
class RequirementBuilder {
    
    private var criteria: MutableList<String> = ArrayList()
    
    fun criterion(criterion: Criterion) {
        criteria.add(criterion.name)
    }
    
    fun criterion(criterion: String) {
        criteria.add(criterion)
    }
    
    fun criteria(criteria: List<Criterion>) {
        this.criteria = criteria.mapTo(ArrayList()) { it.name }
    }
    
    @JvmName("criteria1")
    fun criteria(criteria: List<String>) {
        this.criteria = criteria.toMutableList()
    }
    
    internal fun build(): Array<String> = criteria.toTypedArray()
    
}

@AdvancementDsl
class RequirementsBuilder {
    
    private val requirements = ArrayList<Array<String>>()
    
    fun requirement(init: RequirementBuilder.() -> Unit) {
        val builder = RequirementBuilder()
        init.invoke(builder)
        requirements += builder.build()
    }
    
    internal fun build(): Array<Array<String>> = requirements.toTypedArray()
    
}