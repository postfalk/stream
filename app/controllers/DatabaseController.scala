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
)(implicit ec: ExecutionContext)
    extends AbstractController(cc) 
    with HasDatabaseConfigProvider[JdbcProfile] {

  val user = User(2, "Falk", "token")
  Await.result(userDao.insert(user), Duration.Inf)

  def index() = Action { implicit request: Request[AnyContent]  => 
    Ok("hello")
  }

}
