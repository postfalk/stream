package controllers

import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ 
  AbstractController, AnyContent, ControllerComponents, Request }
import models.{ User, Users }


@Singleton
class UsersController @Inject() (
  cc: ControllerComponents, userService: Users)
  extends AbstractController(cc)
{

  def get(id: Integer) = Action {implicit request: Request[AnyContent] =>

    val resOption = userService.retrieve(id.toLong)
    resOption match {
      case Some(value) => Ok(value.toString)
      case None => NotFound("404")
    }
    // Ok(id.toString)
  }

}
