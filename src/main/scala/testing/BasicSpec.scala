package testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.TestActors.BlackholeActor
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import testing.BasicSpec.{LabTestActor, SimpleActor}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll { // all test should have spec suffix

  // setup
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An echo actor" should {
    "send back the same message" in {
      val echoActor = system.actorOf(Props[SimpleActor])
      val message = "hello, test"
      echoActor ! message

      expectMsg(message)
    }
  }

  "A blackhole adctor" should {
    "send back some message" in {
      val blackhole = system.actorOf(Props[BlackholeActor])
      val message = "hello, test"
      blackhole ! message

      expectNoMessage(1 second) // by default waiting 3 secs
    }
  }

  // message assertion
  "A lab test actor" should {
    val labTestActor = system.actorOf(Props[LabTestActor])

    "turn a string into upper case" in {
      labTestActor ! "I love akka"
      //expectMsg("I LOVE AKKA")
      val reply = expectMsgType[String]

      assert(reply == "I LOVE AKKA")
    }

    "reply to a random" in {
      labTestActor ! "random"
      expectMsgAnyOf("Hi", "Hello")
    }
  }
}

object BasicSpec {
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => sender ! message
    }
  }

  class BlackHole extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTestActor extends Actor {
    val random = new Random()
    override def receive: Receive = {
      case "random" =>
        if (random.nextInt() % 2 == 0) sender ! "Hello"
        else sender ! "Hi"
      case message: String => sender ! message.toUpperCase()
    }
  }
}
