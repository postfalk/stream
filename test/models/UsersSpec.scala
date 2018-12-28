package models

import javax.inject.Inject

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting
import play.api.test.Helpers._
import db.DbContext

class UsersSpec extends PlaySpec with GuiceOneAppPerTest with Injecting
{

  "UserService.insert" should {

    "should insert two users" in {
      val userService = inject[Users]
      val user = userService.create(User(0, "Test User", false))
      user.id mustBe 1
      val anotherUser = userService.create(User(1, "Another User", true))
      anotherUser.id mustBe 2
    }

  }
}
