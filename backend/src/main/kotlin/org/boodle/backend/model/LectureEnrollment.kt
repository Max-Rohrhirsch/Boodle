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

object LectureEnrollmentTable : Table("lecture_enrollment") {
    val vorlesungId = integer("vorlesung_id").references(VorlesungTable.id)
    val studentMatr = varchar("student_matr", 20).references(UsersTable.matr)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(vorlesungId, studentMatr)
}

class EnrollmentAlreadyExistsException(vorlesungId: Int, studentMatr: String) :
    RuntimeException("Student '$studentMatr' is already enrolled in Vorlesung '$vorlesungId'.")

@Service
class LectureEnrollmentService {

    fun enrollStudentDirect(vorlesungId: Int, studentMatr: String) = transaction {
        Vorlesung.findById(vorlesungId) ?: throw VorlesungNotFoundException(vorlesungId)
        User.findById(studentMatr) ?: throw InvalidVorlesungInputException("Student with matr '$studentMatr' not found.")

        val exists = LectureEnrollmentTable.select { 
            (LectureEnrollmentTable.vorlesungId eq vorlesungId) and 
            (LectureEnrollmentTable.studentMatr eq studentMatr) 
        }.firstOrNull() != null
        
        if (exists) {
            throw EnrollmentAlreadyExistsException(vorlesungId, studentMatr)
        }

        LectureEnrollmentTable.insert {
            it[LectureEnrollmentTable.vorlesungId] = vorlesungId
            it[LectureEnrollmentTable.studentMatr] = studentMatr
            it[LectureEnrollmentTable.createdAt] = LocalDateTime.now()
        }
    }

    fun unenrollStudent(vorlesungId: Int, studentMatr: String) = transaction {
        val deleted = LectureEnrollmentTable.deleteWhere { 
            (LectureEnrollmentTable.vorlesungId eq vorlesungId) and 
            (LectureEnrollmentTable.studentMatr eq studentMatr) 
        }
        if (deleted == 0) throw EnrollmentAlreadyExistsException(vorlesungId, studentMatr)
    }

    fun getEnrolledStudents(vorlesungId: Int): List<String> = transaction {
        Vorlesung.findById(vorlesungId) ?: throw VorlesungNotFoundException(vorlesungId)

        LectureEnrollmentTable.select { LectureEnrollmentTable.vorlesungId eq vorlesungId }
            .map { it[LectureEnrollmentTable.studentMatr] }
            .toList()
    }

    fun getEnrolledStudentUsers(vorlesungId: Int): List<UserLookupDTO> = transaction {
        Vorlesung.findById(vorlesungId) ?: throw VorlesungNotFoundException(vorlesungId)

        val studentMatrs = LectureEnrollmentTable.select { LectureEnrollmentTable.vorlesungId eq vorlesungId }
            .map { it[LectureEnrollmentTable.studentMatr] }

        if (studentMatrs.isEmpty()) return@transaction emptyList()

        User.find { UsersTable.matr inList studentMatrs }
            .map { it.toLookupDTO() }
    }
}
