package models

import javax.inject.Inject
/**
 * Database context is created in app/db/package.scala
 */
import db.DbContext

/**
 *  User class
 */
case class User(id: Long, name: String, is_active: Boolean)

/**
 * Users DAO (no need to create another file for now)
 */
class Users @Inject() (val db: DbContext) {

  import db._

  val users = quote(querySchema[User]("users"))

  def create(user: User) = user.copy(
    id = run(users.insert(lift(user)).returning(_.id)))

}
