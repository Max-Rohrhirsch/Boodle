package org.boodle.backend.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val tokenType: String,
    val expiresInSeconds: Long,
    val user: UserDTO
)