package patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

class AskSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import AskSpec._
  "An authenticator" should {
    val authManager = system.actorOf(Props[AutManagerActor])
    "fail to authenticate a non-registered user" in {
      authManager ! Authenticate("jp", "Start123")
      expectMsg(AuthFailure(AuthMessages.USERNAME_NOT_FOUND))
    }

    "fail to authenticate with invalid password" in {
      authManager ! RegisterUser("jp", "Start123")
      authManager ! Authenticate("jp", "invalid")
      expectMsg(AuthFailure(AuthMessages.INVALID_PASSWORD))
    }

    "success with correct username and password" in {
      authManager ! RegisterUser("jp", "Start123")
      authManager ! Authenticate("jp", "Start123")
      expectMsg(AuthSuccess)
    }
  }
}

case class ReadDb(key: String)
case class WriteDb(key: String, value: String)

class KVActor extends Actor with ActorLogging {
  override def receive: Receive = online(Map.empty)

  def online(kv: Map[String, String]): Receive = {
    case ReadDb(key) =>
      log.info(s"Trying to read the value at the key $key")
      sender() ! kv.get(key) // Option[String]
    case WriteDb(key, value) =>
      log.info(s"Writing the key / value => $key / $value")
      context.become(online(kv + (key -> value)))
  }
}

case class RegisterUser(username: String, password: String)
case class Authenticate(user: String, password: String)
case class AuthFailure(message: String)
case object AuthSuccess

object AuthMessages {
  val USERNAME_NOT_FOUND = "Username not found"
  val INVALID_PASSWORD = "Invalid password"
  val SYSTEM_ERROR = "System error"
}

class AutManagerActor extends Actor with ActorLogging {
  implicit val timeout: Timeout = Timeout(1 second)
  implicit val ec: ExecutionContext = context.dispatcher

  private val authDb = context.actorOf(Props[KVActor])

  override def receive: Receive = {
    case RegisterUser(username, password) =>
      authDb ! WriteDb(username, password)
    case Authenticate(username, password) =>
      val originalSender = sender() // didn't break encapsulation
      val future = authDb ? ReadDb(username)
      future.onComplete {
        // !!! never call methods on the actor instance or access mutable state in oncomplete
        case Success(None) =>
          // !!! save sender to the value before using it
          // line above breaks encapsulation, because this code run in an another thread!
          // sender() ! AuthFailure("Username not found")
          // better option is using pipeTo (always prefer this approach)
          originalSender ! AuthFailure(AuthMessages.USERNAME_NOT_FOUND)
        case Success(Some(dbPassword)) =>
          if (dbPassword == password)
            originalSender ! AuthSuccess
          else
            originalSender ! AuthFailure(AuthMessages.INVALID_PASSWORD)
        case Failure(_) => originalSender ! AuthFailure(AuthMessages.SYSTEM_ERROR)
      }
  }
}
