package org.boodle.backend.model

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.time.LocalDateTime

object KursTable : IntIdTable("kurs") {
    val name = varchar("name", 255)
    val dozentMatr = varchar("dozent_matr", 20)
    val kurssprecher1Matr = varchar("kurssprecher1_matr", 20).nullable()
    val kurssprecher2Matr = varchar("kurssprecher2_matr", 20).nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

data class KursDTO(
    val id: Int,
    val name: String,
    val dozentMatr: String,
    val kurssprecher1Matr: String?,
    val kurssprecher2Matr: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

class Kurs(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, Kurs>(KursTable)

    var name by KursTable.name
    var dozentMatr by KursTable.dozentMatr
    var kurssprecher1Matr by KursTable.kurssprecher1Matr
    var kurssprecher2Matr by KursTable.kurssprecher2Matr
    var createdAt by KursTable.createdAt
    var updatedAt by KursTable.updatedAt
}

fun Kurs.toDTO(): KursDTO = KursDTO(
    id = id.value,
    name = name,
    dozentMatr = dozentMatr,
    kurssprecher1Matr = kurssprecher1Matr,
    kurssprecher2Matr = kurssprecher2Matr,
    createdAt = createdAt,
    updatedAt = updatedAt
)

class KursNotFoundException(id: Int) : RuntimeException("Kurs with ID '$id' was not found.")
class InvalidKursInputException(message: String) : RuntimeException(message)

@Service
class KursService {

    fun getAllKurse(): List<KursDTO> = transaction {
        Kurs.all().map { it.toDTO() }
    }

    fun getKursById(id: Int): KursDTO = transaction {
        Kurs.findById(id)?.toDTO() ?: throw KursNotFoundException(id)
    }

    fun create(
        name: String,
        dozentMatr: String,
        kurssprecher1Matr: String?,
        kurssprecher2Matr: String?
    ): KursDTO = transaction {
        val normalizedName = name.trim()

        if (normalizedName.isBlank()) throw InvalidKursInputException("Name must not be empty.")
        if (normalizedName.length > 255) throw InvalidKursInputException("Name is too long (max 255 chars).")

        // Verify dozent exists
        User.findById(dozentMatr) ?: throw InvalidKursInputException("Dozent with matr '$dozentMatr' not found.")

        // Verify kurssprecher if provided
        kurssprecher1Matr?.let { User.findById(it) ?: throw InvalidKursInputException("Kurssprecher1 with matr '$it' not found.") }
        kurssprecher2Matr?.let { User.findById(it) ?: throw InvalidKursInputException("Kurssprecher2 with matr '$it' not found.") }

        Kurs.new {
            this.name = normalizedName
            this.dozentMatr = dozentMatr
            this.kurssprecher1Matr = kurssprecher1Matr
            this.kurssprecher2Matr = kurssprecher2Matr
            this.createdAt = LocalDateTime.now()
            this.updatedAt = LocalDateTime.now()
        }.toDTO()
    }

    fun update(
        id: Int,
        name: String?,
        kurssprecher1Matr: String?,
        kurssprecher2Matr: String?
    ): KursDTO = transaction {
        val kurs = Kurs.findById(id) ?: throw KursNotFoundException(id)

        name?.let {
            val normalizedName = it.trim()
            if (normalizedName.isBlank()) throw InvalidKursInputException("Name must not be empty.")
            if (normalizedName.length > 255) throw InvalidKursInputException("Name is too long (max 255 chars).")
            kurs.name = normalizedName
        }

        kurssprecher1Matr?.let { User.findById(it) ?: throw InvalidKursInputException("Kurssprecher1 with matr '$it' not found.") }
        kurs.kurssprecher1Matr = kurssprecher1Matr

        kurssprecher2Matr?.let { User.findById(it) ?: throw InvalidKursInputException("Kurssprecher2 with matr '$it' not found.") }
        kurs.kurssprecher2Matr = kurssprecher2Matr

        kurs.updatedAt = LocalDateTime.now()
        kurs.toDTO()
    }

    fun delete(id: Int) = transaction {
        val kurs = Kurs.findById(id) ?: throw KursNotFoundException(id)
        kurs.delete()
    }
}
