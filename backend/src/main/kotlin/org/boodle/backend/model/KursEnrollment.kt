package org.boodle.backend.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.time.LocalDateTime

object KursEnrollmentTable : Table("kurs_enrollment") {
    val kursId = integer("kurs_id").references(KursTable.id)
    val studentMatr = varchar("student_matr", 20).references(UsersTable.matr)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(kursId, studentMatr)
}

class KursStudentAlreadyExistsException(kursId: Int, studentMatr: String) :
    RuntimeException("Student '$studentMatr' is already assigned to Kurs '$kursId'.")

@Service
class KursEnrollmentService {

    fun enrollStudentToKurs(kursId: Int, studentMatr: String) = transaction {
        Kurs.findById(kursId) ?: throw KursNotFoundException(kursId)
        User.findById(studentMatr) ?: throw InvalidKursInputException("Student with matr '$studentMatr' not found.")

        val exists = KursEnrollmentTable.select {
            (KursEnrollmentTable.kursId eq kursId) and
            (KursEnrollmentTable.studentMatr eq studentMatr)
        }.firstOrNull() != null

        if (exists) {
            throw KursStudentAlreadyExistsException(kursId, studentMatr)
        }

        KursEnrollmentTable.insert {
            it[KursEnrollmentTable.kursId] = kursId
            it[KursEnrollmentTable.studentMatr] = studentMatr
            it[KursEnrollmentTable.createdAt] = LocalDateTime.now()
        }
    }

    fun unenrollStudentFromKurs(kursId: Int, studentMatr: String) = transaction {
        val deleted = KursEnrollmentTable.deleteWhere {
            (KursEnrollmentTable.kursId eq kursId) and
            (KursEnrollmentTable.studentMatr eq studentMatr)
        }

        if (deleted == 0) {
            throw KursStudentAlreadyExistsException(kursId, studentMatr)
        }
    }

    fun getEnrolledStudentsForKurs(kursId: Int): List<UserLookupDTO> = transaction {
        Kurs.findById(kursId) ?: throw KursNotFoundException(kursId)

        val studentMatrs = KursEnrollmentTable.select { KursEnrollmentTable.kursId eq kursId }
            .map { it[KursEnrollmentTable.studentMatr] }

        if (studentMatrs.isEmpty()) return@transaction emptyList()

        User.find { UsersTable.matr inList studentMatrs }
            .map { it.toLookupDTO() }
    }
}