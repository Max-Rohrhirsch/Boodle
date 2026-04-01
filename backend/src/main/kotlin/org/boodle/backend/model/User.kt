package org.boodle.backend.model

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Base64
import java.util.UUID

object UsersTable : IdTable<String>("users") {
    val matr = varchar("matr", 20).entityId()
    override val id = matr
    override val primaryKey = PrimaryKey(id)

    val name = varchar("name", 255)
    val passHash = varchar("pass_hash", 255)
    val salt = varchar("salt", 255)
    val email = varchar("email", 255)
    val rolle = enumerationByName<UserRole>("rolle", 50)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

data class UserDTO(
    val matr: String,
    val name: String,
    val email: String,
    val rolle: UserRole,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class CreateUserRequest(
    val matr: String,
    val name: String,
    val password: String,
    val email: String,
    val rolle: UserRole
)

data class UpdateUserRequest(
    val name: String,
    val email: String,
    val rolle: UserRole
)

class User(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, User>(UsersTable)

    var name by UsersTable.name
    var passHash by UsersTable.passHash
    var salt by UsersTable.salt
    var email by UsersTable.email
    var rolle by UsersTable.rolle
    var createdAt by UsersTable.createdAt
    var updatedAt by UsersTable.updatedAt
}

fun User.toDTO(): UserDTO = UserDTO(
    matr = id.value,
    name = name,
    email = email,
    rolle = rolle,
    createdAt = createdAt,
    updatedAt = updatedAt
)

class UserNotFoundException(matr: String) : RuntimeException("User with matr '$matr' was not found.")
class UserAlreadyExistsException(matr: String) : RuntimeException("User with matr '$matr' already exists.")
class InvalidUserInputException(message: String) : RuntimeException(message)

@Service
class UserService {
    fun getAllUsers(): List<UserDTO> = transaction {
        User.all().map { it.toDTO() }
    }

    fun getUserByMatr(matr: String): UserDTO = transaction {
        User.findById(matr)?.toDTO() ?: throw UserNotFoundException(matr)
    }

    fun create(
        matr: String,
        name: String,
        password: String,
        email: String,
        rolle: UserRole
    ): UserDTO = transaction {
        val normalizedMatr = matr.trim()
        val normalizedName = name.trim()
        val normalizedEmail = email.trim().lowercase()

        if (normalizedMatr.isBlank()) throw InvalidUserInputException("Matrikelnummer must not be empty.")
        if (normalizedName.isBlank()) throw InvalidUserInputException("Name must not be empty.")
        if (!normalizedEmail.contains("@")) throw InvalidUserInputException("Email is invalid.")
        if (password.length < 8) throw InvalidUserInputException("Password must be at least 8 characters long.")

        if (User.findById(normalizedMatr) != null) {
            throw UserAlreadyExistsException(normalizedMatr)
        }

        if (User.find { UsersTable.email eq normalizedEmail }.firstOrNull() != null) {
            throw InvalidUserInputException("Email already exists.")
        }

        val salt = UUID.randomUUID().toString().replace("-", "").take(16)
        val passHash = Base64.getEncoder().encodeToString((password + salt).toByteArray())

        User.new(normalizedMatr) {
            this.name = normalizedName
            this.passHash = passHash
            this.salt = salt
            this.email = normalizedEmail
            this.rolle = rolle
            this.createdAt = LocalDateTime.now()
            this.updatedAt = LocalDateTime.now()
        }.toDTO()
    }

    fun update(matr: String, name: String, email: String, rolle: UserRole): UserDTO = transaction {
        val normalizedMatr = matr.trim()
        val normalizedName = name.trim()
        val normalizedEmail = email.trim().lowercase()

        if (normalizedName.isBlank()) throw InvalidUserInputException("Name must not be empty.")
        if (!normalizedEmail.contains("@")) throw InvalidUserInputException("Email is invalid.")

        val user = User.findById(normalizedMatr) ?: throw UserNotFoundException(normalizedMatr)
        user.name = normalizedName
        user.email = normalizedEmail
        user.rolle = rolle
        user.updatedAt = LocalDateTime.now()
        user.toDTO()
    }

    fun delete(matr: String) = transaction {
        val normalizedMatr = matr.trim()
        val user = User.findById(normalizedMatr) ?: throw UserNotFoundException(normalizedMatr)
        user.delete()
    }
}

