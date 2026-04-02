package org.boodle.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.boodle.backend.config.JwtTokenService
import org.boodle.backend.model.LoginRequest
import org.boodle.backend.model.LoginResponse
import org.boodle.backend.model.UserService
import org.boodle.backend.model.toDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
class AuthController(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenService: JwtTokenService
) {
    @PostMapping("/login")
    @Operation(summary = "Login and return JWT")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        val user = userService.findByEmail(request.email)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Invalid credentials"))

        if (!passwordEncoder.matches(request.password, user.passHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Invalid credentials"))
        }

        val userDto = user.toDTO()
        return ResponseEntity.ok(
            LoginResponse(
                token = jwtTokenService.generateToken(userDto),
                tokenType = "Bearer",
                expiresInSeconds = jwtTokenService.expirationSeconds,
                user = userDto
            )
        )
    }

    private fun errorBody(message: String): Map<String, Any> = mapOf(
        "status" to HttpStatus.UNAUTHORIZED.value(),
        "error" to HttpStatus.UNAUTHORIZED.reasonPhrase,
        "message" to message
    )
}