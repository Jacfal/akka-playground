package testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

class SynchronousTestingSpec  extends AnyWordSpecLike with BeforeAndAfterAll {
  implicit val system: ActorSystem = ActorSystem("SynchronousTestSpec")

  override def afterAll(): Unit = system.terminate()

  import SynchronousTestingSpec._

  "A counter" should {
    "synchronously increased its counter" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter ! Increment // counter has already received the message
      assert(counter.underlyingActor.count == 1)
    }

    "synchronously increased its counter at the call of the receive function" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter.receive(Increment)
      assert(counter.underlyingActor.count == 1)
    }

    "work on the calling thread dispatcher" in { // dispatcher call whole stack asynchronously!
      val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id))
      val probe = TestProbe()

      probe.send(counter, Read)
      probe.expectMsg(Duration.Zero, 0) // probe has already received the message 0
    }
  }
}

object SynchronousTestingSpec {
  case object Increment
  case object Read

  class Counter extends Actor {
    var count = 0
    override def receive: Receive = {
      case Increment => count += 1
      case Read => sender ! count
    }
  }
}
