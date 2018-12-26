import play.api.{
  Application, ApplicationLoader, BuiltInComponentsFromContext }
import play.api.inject.{ Injector, NewInstanceInjector, SimpleInjector }
import play.api.ApplicationLoader.Context
import play.filters.HttpFiltersComponents
import play.api.routing.Router
import play.api.routing.sird._

import controllers.{ HomeController, StreamController }


class AppLoader extends ApplicationLoader {

  override def load(context: Context): 
    Application = new BuiltInComponentsFromContext(context)
      with HttpFiltersComponents
  {

  val homeController = new HomeController(controllerComponents)
  val streamController = new StreamController(controllerComponents)
  val router = Router.from{
    case GET(p"/") => homeController.index
    case GET(p"/stream/") => streamController.chunkedFromSource
    case POST(p"/stream") => streamController.chunkedFromSource
  }

  override lazy val injector: Injector =
    new SimpleInjector(NewInstanceInjector) + homeController +
      streamController + router + cookieSigner + csrfTokenSigner +
      httpConfiguration + tempFileCreator

  }.application

}
