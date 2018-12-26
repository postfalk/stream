/**
 * Start from here
 * https://github.com/getquill/play-quill-jdbc/blob/master/app/AppLoader.scala
 */

import java.io.Closeable
import javax.sql.DataSource
import javax.inject._

import controllers.{ HomeController, StreamController }
import io.getquill._
import play.api.{
  ApplicationLoader, Application, BuiltInComponentsFromContext }
import play.api.db.evolutions.Evolutions
import play.api.db.{ DBComponents, HikariCPComponents }
import play.api.inject.{ Injector, NewInstanceInjector, SimpleInjector }
import play.api.routing.Router
import play.api.routing.sird._
import play.api.mvc.{ ControllerComponents }
import play.filters.HttpFiltersComponents


class AppLoader extends ApplicationLoader {

  override def load(context: ApplicationLoader.Context): Application =
    new BuiltInComponentsFromContext(context)
    with DBComponents with HikariCPComponents
    with HttpFiltersComponents
  {

  lazy val db = new PostgresJdbcContext(SnakeCase, "db")

  val homeController = new HomeController
  // val streamController = new StreamController

  // override val httpFilters: List[EssentialFilter] 

  val router = Router.from {
    case GET(p"/") => homeController.index()
    // case GET(p"/stream/") => streamController.chunkedFromSource
   }

  // lazy val router = new Routes(assets)

  override lazy val injector: Injector =
    new SimpleInjector(NewInstanceInjector) + router + cookieSigner + csrfTokenSigner + httpConfiguration + tempFileCreator

  Evolutions.applyEvolutions(dbApi.database("default"))

  }.application
}
