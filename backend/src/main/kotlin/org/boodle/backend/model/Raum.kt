package org.boodle.backend.model

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.time.LocalDateTime

object RaumTable : IntIdTable("raum") {
    val code = varchar("code", 50).uniqueIndex()
    val beschreibung = varchar("beschreibung", 255).nullable()
    val kapazitaet = integer("kapazitaet").default(30) // Default capacity 30
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

data class RaumDTO(
    val id: Int,
    val code: String,
    val beschreibung: String?,
    val kapazitaet: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

class Raum(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, Raum>(RaumTable)

    var code by RaumTable.code
    var beschreibung by RaumTable.beschreibung
    var kapazitaet by RaumTable.kapazitaet
    var createdAt by RaumTable.createdAt
    var updatedAt by RaumTable.updatedAt
}

fun Raum.toDTO(): RaumDTO = RaumDTO(
    id = id.value,
    code = code,
    beschreibung = beschreibung,
    kapazitaet = kapazitaet,
    createdAt = createdAt,
    updatedAt = updatedAt
)

class RaumNotFoundException(id: Int) : RuntimeException("Raum with ID '$id' was not found.")
class RaumCodeAlreadyExistsException(code: String) : RuntimeException("Raum with code '$code' already exists.")
class InvalidRaumInputException(message: String) : RuntimeException(message)

@Service
class RaumService {

    fun getAllRaeume(): List<RaumDTO> = transaction {
        Raum.all().map { it.toDTO() }
    }

    fun getRaumById(id: Int): RaumDTO = transaction {
        Raum.findById(id)?.toDTO() ?: throw RaumNotFoundException(id)
    }

    fun create(code: String, beschreibung: String?, kapazitaet: Int = 30): RaumDTO = transaction {
        val normalizedCode = code.trim().uppercase()
        if (normalizedCode.isBlank()) throw InvalidRaumInputException("Code must not be empty.")
        if (normalizedCode.length > 50) throw InvalidRaumInputException("Code is too long (max 50 chars).")
        if (kapazitaet <= 0) throw InvalidRaumInputException("Kapazität must be greater than 0.")

        Raum.find { RaumTable.code eq normalizedCode }.firstOrNull()?.let {
            throw RaumCodeAlreadyExistsException(normalizedCode)
        }

        Raum.new {
            this.code = normalizedCode
            this.beschreibung = beschreibung?.trim()?.takeIf { it.isNotEmpty() }
            this.kapazitaet = kapazitaet
            this.createdAt = LocalDateTime.now()
            this.updatedAt = LocalDateTime.now()
        }.toDTO()
    }

    fun update(id: Int, code: String, beschreibung: String?, kapazitaet: Int = 30): RaumDTO = transaction {
        val normalizedCode = code.trim().uppercase()
        if (normalizedCode.isBlank()) throw InvalidRaumInputException("Code must not be empty.")
        if (normalizedCode.length > 50) throw InvalidRaumInputException("Code is too long (max 50 chars).")
        if (kapazitaet <= 0) throw InvalidRaumInputException("Kapazität must be greater than 0.")

        val raum = Raum.findById(id) ?: throw RaumNotFoundException(id)
        val duplicate = Raum.find { RaumTable.code eq normalizedCode }
            .firstOrNull { it.id.value != id }

        if (duplicate != null) {
            throw RaumCodeAlreadyExistsException(normalizedCode)
        }

        raum.code = normalizedCode
        raum.beschreibung = beschreibung?.trim()?.takeIf { it.isNotEmpty() }
        raum.kapazitaet = kapazitaet
        raum.updatedAt = LocalDateTime.now()
        raum.toDTO()
    }

    fun delete(id: Int) = transaction {
        val raum = Raum.findById(id) ?: throw RaumNotFoundException(id)
        raum.delete()
    }
}
