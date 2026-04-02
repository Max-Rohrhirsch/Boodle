package org.boodle.backend.controller

import io.mockk.every
import io.mockk.mockk
import org.boodle.backend.model.CreateUserRequest
import org.boodle.backend.model.UserAlreadyExistsException
import org.boodle.backend.model.UserRole
import org.boodle.backend.model.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class UserControllerMockingTest {

    private val userService = mockk<UserService>()
    private val controller = UserController(userService)

    @Test
    fun createUser_returnsConflict_whenMatrAlreadyExists() {
        val request = CreateUserRequest(
            matr = "12345678",
            name = "Max Mustermann",
            password = "12345678",
            email = "max@gmail.com",
            rolle = UserRole.STUDENT
        )

        every {
            userService.create(
                matr = request.matr,
                name = request.name,
                password = request.password,
                email = request.email,
                rolle = request.rolle
            )
        } throws UserAlreadyExistsException(request.matr)

        val response = controller.createUser(request)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        val body = response.body as Map<*, *>
        assertEquals("Conflict", body["error"])
    }
}
