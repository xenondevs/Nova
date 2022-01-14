package xyz.xenondevs.nova.integration.protection

import xyz.xenondevs.nova.api.protection.ProtectionIntegration

interface InternalProtectionIntegration : ProtectionIntegration {
    
    val isInstalled: Boolean
    
}