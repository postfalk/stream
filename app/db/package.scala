/**
 *  Get Database context from Play framework instead of
 *  creating a second one within the Quill framework
 *  See https://stackoverflow.com/questions/49075152/
 *  conflict-between-hikari-quill-and-postgres-in-the-conf-file-for-play-2-6
 */
import javax.inject.{ Inject, Singleton }
import io.getquill.{ PostgresJdbcContext, SnakeCase }
import com.zaxxer.hikari.HikariDataSource
import play.api.db.Database


package object db {

  @Singleton
    class DbContext @Inject() (db: Database) extends PostgresJdbcContext(
    SnakeCase, db.dataSource.asInstanceOf[HikariDataSource])
}
