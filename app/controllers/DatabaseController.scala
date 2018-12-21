package controllers

import models.User
import dao.UserDAO

import scala.concurrent._
import scala.concurrent.duration.Duration
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.db.slick._
import slick.jdbc.JdbcProfile

/**
 * This controller is just here for getting started with a database
 */
@Singleton
class DatabaseController @Inject() (
  userDao: UserDAO,
  protected val dbConfigProvider: DatabaseConfigProvider,
  cc: ControllerComponents
) (implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile] {

  def index() = Action { implicit request: Request[AnyContent]  =>

    val user = User(0, "Test", "token")
    val returned = Await.result(userDao.insert(user), Duration.Inf)

    Ok(returned.id.toString)
  }

}
