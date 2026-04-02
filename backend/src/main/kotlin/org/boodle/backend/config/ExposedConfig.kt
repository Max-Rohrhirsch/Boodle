package org.boodle.backend.config

import org.boodle.backend.model.UsersTable
import org.boodle.backend.model.KursTable
import org.boodle.backend.model.VorlesungTable
import org.boodle.backend.model.KursInLectureTable
import org.boodle.backend.model.LectureEnrollmentTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class ExposedConfig(private val dataSource: DataSource) {

    @Bean
    fun setupExposed(): Database {
        val database = Database.connect(dataSource)
        transaction(database) {
            SchemaUtils.drop(LectureEnrollmentTable, KursInLectureTable, VorlesungTable, KursTable, UsersTable)
            SchemaUtils.create(UsersTable, KursTable, VorlesungTable, KursInLectureTable, LectureEnrollmentTable)
        }
        return database
    }
}
