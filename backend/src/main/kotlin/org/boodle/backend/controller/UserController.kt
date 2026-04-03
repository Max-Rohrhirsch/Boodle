package org.boodle.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.boodle.backend.model.CreateUserRequest
import org.boodle.backend.model.InvalidUserInputException
import org.boodle.backend.model.UpdateUserRequest
import org.boodle.backend.model.UserAlreadyExistsException
import org.boodle.backend.model.UserDTO
import org.boodle.backend.model.UserNotFoundException
import org.boodle.backend.model.UserRole
import org.boodle.backend.model.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
class UserController(private val userService: UserService) {

    @GetMapping
    @Operation(summary = "Get all users")
    fun getAllUsers(): ResponseEntity<Any> =
        ResponseEntity.ok(userService.getAllUsers())

    @GetMapping("/search")
    @Operation(summary = "Search users for autocomplete")
    fun searchUsers(
        @RequestParam("q") query: String,
        @RequestParam("rolle", required = false) rolle: UserRole?,
        @RequestParam("limit", required = false, defaultValue = "10") limit: Int
    ): ResponseEntity<Any> =
        ResponseEntity.ok(userService.searchUsers(query, rolle, limit))

    @GetMapping("/{matr}")
    @Operation(summary = "Get user by matrikelnummer")
    fun getUserByMatr(@PathVariable matr: String): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(userService.getUserByMatr(matr))
        } catch (e: UserNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "User not found"))
        }

    @PostMapping
    @Operation(summary = "Create user")
    fun createUser(@RequestBody request: CreateUserRequest): ResponseEntity<Any> =
        try {
            val created = userService.create(
                matr = request.matr,
                name = request.name,
                password = request.password,
                email = request.email,
                rolle = request.rolle
            )
            ResponseEntity.status(HttpStatus.CREATED).body(created)
        } catch (e: UserAlreadyExistsException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(HttpStatus.CONFLICT, e.message ?: "User already exists"))
        } catch (e: InvalidUserInputException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(HttpStatus.BAD_REQUEST, e.message ?: "Invalid request"))
        }

    @PutMapping("/{matr}")
    @Operation(summary = "Update user")
    fun updateUser(@PathVariable matr: String, @RequestBody request: UpdateUserRequest): ResponseEntity<Any> =
        try {
            val updated = userService.update(
                matr = matr,
                name = request.name,
                email = request.email,
                rolle = request.rolle
            )
            ResponseEntity.ok(updated)
        } catch (e: UserNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "User not found"))
        } catch (e: InvalidUserInputException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(HttpStatus.BAD_REQUEST, e.message ?: "Invalid request"))
        }

    @DeleteMapping("/{matr}")
    @Operation(summary = "Delete user")
    fun deleteUser(@PathVariable matr: String): ResponseEntity<Any> =
        try {
            userService.delete(matr)
            ResponseEntity.noContent().build()
        } catch (e: UserNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "User not found"))
        }

    private fun errorBody(status: HttpStatus, message: String): Map<String, Any> = mapOf(
        "status" to status.value(),
        "error" to status.reasonPhrase,
        "message" to message
    )
}
