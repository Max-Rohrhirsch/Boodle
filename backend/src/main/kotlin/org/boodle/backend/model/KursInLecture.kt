package org.boodle.backend.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.time.LocalDateTime

object KursInLectureTable : Table("kurs_in_lecture") {
    val kursId = integer("kurs_id").references(KursTable.id)
    val vorlesungId = integer("vorlesung_id").references(VorlesungTable.id)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(kursId, vorlesungId)
}

class KursInLectureAlreadyExistsException(kursId: Int, vorlesungId: Int) :
    RuntimeException("Kurs '$kursId' is already assigned to Vorlesung '$vorlesungId'.")

@Service
class KursInLectureService {

    fun assignLectureToKurs(kursId: Int, vorlesungId: Int) = transaction {
        Kurs.findById(kursId) ?: throw KursNotFoundException(kursId)
        Vorlesung.findById(vorlesungId) ?: throw VorlesungNotFoundException(vorlesungId)

        val exists = KursInLectureTable.select { 
            (KursInLectureTable.kursId eq kursId) and 
            (KursInLectureTable.vorlesungId eq vorlesungId)
        }.firstOrNull() != null
        
        if (exists) {
            throw KursInLectureAlreadyExistsException(kursId, vorlesungId)
        }

        KursInLectureTable.insert {
            it[KursInLectureTable.kursId] = kursId
            it[KursInLectureTable.vorlesungId] = vorlesungId
            it[KursInLectureTable.createdAt] = LocalDateTime.now()
        }
    }

    fun removeLectureFromKurs(kursId: Int, vorlesungId: Int) = transaction {
        val deleted = KursInLectureTable.deleteWhere { 
            (KursInLectureTable.kursId eq kursId) and
            (KursInLectureTable.vorlesungId eq vorlesungId)
        }
        if (deleted == 0) throw KursInLectureAlreadyExistsException(kursId, vorlesungId)
    }
}
