package dao

import scala.concurrent.{ ExecutionContext, Future }
import javax.inject._

import models.User
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._


class UserDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider) (
  implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

    import profile.api._

    private val Users = TableQuery[UsersTable]

    def insert(user: User): Future[Unit] = db.run(Users += user).map { _ => () }

    private class UsersTable(tag: Tag) extends Table[User](tag, "USERS") {

      def id = column[Int]("ID", O.PrimaryKey)
      def name = column[String]("NAME")
      def token = column[String]("TOKEN")

      def * = (id, name, token) <> (User.tupled, User.unapply)

    }

  }
