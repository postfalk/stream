package controllers

import javax.inject._
import play.api._
import play.api.mvc._

import models.{ User, Users }


/**
 * This controller tests quill interactions
 */


@Singleton
class DatabaseTestController @Inject() (
  cc: ControllerComponents, userService: Users)
  extends AbstractController(cc)
{
  def get() = Action { implicit request: Request[AnyContent] =>
    Ok(
      userService.create(User(0, "Falk", true)).id.toString
    )
  }
}
