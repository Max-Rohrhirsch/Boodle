package org.boodle.backend.config

import org.boodle.backend.model.UsersTable
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
            SchemaUtils.createMissingTablesAndColumns(UsersTable)
        }
        return database
    }
}
