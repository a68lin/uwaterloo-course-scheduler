package database.users

import com.zaxxer.hikari.HikariDataSource
import utility.checkPassword
import utility.hashPassword

fun createUsersTableIfNotExists(db: HikariDataSource) {
    db.connection.use { conn ->
        val createTableSQL = """
            CREATE TABLE IF NOT EXISTS users (
                id INT UNIQUE PRIMARY KEY,
                email VARCHAR(50) unique,
                username VARCHAR(20),
                password VARCHAR(64)
            )
        """.trimIndent()

        conn.createStatement().use { stmt ->
            stmt.execute(createTableSQL)
        }
    }
}

fun verifyPasswordByUID(id: Int, password: String, db: HikariDataSource): Boolean {
    return checkPassword(password, queryHashedPasswordByUID(id, db))
}

private fun queryHashedPasswordByUID(id: Int, db: HikariDataSource): String {
    val querySQL = """
        SELECT password FROM users
        WHERE id = ?
    """.trimIndent()
    db.connection.use { conn ->
        conn.prepareStatement(querySQL).use { stmt ->
            stmt.setString(1, id.toString())
            stmt.executeQuery().use { result ->
                if (result.next()) return result.getString("password")
                else return ""
            }
        }
    }
}


fun queryUIDByEmail(email: String, db: HikariDataSource): Int {
    val querySQL = """
       SELECT id FROM users 
       WHERE email = ?
    """.trimIndent()
    db.connection.use { conn ->
        conn.prepareStatement(querySQL).use { stmt ->
            stmt.setString(1, email)
            stmt.executeQuery().use { result ->
                if (result.next()) return result.getInt("id")
                else return 0
            }
        }
    }
}

fun createUser(userName: String, hashedPassword: String, email: String, db: HikariDataSource) {
    val insertSQL = """
        INSERT INTO users (
        SELECT COALESCE(MAX(id) + 1, 1), ?, ?, ?
        FROM users)
        ON DUPLICATE KEY UPDATE id = id
    """.trimIndent()

    db.connection.prepareStatement(insertSQL).use { stmt ->
        stmt.setString(1, email)
        stmt.setString(2, userName)
        stmt.setString(3, hashedPassword)
        stmt.execute()
    }
}

fun createUserRaw(userName: String, password: String, email: String, db: HikariDataSource) {
    val hashedPassword = hashPassword(password)
    createUser(userName, hashedPassword, email, db)
}

fun updatePasswordByUID(id: Int, hashedPassword: String, db: HikariDataSource) {
    val updateSQL = """
        UPDATE users
        SET password = ?
        WHERE id  = ?
    """.trimIndent()

    db.connection.prepareStatement(updateSQL).use { stmt ->
        stmt.setString(1, hashedPassword)
        stmt.setString(2, id.toString())
        stmt.execute()
    }
}

fun updatePasswordByUIDRaw(id: Int, password: String, db: HikariDataSource) {
    val hashedPassword = hashPassword(password)
    updatePasswordByUID(id, hashedPassword, db)
}

fun main() {
    database.common.createDataSource().use {
        createUsersTableIfNotExists(it)
        createUser("xiaoye", "password", "aaa@gmail.com", it)
        val uid = queryUIDByEmail("aaa@gmail.com", it)
        updatePasswordByUIDRaw(uid, "password", it)
        createUserRaw("eddy", "password", "ccc@gmail.com", it)
        createUserRaw("alex", "password", "bbb@gmail.com", it)
        createUserRaw("ryan", "password", "ddd@gmail.com", it)
//        createUserRaw("hello", "password", "eee@gmail.com", it)
    }
}