package models

import javax.inject.Inject

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting
import play.api.db._
import play.api.test.Helpers._
import play.api.db.evolutions._


class UsersSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  lazy val database1 = app.injector.instanceOf[Database]

  "UserService.insert" should {
    "insert two users" in Evolutions.withEvolutions(database1) {
      val userService = inject[Users]
      val user = userService.create(User(0, "Test User", false))
      user.id mustBe 1
      val anotherUser = userService.create(User(0, "Another User", true))
      anotherUser.id mustBe 2
    }
  }

  /**
   * This is not pretty and I have to figure out how to use withEvolutions for
   * several test at once but it works for now. The reason is that the 
   * database connection will be closed after the database instance is used 
   * with Evolutions.withEvolutions()
   */
  lazy val database2 = app.injector.instanceOf[Database]

  "UserService.retrieve" should {
    "retrieve a User" in Evolutions.withEvolutions(database2) {
      val userService = inject[Users]
      val user = userService.create(User(0, "retrieval user", false))
      val userRetrieved = userService.retrieve(user.id)
      userRetrieved.get.id mustBe 1
      userRetrieved.get.name mustBe "retrieval user"
      val nonExistentUser = userService.retrieve(10)
      nonExistentUser mustBe None
    }
  }

}
