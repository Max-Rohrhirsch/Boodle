package org.boodle.backend.controller

import io.mockk.every
import io.mockk.mockk
import org.boodle.backend.config.JwtTokenService
import org.boodle.backend.model.LoginRequest
import org.boodle.backend.model.User
import org.boodle.backend.model.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder

class AuthControllerMockingTest {

    private val userService = mockk<UserService>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val jwtTokenService = mockk<JwtTokenService>()

    private val controller = AuthController(userService, passwordEncoder, jwtTokenService)

    @Test
    fun login_returnsUnauthorized_whenUserDoesNotExist() {
        every { userService.findByEmail("nobody@gmail.com") } returns null

        val response = controller.login(LoginRequest(email = "nobody@gmail.com", password = "________"))

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        val body = response.body as Map<*, *>
        assertEquals("Invalid credentials", body["message"])
    }

    @Test
    fun login_returnsUnauthorized_whenPasswordDoesNotMatch() {
        val user = mockk<User>()
        every { user.passHash } returns "stored-hash"
        every { userService.findByEmail("student@gmail.com") } returns user
        every { passwordEncoder.matches("wrong-password", "stored-hash") } returns false

        val response = controller.login(LoginRequest(email = "student@gmail.com", password = "wrong-password"))

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        val body = response.body as Map<*, *>
        assertEquals("Invalid credentials", body["message"])
    }
}
