package org.boodle.backend.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class SecurityUtils {
    
    /**
     * Gets the current authenticated user's matrikelnummer (subject claim from JWT).
     * Returns null if not authenticated or in an anonymous context.
     */
    fun getCurrentUserMatr(): String? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication != null && authentication.isAuthenticated) {
            authentication.principal as? String ?: authentication.name
        } else {
            null
        }
    }
    
    /**
     * Asserts that the current user is authenticated and returns their matrikelnummer.
     * Throws IllegalStateException if not authenticated.
     */
    fun requireCurrentUserMatr(): String {
        return getCurrentUserMatr() 
            ?: throw IllegalStateException("No authenticated user found in security context")
    }
}
