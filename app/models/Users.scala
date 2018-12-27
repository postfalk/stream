package models


import db.DbContext

/**
 *  User class
 */
case class User(id: Long, name: String, is_active: Boolean)


/**
 * Users DAO
 */
class Users(val db: DbContext) {
  import db._

  val users = quote(querySchema[User]("users"))

  def create(user: User) = user.copy(
    id = run(users.insert(lift(user)).returning(_.id)))

}
