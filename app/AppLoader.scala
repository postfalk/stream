/** Custom application loader
 *  following 
 *  https://github.com/getquill/play-quill-jdbc/blob/master/app/AppLoader.scala
 */
import play.api.{
  Application, ApplicationLoader, BuiltInComponentsFromContext }
import play.api.inject.{ Injector, NewInstanceInjector, SimpleInjector }
import play.api.ApplicationLoader.Context
import play.filters.HttpFiltersComponents
import play.api.routing.Router
import play.api.routing.sird._
// database modules
import io.getquill.{ PostgresJdbcContext, SnakeCase }
import play.api.db.evolutions.Evolutions
import play.api.db.{ DBComponents, HikariCPComponents }
// application modules
import controllers.{ HomeController, StreamController }


class AppLoader extends ApplicationLoader {

  override def load(context: Context): 
    Application = new BuiltInComponentsFromContext(context)
      with HttpFiltersComponents
      with DBComponents with HikariCPComponents
  {

  lazy val db = new PostgresJdbcContext(SnakeCase, "db")

  // Let's see whether we can push that back to the framework
  // configuration at some point
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

  Evolutions.applyEvolutions(dbApi.database("default"))

  }.application

}
