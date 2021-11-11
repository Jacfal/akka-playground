package testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class TimeAssertionsSpec extends TestKit(ActorSystem("TImeAssertionsSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TimeAssertionsSpec._
  "A worker actor" should {
    val workerActor = system.actorOf(Props[WorkerActor])
    "reply with some result in a timely manner" in {
      within(500 millis, 1 second) {
        workerActor ! "work"
        expectMsg(WorkResult(42))
      }
    }

    "reply with valid work in a reasonable time" in {
      within(1 second) {
        workerActor ! "workSequence"
        val results = receiveWhile[Int](max = 2 seconds, idle = 500 millis, messages = 10) {
          case WorkResult(result) => result
        }

        assert(results.sum > 5)
      }
    }
  }
}

object TimeAssertionsSpec {
  case class WorkResult(result: Int)
  // testing scenario
  class WorkerActor extends Actor {
    override def receive: Receive = {
      case "work" =>
        // long computation
        Thread.sleep(500)
        sender ! WorkResult(42)

      case "workSequence" =>
        val r = new Random()
        for (i <- 1 to 10) {
          Thread.sleep(r.nextInt(50)) // wait random time up to 50 ms
          sender ! WorkResult(1)
        }

    }
  }
}
