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
    /**
     * We need to manage which fields to insert to avoid the insertion of
     * auto-generated id value which would cause a unique constraint violation
     */
    id = run(users
      .insert(_.name -> lift(user.name), _.is_active -> lift(user.is_active))
      .returningGenerated(_.id)))

  def retrieve(id: Long) = run(
    users.filter(p => p.id==lift(id))).headOption

}
