package org.boodle.backend.integration

import org.boodle.backend.config.JwtTokenService
import org.boodle.backend.model.UserDTO
import org.boodle.backend.model.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class JwtTokenServiceIntegrationTest {

    @Test
    fun generateToken_andParseClaims_roundTripsExpectedValues() {
        val tokenService = JwtTokenService(
            secret = "0123456789abcdef0123456789abcdef",
            issuer = "boodle-backend-tests",
            expirationSeconds = 3600
        )

        val now = LocalDateTime.now()
        val user = UserDTO(
            matr = "1234567",
            name = "Max Mustermann",
            email = "max@gmail.com",
            rolle = UserRole.STUDENT,
            createdAt = now,
            updatedAt = now
        )

        val token = tokenService.generateToken(user)
        val claims = tokenService.parseClaims(token)

        assertEquals("1234567", claims.subject)
        assertEquals("boodle-backend-tests", claims.issuer)
        assertEquals("max@gmail.com", claims["email"])
        assertTrue((claims["roles"] as Collection<*>).contains("STUDENT"))
    }
}
