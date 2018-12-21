package dao

import scala.concurrent.{ ExecutionContext, Future }
import javax.inject._

import models.User
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._


class UserDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider) (
  implicit ec: ExecutionContext) 
  extends HasDatabaseConfigProvider[JdbcProfile] {

    import profile.api._

    private val Users = TableQuery[UsersTable]

    val insertQuery = Users
      .returning(Users.map(_.id))
      .into((item, id) => item.copy(id = id))

    def insert(user: User): Future[User] = db
      .run(insertQuery += user)
      .map({ result => result })

    class UsersTable(tag: Tag) extends Table[User] (tag, "users") {

      def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
      def name = column[String]("name")
      def token = column[String]("token")

      def * = (id, name, token) <> (User.tupled, User.unapply)

    }

  }
