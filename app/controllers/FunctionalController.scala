package controllers

import javax.inject._
import play.api._
import play.api.mvc._

/**
 * This controller serves functional flow data
 */

class FunctionalController @Inject() (
  cc: ControllerComponents) extends AbstractController(cc) {

  def get() = Action { implicit request: Request[AnyContent] =>
    Ok("hello")}
}
