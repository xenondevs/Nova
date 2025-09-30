package xyz.xenondevs.nova.resources.builder

import xyz.xenondevs.nova.util.data.WildcardUtils

/**
 * Filters files from the resource pack based on the specified criteria.
 *
 * @param stage The stage at which the filter is applied.
 * @param type The type of filter (whitelist or blacklist).
 * @param filter The regex pattern to match file paths against.
 * @param directory An optional directory to which the filter is scoped, or `null` for global application.
 */
class ResourceFilter(
    val stage: Stage,
    val type: Type,
    val filter: Regex,
    val directory: String? = null
) {
    
    /**
     * Creates a new [ResourceFilter] using a wildcard pattern.
     * 
     * @param stage The stage at which the filter is applied.
     * @param type The type of filter (whitelist or blacklist).
     * @param filterWildcard The wildcard pattern to match file paths against.
     * @param directory An optional directory to which the filter is scoped, or `null` for global application.
     */
    constructor(stage: Stage, type: Type, filterWildcard: String, directory: String? = null) :
        this(stage, type, WildcardUtils.toRegex(filterWildcard), directory)
    
    /**
     * Checks whether the given [path] is allowed by this filter.
     */
    fun allows(path: String): Boolean =
        when (type) {
            Type.WHITELIST -> (directory != null && !path.startsWith(directory)) || filter.matches(path)
            Type.BLACKLIST -> (directory != null && !path.startsWith(directory)) || !filter.matches(path)
        }
    
    /**
     * The stage at which the filter is applied.
     */
    enum class Stage {
        
        /**
         * During extraction from the asset packs.
         */
        ASSET_PACK,
        
        /**
         * During the final assembly of the resource pack.
         */
        RESOURCE_PACK
        
    }
    
    /**
     * The type of filter.
     */
    enum class Type {
        
        /**
         * Allows only files that match the filter.
         */
        WHITELIST,
        
        /**
         * Blocks files that match the filter.
         */
        BLACKLIST
        
    }
    
    /**
     * The type of pattern used for filtering.
     */
    enum class PatternType {
        
        /**
         * A wildcard pattern, using '*' to match any sequence of characters
         * and '?' to match any single character.
         */
        WILDCARD,
        
        /**
         * A regular expression pattern.
         */
        REGEX
        
    }
    
}
